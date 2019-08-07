package info.nightscout.androidaps.database.transactions.insight

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*
import java.util.concurrent.TimeUnit

class InsightHistoryTransaction(val pumpSerial: String) : Transaction<Unit>() {

    val therapyEvents = mutableListOf<TherapyEvent>()
    val temporaryBasals = mutableListOf<TemporaryBasal>()
    val boluses = mutableListOf<Bolus>()
    val totalDailyDoses = mutableListOf<TotalDailyDose>()
    val operatingModeChanges = mutableListOf<OperatingModeChange>()

    override fun run() {
        processBoluses()
        processTemporaryBasals()
        processTherapyEvents()
        processTotalDailyDoses()
        processOperatingModeChanges()
    }

    private fun processTotalDailyDoses() {
        totalDailyDoses.forEach {
            database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    bolusAmount = it.bolusAmount,
                    basalAmount = it.basalAmount,
                    totalAmount = it.bolusAmount + it.basalAmount
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = it.eventId
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        }
    }

    private fun processTherapyEvents() {
        therapyEvents.forEach {
            val timestamp = it.timestamp
            val eventId = it.eventId
            val type = when (it.type) {
                TherapyEvent.Type.CANNULA_FILLED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.CANNULA_CHANGED
                TherapyEvent.Type.OCCLUSION -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.OCCLUSION
                TherapyEvent.Type.TUBE_FILLED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.TUBE_CHANGED
                TherapyEvent.Type.RESERVOIR_CHANGED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.RESERVOIR_CHANGED
                TherapyEvent.Type.BATTERY_EMPTY -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.BATTERY_EMPTY
                TherapyEvent.Type.RESERVOIR_EMPTY -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.RESERVOIR_EMPTY
                TherapyEvent.Type.BATTERY_CHANGED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.BATTERY_CHANGED
            }
            database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    type = type
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = eventId
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        }
    }

    private fun getDateStopped(eventId: Long): Long? {
        val stopEvent = database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, eventId)
        if (stopEvent?.type == info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STOPPED) {
            val pauseEvent = database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, stopEvent.interfaceIDs.pumpId!!)
            if (pauseEvent?.type == info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_PAUSED
                    && stopEvent.timestamp - pauseEvent.timestamp < TimeUnit.MINUTES.toMillis(20)) {
                return pauseEvent.timestamp
            }
            return stopEvent.timestamp
        }
        return null
    }

    private fun saveSuspendTBR(startEvent: OperatingModeChange, lastStoppedEvent: OperatingModeChange?, lastPausedEvent: OperatingModeChange?) {
        val dateStopped = if (lastStoppedEvent == null) {
            getDateStopped(startEvent.eventId)
        } else {
            lastPausedEvent?.timestamp ?: lastStoppedEvent.timestamp
        }
        if (dateStopped != null) {
            database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(dateStopped).toLong(),
                    timestamp = dateStopped,
                    duration = startEvent.timestamp - dateStopped,
                    absolute = false,
                    rate = 0.0,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.PUMP_SUSPEND
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = startEvent.eventId
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        }
    }

    private fun processOperatingModeChanges() {
        var lastStoppedEvent: OperatingModeChange? = null
        var lastPausedEvent: OperatingModeChange? = null
        operatingModeChanges.forEach {
            val therapyEventType = when (it.to) {
                OperatingModeChange.OperatingMode.STARTED -> {
                    if (it.from == OperatingModeChange.OperatingMode.STOPPED) {
                        saveSuspendTBR(it, lastStoppedEvent, lastPausedEvent)
                    }
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STARTED
                }
                OperatingModeChange.OperatingMode.PAUSED -> {
                    lastPausedEvent = it
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_PAUSED
                }
                OperatingModeChange.OperatingMode.STOPPED -> {
                    lastStoppedEvent = it
                    if (it.from != OperatingModeChange.OperatingMode.PAUSED) lastPausedEvent = null
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STOPPED
                }
            }
            database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    type = therapyEventType
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = it.eventId
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        }
    }

    private fun updateExistingTemporaryBasal(temporaryBasal: TemporaryBasal) {
        var dbTBR = database.temporaryBasalDao.getWithSmallerStartId_Within24Hours_WithPumpSerial_PumpAndEndIdAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, temporaryBasal.timestamp, temporaryBasal.eventId)
        if (dbTBR != null) {
            dbTBR.duration = temporaryBasal.duration
            dbTBR.interfaceIDs.endId = temporaryBasal.eventId
            database.temporaryBasalDao.updateExistingEntry(dbTBR)
        } else {
            dbTBR = TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(temporaryBasal.timestamp).toLong(),
                    timestamp = temporaryBasal.timestamp,
                    absolute = false,
                    rate = temporaryBasal.percentage.toDouble(),
                    duration = temporaryBasal.duration,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.NORMAL
            )
            dbTBR.interfaceIDs.pumpSerial = pumpSerial
            dbTBR.interfaceIDs.endId = temporaryBasal.eventId
            database.temporaryBasalDao.insertNewEntry(dbTBR)
        }
    }

    private fun processTemporaryBasals() {
        val tbrs = temporaryBasals.toMutableList()
        tbrs.firstOrNull()?.let {
            if (!it.start) {
                updateExistingTemporaryBasal(it)
                tbrs.removeAt(0)
            }
        }
        val tbrPairs = mutableListOf<Pair<TemporaryBasal, TemporaryBasal?>>()
        for (i in 0 until tbrs.size step 2) {
            val first = tbrs.get(i)
            val second = tbrs.getOrNull(i + 1)
            if (!first.start || second?.start == true) throw IllegalArgumentException("InsightHistoryTransaction.TemporaryBasals must be alternately start and end events.")
            tbrPairs.add(tbrs.get(i) to tbrs.getOrNull(i + 1))
        }
        tbrPairs.forEach {
            val percentage = it.first.percentage
            val duration = it.second?.duration ?: it.first.duration
            val timestamp = it.first.timestamp
            val id = database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    absolute = false,
                    rate = percentage.toDouble(),
                    duration = duration,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.NORMAL
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.startId = it.first.eventId
                interfaceIDs.endId = it.second?.eventId
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        }
    }

    private fun saveStandardBolus(timestamp: Long, utcOffset: Long, amount: Double, bolusId: Long, startId: Long?, endId: Long?): Pair<Long, Boolean> {
        var bolus = if (startId == null && endId != null) {
            database.bolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
        } else {
            database.bolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
        }
        if (bolus != null) {
            bolus.amount = amount
            if (startId != null) bolus.interfaceIDs.startId = startId
            if (endId != null) bolus.interfaceIDs.endId = endId
            database.bolusDao.updateExistingEntry(bolus)
            return bolus.id to false
        } else {
            bolus = Bolus(
                    utcOffset = utcOffset,
                    timestamp = timestamp,
                    amount = amount,
                    type = info.nightscout.androidaps.database.entities.Bolus.Type.NORMAL,
                    basalInsulin = false
            )
            bolus.interfaceIDs.pumpId = bolusId
            bolus.interfaceIDs.startId = startId
            bolus.interfaceIDs.endId = endId
            bolus.interfaceIDs.pumpSerial = pumpSerial
            bolus.interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            return database.bolusDao.insertNewEntry(bolus) to true
        }
    }

    private fun saveExtendedBolus(timestamp: Long, utcOffset: Long, amount: Double, duration: Long, bolusId: Long, startId: Long?, endId: Long?): Pair<Long, Boolean> {
        var extendedBolus = if (startId == null && endId != null) {
            database.extendedBolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
        } else {
            database.extendedBolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
        }
        if (extendedBolus != null) {
            extendedBolus.amount = amount
            extendedBolus.duration = duration
            if (startId != null) extendedBolus.interfaceIDs.startId = startId
            if (endId != null) extendedBolus.interfaceIDs.endId = endId
            database.extendedBolusDao.updateExistingEntry(extendedBolus)
            return extendedBolus.id to false
        } else {
            extendedBolus = ExtendedBolus(
                    utcOffset = utcOffset,
                    timestamp = timestamp,
                    amount = amount,
                    duration = duration,
                    emulatingTempBasal = false
            )
            extendedBolus.interfaceIDs.pumpId = bolusId
            extendedBolus.interfaceIDs.startId = startId
            extendedBolus.interfaceIDs.endId = endId
            extendedBolus.interfaceIDs.pumpSerial = pumpSerial
            extendedBolus.interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            return database.extendedBolusDao.insertNewEntry(extendedBolus) to true
        }
    }

    private fun saveMultiwaveBolusLink(create: Boolean, bolusId: Long, startId: Long?, endId: Long?, standardBolusId: Long, extendedBolusId: Long): Long {
        if (create) {
            return database.multiwaveBolusLinkDao.insertNewEntry(MultiwaveBolusLink(
                    bolusId = standardBolusId,
                    extendedBolusId = extendedBolusId
            ).apply {
                interfaceIDs.pumpId = bolusId
                interfaceIDs.startId = startId
                interfaceIDs.endId = endId
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            })
        } else {
            val multiwaveBolusLink = database.multiwaveBolusLinkDao.findByBolusIDs(standardBolusId, extendedBolusId)!!
            if (startId != null) multiwaveBolusLink.interfaceIDs.startId = startId
            if (endId != null) multiwaveBolusLink.interfaceIDs.endId = endId
            database.multiwaveBolusLinkDao.updateExistingEntry(multiwaveBolusLink)
            return multiwaveBolusLink.id
        }
    }

    private fun processBoluses() {
        boluses.groupBy { it.bolusId }
                .map { entry -> entry.value.find { it.start } to entry.value.findLast { !it.start } }
                .forEach {
            val type = it.first?.type ?: it.second!!.type
            val timestamp = it.first?.timestamp ?: it.second!!.timestamp
            val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
            val bolusId = (it.first?.bolusId ?: it.second!!.bolusId).toLong()
            val startId = it.first?.eventId
            val endId = it.second?.eventId
            val (bolusDatabaseId, bolusCreated) = if (type == Bolus.Type.STANDARD || type == Bolus.Type.MULTIWAVE) {
                saveStandardBolus(timestamp, utcOffset, it.second?.immediateAmount ?: it.first!!.immediateAmount, bolusId, startId, endId)
            } else {
                null to false
            }
            val (extendedBolusDatabaseId, extendedBolusCreated) = if (type == Bolus.Type.EXTENDED || type == Bolus.Type.MULTIWAVE) {
                saveExtendedBolus(timestamp, utcOffset, it.second?.extendedAmount ?: it.first!!.extendedAmount, it.second?.duration ?: it.first!!.duration, bolusId, startId, endId)
            } else {
                null to false
            }
            if (bolusDatabaseId != null && extendedBolusDatabaseId != null) {
                saveMultiwaveBolusLink(bolusCreated && extendedBolusCreated, bolusId, startId, endId, bolusDatabaseId, extendedBolusDatabaseId)
            }
        }
    }

    data class TotalDailyDose(
            val eventId: Long,
            val timestamp: Long,
            val bolusAmount: Double,
            val basalAmount: Double
    )

    data class OperatingModeChange(
            val eventId: Long,
            val timestamp: Long,
            val from: OperatingMode,
            val to: OperatingMode
    ) {
        enum class OperatingMode {
            STARTED,
            STOPPED,
            PAUSED
        }
    }

    data class TherapyEvent(
            val eventId: Long,
            val timestamp: Long,
            val type: Type
    ) {
        enum class Type {
            CANNULA_FILLED,
            TUBE_FILLED,
            RESERVOIR_CHANGED,
            OCCLUSION,
            BATTERY_EMPTY,
            BATTERY_CHANGED,
            RESERVOIR_EMPTY
        }
    }

    data class TemporaryBasal(
            val start: Boolean,
            val eventId: Long,
            val timestamp: Long,
            val duration: Long,
            val percentage: Int
    )

    data class Bolus(
            val start: Boolean,
            val eventId: Long,
            val type: Type,
            val timestamp: Long,
            val bolusId: Int,
            val immediateAmount: Double,
            val duration: Long,
            val extendedAmount: Double
    ) {
        enum class Type {
            STANDARD,
            MULTIWAVE,
            EXTENDED
        }
    }

}