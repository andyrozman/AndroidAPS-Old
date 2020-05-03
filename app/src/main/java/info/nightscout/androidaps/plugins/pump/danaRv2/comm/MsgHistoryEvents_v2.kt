package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.R
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.db.ExtendedBolus
import info.nightscout.androidaps.db.Source
import info.nightscout.androidaps.db.TemporaryBasal
import info.nightscout.androidaps.events.EventPumpStatusChanged
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.pump.common.bolusInfo.DetailedBolusInfoStorage
import info.nightscout.androidaps.plugins.pump.danaR.DanaRPump
import info.nightscout.androidaps.plugins.pump.danaR.comm.MessageBase
import info.nightscout.androidaps.plugins.pump.danaRv2.DanaRv2Plugin
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper
import java.util.*

class MsgHistoryEvents_v2 constructor(
    val aapsLogger: AAPSLogger,
    val resourceHelper: ResourceHelper,
    private val detailedBolusInfoStorage: DetailedBolusInfoStorage,
    val danaRv2Plugin: DanaRv2Plugin,
    val rxBus: RxBusWrapper,
    val treatmentsPlugin: TreatmentsPlugin,
    private val injector: HasAndroidInjector,
    var from: Long = 0
) : MessageBase() {

    init {
        SetCommand(0xE003)
        if (from > DateUtil.now()) {
            aapsLogger.error("Asked to load from the future")
            from = 0
        }
        if (from == 0L) {
            AddParamByte(0.toByte())
            AddParamByte(1.toByte())
            AddParamByte(1.toByte())
            AddParamByte(0.toByte())
            AddParamByte(0.toByte())
        } else {
            val gfrom = GregorianCalendar()
            gfrom.timeInMillis = from
            AddParamDate(gfrom)
        }
        danaRv2Plugin.eventsLoadingDone = false
        aapsLogger.debug(LTag.PUMPBTCOMM, "New message")
    }

    override fun handleMessage(bytes: ByteArray) {
        val recordCode = intFromBuff(bytes, 0, 1).toByte()

        // Last record
        if (recordCode == 0xFF.toByte()) {
            danaRv2Plugin.eventsLoadingDone = true
            return
        }
        danaRv2Plugin.eventsLoadingDone = false
        val datetime = dateTimeSecFromBuff(bytes, 1) // 6 bytes
        val param1 = intFromBuff(bytes, 7, 2)
        val param2 = intFromBuff(bytes, 9, 2)
        val temporaryBasal = TemporaryBasal(injector)
            .date(datetime)
            .source(Source.PUMP)
            .pumpId(datetime)
        val extendedBolus = ExtendedBolus(injector)
            .date(datetime)
            .source(Source.PUMP)
            .pumpId(datetime)
        val status: String
        when (recordCode.toInt()) {
            DanaRPump.TEMPSTART         -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT TEMPSTART (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Ratio: " + param1 + "% Duration: " + param2 + "min")
                temporaryBasal.percentRate = param1
                temporaryBasal.durationInMinutes = param2
                treatmentsPlugin.addToHistoryTempBasal(temporaryBasal)
                status = "TEMPSTART " + DateUtil.timeString(datetime)
            }

            DanaRPump.TEMPSTOP          -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT TEMPSTOP (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime))
                treatmentsPlugin.addToHistoryTempBasal(temporaryBasal)
                status = "TEMPSTOP " + DateUtil.timeString(datetime)
            }

            DanaRPump.EXTENDEDSTART     -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT EXTENDEDSTART (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Amount: " + param1 / 100.0 + "U Duration: " + param2 + "min")
                extendedBolus.insulin = param1 / 100.0
                extendedBolus.durationInMinutes = param2
                treatmentsPlugin.addToHistoryExtendedBolus(extendedBolus)
                status = "EXTENDEDSTART " + DateUtil.timeString(datetime)
            }

            DanaRPump.EXTENDEDSTOP      -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT EXTENDEDSTOP (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Delivered: " + param1 / 100.0 + "U RealDuration: " + param2 + "min")
                treatmentsPlugin.addToHistoryExtendedBolus(extendedBolus)
                status = "EXTENDEDSTOP " + DateUtil.timeString(datetime)
            }

            DanaRPump.BOLUS             -> {
                val detailedBolusInfo = detailedBolusInfoStorage.findDetailedBolusInfo(datetime, param1 / 100.0)
                    ?: DetailedBolusInfo()
                detailedBolusInfo.date = datetime
                detailedBolusInfo.source = Source.PUMP
                detailedBolusInfo.pumpId = datetime
                detailedBolusInfo.insulin = param1 / 100.0
                val newRecord = treatmentsPlugin.addToHistoryTreatment(detailedBolusInfo, false)
                aapsLogger.debug(LTag.PUMPBTCOMM, (if (newRecord) "**NEW** " else "") + "EVENT BOLUS (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Bolus: " + param1 / 100.0 + "U Duration: " + param2 + "min")
                status = "BOLUS " + DateUtil.timeString(datetime)
            }

            DanaRPump.DUALBOLUS         -> {
                val detailedBolusInfo = detailedBolusInfoStorage.findDetailedBolusInfo(datetime, param1 / 100.0)
                    ?: DetailedBolusInfo()
                detailedBolusInfo.date = datetime
                detailedBolusInfo.source = Source.PUMP
                detailedBolusInfo.pumpId = datetime
                detailedBolusInfo.insulin = param1 / 100.0
                val newRecord = treatmentsPlugin.addToHistoryTreatment(detailedBolusInfo, false)
                aapsLogger.debug(LTag.PUMPBTCOMM, (if (newRecord) "**NEW** " else "") + "EVENT DUALBOLUS (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Bolus: " + param1 / 100.0 + "U Duration: " + param2 + "min")
                status = "DUALBOLUS " + DateUtil.timeString(datetime)
            }

            DanaRPump.DUALEXTENDEDSTART -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT DUALEXTENDEDSTART (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Amount: " + param1 / 100.0 + "U Duration: " + param2 + "min")
                extendedBolus.insulin = param1 / 100.0
                extendedBolus.durationInMinutes = param2
                treatmentsPlugin.addToHistoryExtendedBolus(extendedBolus)
                status = "DUALEXTENDEDSTART " + DateUtil.timeString(datetime)
            }

            DanaRPump.DUALEXTENDEDSTOP  -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT DUALEXTENDEDSTOP (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Delivered: " + param1 / 100.0 + "U RealDuration: " + param2 + "min")
                treatmentsPlugin.addToHistoryExtendedBolus(extendedBolus)
                status = "DUALEXTENDEDSTOP " + DateUtil.timeString(datetime)
            }

            DanaRPump.SUSPENDON         -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT SUSPENDON (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")")
                status = "SUSPENDON " + DateUtil.timeString(datetime)
            }

            DanaRPump.SUSPENDOFF        -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT SUSPENDOFF (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")")
                status = "SUSPENDOFF " + DateUtil.timeString(datetime)
            }

            DanaRPump.REFILL            -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT REFILL (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Amount: " + param1 / 100.0 + "U")
                status = "REFILL " + DateUtil.timeString(datetime)
            }

            DanaRPump.PRIME             -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT PRIME (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Amount: " + param1 / 100.0 + "U")
                status = "PRIME " + DateUtil.timeString(datetime)
            }

            DanaRPump.PROFILECHANGE     -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "EVENT PROFILECHANGE (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " No: " + param1 + " CurrentRate: " + param2 / 100.0 + "U/h")
                status = "PROFILECHANGE " + DateUtil.timeString(datetime)
            }

            DanaRPump.CARBS             -> {
                val emptyCarbsInfo = DetailedBolusInfo()
                emptyCarbsInfo.carbs = param1.toDouble()
                emptyCarbsInfo.date = datetime
                emptyCarbsInfo.source = Source.PUMP
                emptyCarbsInfo.pumpId = datetime
                val newRecord = treatmentsPlugin.addToHistoryTreatment(emptyCarbsInfo, false)
                aapsLogger.debug(LTag.PUMPBTCOMM, (if (newRecord) "**NEW** " else "") + "EVENT CARBS (" + recordCode + ") " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Carbs: " + param1 + "g")
                status = "CARBS " + DateUtil.timeString(datetime)
            }

            else                        -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "Event: " + recordCode + " " + DateUtil.dateAndTimeString(datetime) + " (" + datetime + ")" + " Param1: " + param1 + " Param2: " + param2)
                status = "UNKNOWN " + DateUtil.timeString(datetime)
            }
        }
        if (datetime > danaRv2Plugin.lastEventTimeLoaded) danaRv2Plugin.lastEventTimeLoaded = datetime
        rxBus.send(EventPumpStatusChanged(resourceHelper.gs(R.string.processinghistory) + ": " + status))
    }
}