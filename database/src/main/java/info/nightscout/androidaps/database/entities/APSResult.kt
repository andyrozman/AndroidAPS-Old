package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_APS_RESULTS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

@Entity(tableName = TABLE_APS_RESULTS,
        foreignKeys = [ForeignKey(
                entity = APSResult::class,
                parentColumns = ["id"],
                childColumns = ["referenceId"])],
        indices = [Index("referenceId"), Index("timestamp")])
data class APSResult(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var dateCreated: Long = -1,
        override var isValid: Boolean = true,
        override var referenceId: Long? = null,
        @Embedded
        override var interfaceIDs_backing: InterfaceIDs? = null,
        override var timestamp: Long,
        override var utcOffset: Long,
        var algorithm: Algorithm,
        var glucoseStatusJson: String,
        var currentTempJson: String,
        var iobDataJson: String,
        var profileJson: String,
        var autosensDataJson: String?,
        var mealDataJson: String,
        var isMicroBolusAllowed: Boolean?,
        var resultJson: String
) : DBEntry, DBEntryWithTime {
        enum class Algorithm {
                MA,
                AMA,
                SMB
        }
}