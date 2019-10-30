package info.nightscout.androidaps.utils

import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.database.entities.GlucoseValue

fun GlucoseValue.TrendArrow.toSymbol(): String {
    return when (this) {
        GlucoseValue.TrendArrow.DOUBLE_DOWN -> "\u21ca"
        GlucoseValue.TrendArrow.SINGLE_DOWN -> "\u2193"
        GlucoseValue.TrendArrow.FORTY_FIVE_DOWN -> "\u2198"
        GlucoseValue.TrendArrow.FLAT -> "\u2192"
        GlucoseValue.TrendArrow.FORTY_FIVE_UP -> "\u2197"
        GlucoseValue.TrendArrow.SINGLE_UP -> "\u2191"
        GlucoseValue.TrendArrow.DOUBLE_UP -> "\u21c8"
        else -> "??"
    }
}

fun GlucoseValue.TrendArrow.toText() = when (this) {
    GlucoseValue.TrendArrow.DOUBLE_DOWN -> "DoubleDown"
    GlucoseValue.TrendArrow.SINGLE_DOWN -> "SingleDown"
    GlucoseValue.TrendArrow.FORTY_FIVE_DOWN -> "FortyFiveDown"
    GlucoseValue.TrendArrow.FLAT -> "Flat"
    GlucoseValue.TrendArrow.FORTY_FIVE_UP -> "FortyFiveUp"
    GlucoseValue.TrendArrow.SINGLE_UP -> "SingleUp"
    GlucoseValue.TrendArrow.DOUBLE_UP -> "DoubleUp"
    else -> "NONE"
}

fun String.toTrendArrow() = when (this) {
    "DoubleDown" -> GlucoseValue.TrendArrow.DOUBLE_DOWN
    "SingleDown" -> GlucoseValue.TrendArrow.SINGLE_DOWN
    "FortyFiveDown" -> GlucoseValue.TrendArrow.FORTY_FIVE_DOWN
    "Flat" -> GlucoseValue.TrendArrow.FLAT
    "FortyFiveUp" -> GlucoseValue.TrendArrow.FORTY_FIVE_UP
    "SingleUp" -> GlucoseValue.TrendArrow.SINGLE_UP
    "DoubleUp" -> GlucoseValue.TrendArrow.DOUBLE_UP
    else -> GlucoseValue.TrendArrow.NONE
}

fun valueToUnits(value: Double, units: String): Double {
    return if (units == Constants.MGDL)
        value
    else
        value * Constants.MGDL_TO_MMOLL
}

fun valueToUnitsString(value: Double, units: String): String {
    return if (units == Constants.MGDL)
        DecimalFormatter.to0Decimal(value)
    else
        DecimalFormatter.to1Decimal(value * Constants.MGDL_TO_MMOLL)
}
/*
fun GlucoseValue.convertToBGReading(): BgReading {
    val bgReading = BgReading()
    bgReading.date = timestamp
    bgReading.value = value
    bgReading.raw = raw ?: 0.0
    bgReading.direction = trendArrow.toText()
    bgReading._id = interfaceIDs.nightscoutId
    return bgReading
}

fun List<GlucoseValue>.convertToBGReadings(): List<BgReading> = map { it.convertToBGReading() }*/

fun String?.determineSourceSensor() = when (this) {
    "AndroidAPS-Dexcom" -> GlucoseValue.SourceSensor.DEXCOM_NATIVE_UNKNOWN
    "AndroidAPS-DexcomG6" -> GlucoseValue.SourceSensor.DEXCOM_G6_NATIVE
    "AndroidAPS-DexcomG5" -> GlucoseValue.SourceSensor.DEXCOM_G5_NATIVE
    "Bluetooth Wixel" -> GlucoseValue.SourceSensor.DEXCOM_G4_XDRIP
    "xBridge Wixel" -> GlucoseValue.SourceSensor.DEXCOM_G4_XDRIP
    "G4 Share Receiver" -> GlucoseValue.SourceSensor.DEXCOM_G4_NATIVE
    "Medtrum A6" -> GlucoseValue.SourceSensor.MEDTRUM_A6
    "Network G4" -> GlucoseValue.SourceSensor.DEXCOM_G4_XDRIP
    "Network G4 and xBridge" -> GlucoseValue.SourceSensor.DEXCOM_G4_XDRIP
    "Network G4 and Classic xDrip" -> GlucoseValue.SourceSensor.DEXCOM_G4_XDRIP
    "DexcomG5" -> GlucoseValue.SourceSensor.DEXCOM_G5_XDRIP
    "G6 Native" -> GlucoseValue.SourceSensor.DEXCOM_G6_NATIVE
    "G5 Native" -> GlucoseValue.SourceSensor.DEXCOM_G5_NATIVE
    "Network libre" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "BlueReader" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "Transmiter PL" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "Blucon" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "Tomato" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "Rfduino" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    "LimiTTer" -> GlucoseValue.SourceSensor.LIBRE_1_XDRIP
    else -> GlucoseValue.SourceSensor.UNKNOWN
}