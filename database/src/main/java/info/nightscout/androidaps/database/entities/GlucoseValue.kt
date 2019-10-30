package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

@Entity(tableName = TABLE_GLUCOSE_VALUES,
        foreignKeys = [ForeignKey(
                entity = GlucoseValue::class,
                parentColumns = ["id"],
                childColumns = ["referenceId"])],
        indices = [Index("referenceId"), Index("timestamp")])
data class GlucoseValue(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var dateCreated: Long = -1,
        override var isValid: Boolean = true,
        override var referenceId: Long? = null,
        @Embedded
        override var interfaceIDs_backing: InterfaceIDs? = InterfaceIDs(),
        override var timestamp: Long,
        override var utcOffset: Long,
        var raw: Double?,
        var value: Double,
        var trendArrow: TrendArrow,
        var noise: Double?,
        var sourceSensor: SourceSensor
) : DBEntry, DBEntryWithTime {

    fun contentEqualsTo(other: GlucoseValue): Boolean {
        return timestamp == other.timestamp &&
                utcOffset == other.utcOffset &&
                raw == other.raw &&
                value == other.value &&
                trendArrow == other.trendArrow &&
                noise == other.noise &&
                sourceSensor == other.sourceSensor
    }

    enum class TrendArrow {
        NONE,
        TRIPLE_UP,
        DOUBLE_UP,
        SINGLE_UP,
        FORTY_FIVE_UP,
        FLAT,
        FORTY_FIVE_DOWN,
        SINGLE_DOWN,
        DOUBLE_DOWN,
        TRIPLE_DOWN
    }

    enum class SourceSensor {
        UNKNOWN,
        DEXCOM_NATIVE_UNKNOWN,
        DEXCOM_G6_NATIVE,
        DEXCOM_G6_XDRIP,
        DEXCOM_G5_NATIVE,
        DEXCOM_G5_XDRIP,
        DEXCOM_G4_NATIVE,
        DEXCOM_G4_XDRIP,
        LIBRE_1_XDRIP,
        TOMATO,
        GLIMP,
        LIBRE_2_NATIVE,
        POCTECH_NATIVE,
        MM_600_SERIES,
        EVERSENSE,
        MEDTRUM_A6
    }
}