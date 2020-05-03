package info.nightscout.androidaps.plugins.pump.danaR

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.R
import info.nightscout.androidaps.data.Profile
import info.nightscout.androidaps.data.ProfileStore
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.treatments.Treatment
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by mike on 04.07.2016.
 */
@Singleton
class DanaRPump @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val sp: SP,
    private val injector: HasAndroidInjector
) {

    var lastConnection: Long = 0
    var lastSettingsRead: Long = 0

    // Info
    var serialNumber = ""
    var shippingDate: Long = 0
    var shippingCountry = ""
    var isNewPump = true
    var password = -1
    var pumpTime: Long = 0
    var model = 0
    var protocol = 0
    var productCode = 0
    var isConfigUD = false
    var isExtendedBolusEnabled = false
    var isEasyModeEnabled = false

    // Status
    var pumpSuspended = false
    var calculatorEnabled = false
    var dailyTotalUnits = 0.0
    var dailyTotalBolusUnits = 0.0 // RS only
    var dailyTotalBasalUnits = 0.0 // RS only
    var maxDailyTotalUnits = 0
    var bolusStep = 0.1
    var basalStep = 0.1
    var iob = 0.0
    var reservoirRemainingUnits = 0.0
    var batteryRemaining = 0
    var bolusBlocked = false
    var lastBolusTime: Long = 0
    var lastBolusAmount = 0.0
    var currentBasal = 0.0
    var isTempBasalInProgress = false
    var tempBasalPercent = 0
    var tempBasalRemainingMin = 0
    var tempBasalTotalSec = 0
    var tempBasalStart: Long = 0
    var isDualBolusInProgress = false
    var isExtendedInProgress = false
    var extendedBolusMinutes = 0
    var extendedBolusAmount = 0.0
    var extendedBolusAbsoluteRate = 0.0
    var extendedBolusSoFarInMinutes = 0
    var extendedBolusStart: Long = 0
    var extendedBolusRemainingMinutes = 0
    var extendedBolusDeliveredSoFar = 0.0 //RS only = 0.0

    // Profile
    var units = 0
    var easyBasalMode = 0
    var basal48Enable = false
    var currentCIR = 0
    var currentCF = 0.0
    var currentAI = 0.0
    var currentTarget = 0.0
    var currentAIDR = 0
    var morningCIR = 0
    var morningCF = 0.0
    var afternoonCIR = 0
    var afternoonCF = 0.0
    var eveningCIR = 0
    var eveningCF = 0.0
    var nightCIR = 0
    var nightCF = 0.0
    var activeProfile = 0

    //var pumpProfiles = arrayOf<Array<Double>>()
    var pumpProfiles: Array<Array<Double>>? = null

    //Limits
    var maxBolus = 0.0
    var maxBasal = 0.0

    // DanaRS specific
    var rsPassword = ""

    // User settings
    var timeDisplayType = 0
    var buttonScrollOnOff = 0
    var beepAndAlarm = 0
    var lcdOnTimeSec = 0
    var backlightOnTimeSec = 0
    var selectedLanguage = 0
    var shutdownHour = 0
    var lowReservoirRate = 0
    var cannulaVolume = 0
    var refillAmount = 0
    var userOptionsFrompump: ByteArray? = null
    var initialBolusAmount = 0.0

    // Bolus settings
    var bolusCalculationOption = 0
    var missedBolusConfig = 0
    fun getUnits(): String {
        return if (units == UNITS_MGDL) Constants.MGDL else Constants.MMOL
    }

    // DanaR,Rv2,RK specific flags
    // last start bolus erroCode
    var messageStartErrorCode: Int = 0
    var historyDoneReceived: Boolean = false
    var bolusingTreatment: Treatment? = null // actually delivered treatment
    var bolusAmountToBeDelivered = 0.0 // amount to be delivered
    var bolusProgressLastTimeStamp: Long = 0 // timestamp of last bolus progress message
    var bolusStopped = false // bolus finished
    var bolusStopForced = false // bolus forced to stop by user

    fun createConvertedProfile(): ProfileStore? {
        pumpProfiles?.let {
            val json = JSONObject()
            val store = JSONObject()
            val profile = JSONObject()
            //        Morning / 6:00–10:59
            //        Afternoon / 11:00–16:59
            //        Evening / 17:00–21:59
            //        Night / 22:00–5:59
            try {
                json.put("defaultProfile", PROFILE_PREFIX + (activeProfile + 1))
                json.put("store", store)
                profile.put("dia", Constants.defaultDIA)
                val carbratios = JSONArray()
                carbratios.put(JSONObject().put("time", "00:00").put("timeAsSeconds", 0).put("value", nightCIR))
                carbratios.put(JSONObject().put("time", "06:00").put("timeAsSeconds", 6 * 3600).put("value", morningCIR))
                carbratios.put(JSONObject().put("time", "11:00").put("timeAsSeconds", 11 * 3600).put("value", afternoonCIR))
                carbratios.put(JSONObject().put("time", "14:00").put("timeAsSeconds", 17 * 3600).put("value", eveningCIR))
                carbratios.put(JSONObject().put("time", "22:00").put("timeAsSeconds", 22 * 3600).put("value", nightCIR))
                profile.put("carbratio", carbratios)
                val sens = JSONArray()
                sens.put(JSONObject().put("time", "00:00").put("timeAsSeconds", 0).put("value", nightCF))
                sens.put(JSONObject().put("time", "06:00").put("timeAsSeconds", 6 * 3600).put("value", morningCF))
                sens.put(JSONObject().put("time", "11:00").put("timeAsSeconds", 11 * 3600).put("value", afternoonCF))
                sens.put(JSONObject().put("time", "17:00").put("timeAsSeconds", 17 * 3600).put("value", eveningCF))
                sens.put(JSONObject().put("time", "22:00").put("timeAsSeconds", 22 * 3600).put("value", nightCF))
                profile.put("sens", sens)
                val basals = JSONArray()
                val basalValues = if (basal48Enable) 48 else 24
                val basalIncrement = if (basal48Enable) 30 * 60 else 60 * 60
                for (h in 0 until basalValues) {
                    var time: String
                    val df = DecimalFormat("00")
                    time = if (basal48Enable) {
                        df.format(h.toLong() / 2) + ":" + df.format(30 * (h % 2).toLong())
                    } else {
                        df.format(h.toLong()) + ":00"
                    }
                    basals.put(JSONObject().put("time", time).put("timeAsSeconds", h * basalIncrement).put("value", it[activeProfile][h]))
                }
                profile.put("basal", basals)
                profile.put("target_low", JSONArray().put(JSONObject().put("time", "00:00").put("timeAsSeconds", 0).put("value", currentTarget)))
                profile.put("target_high", JSONArray().put(JSONObject().put("time", "00:00").put("timeAsSeconds", 0).put("value", currentTarget)))
                profile.put("units", if (units == UNITS_MGDL) Constants.MGDL else Constants.MMOL)
                store.put(PROFILE_PREFIX + (activeProfile + 1), profile)
            } catch (e: JSONException) {
                aapsLogger.error("Unhandled exception", e)
            } catch (e: Exception) {
                return null
            }
            return ProfileStore(injector, json)
        }
        return null
    }

    fun buildDanaRProfileRecord(nsProfile: Profile): Array<Double> {
        val record = Array(24) { 0.0 }
        for (hour in 0..23) {
            //Some values get truncated to the next lower one.
            // -> round them to two decimals and make sure we are a small delta larger (that will get truncated)
            val value = Math.round(100.0 * nsProfile.getBasalTimeFromMidnight((hour * 60 * 60))) / 100.0 + 0.00001
            aapsLogger.debug(LTag.PUMP, "NS basal value for $hour:00 is $value")
            record[hour] = value
        }
        return record
    }

    val isPasswordOK: Boolean
        get() = !(password != -1 && password != sp.getInt(R.string.key_danar_password, -1))

    fun reset() {
        aapsLogger.debug(LTag.PUMP, "DanaRPump reset")
        lastConnection = 0
    }

    companion object {
        const val UNITS_MGDL = 0
        const val UNITS_MMOL = 1
        const val DELIVERY_PRIME = 0x01
        const val DELIVERY_STEP_BOLUS = 0x02
        const val DELIVERY_BASAL = 0x04
        const val DELIVERY_EXT_BOLUS = 0x08
        const val PROFILE_PREFIX = "DanaR-"

        // v2 history entries
        const val TEMPSTART = 1
        const val TEMPSTOP = 2
        const val EXTENDEDSTART = 3
        const val EXTENDEDSTOP = 4
        const val BOLUS = 5
        const val DUALBOLUS = 6
        const val DUALEXTENDEDSTART = 7
        const val DUALEXTENDEDSTOP = 8
        const val SUSPENDON = 9
        const val SUSPENDOFF = 10
        const val REFILL = 11
        const val PRIME = 12
        const val PROFILECHANGE = 13
        const val CARBS = 14
        const val PRIMECANNULA = 15
        const val DOMESTIC_MODEL = 0x01
        const val EXPORT_MODEL = 0x03
    }
}