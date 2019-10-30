package info.nightscout.androidaps.utils

import info.nightscout.androidaps.database.entities.BolusCalculatorResult
import org.json.JSONObject
import java.util.*

fun BolusCalculatorResult.nsJSON(): String {
    val boluscalcJSON = JSONObject()
    boluscalcJSON.put("eventTime", DateUtil.toISOString(Date()))
    boluscalcJSON.put("targetBGLow", targetBGLow)
    boluscalcJSON.put("targetBGHigh", targetBGHigh)
    boluscalcJSON.put("isf", isf)
    boluscalcJSON.put("ic", ic)
    boluscalcJSON.put("iob", -(bolusIOB + basalIOB))
    boluscalcJSON.put("bolusiob", bolusIOB)
    boluscalcJSON.put("basaliob", basalIOB)
    boluscalcJSON.put("bolusiobused", wasBolusIOBUsed)
    boluscalcJSON.put("basaliobused", wasBasalIOBUsed)
    boluscalcJSON.put("bg", glucoseValue)
    boluscalcJSON.put("insulinbg", glucoseInsulin)
    boluscalcJSON.put("insulinbgused", wasGlucoseUsed)
    boluscalcJSON.put("bgdiff", glucoseDifference)
    boluscalcJSON.put("insulincarbs", carbsInsulin)
    boluscalcJSON.put("carbs", carbs)
    boluscalcJSON.put("cob", cob)
    boluscalcJSON.put("cobused", wasCOBUsed)
    boluscalcJSON.put("insulincob", cobInsulin)
    boluscalcJSON.put("othercorrection", otherCorrection)
    boluscalcJSON.put("insulintrend", trendInsulin)
    boluscalcJSON.put("insulin", totalInsulin)
    boluscalcJSON.put("superbolusused", wasSuperbolusUsed)
    boluscalcJSON.put("insulinsuperbolus", superbolusInsulin)
    boluscalcJSON.put("trendused", wasTrendUsed)
    boluscalcJSON.put("insulintrend", trendInsulin)
    boluscalcJSON.put("trend", glucoseTrend)
    boluscalcJSON.put("ttused", wasTempTargetUsed)
    return boluscalcJSON.toString()
}