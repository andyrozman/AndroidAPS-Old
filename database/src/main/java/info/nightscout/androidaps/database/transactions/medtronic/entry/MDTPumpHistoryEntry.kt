package info.nightscout.androidaps.database.transactions.medtronic.entry

import androidx.room.Embedded
import androidx.room.PrimaryKey
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

data class MDTPumpHistoryEntry (
        val time: Long
)