package info.nightscout.androidaps.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import info.nightscout.androidaps.database.TABLE_PREFERENCE_CHANGES

@Entity(tableName = TABLE_PREFERENCE_CHANGES)
data class PreferenceChange(
        @PrimaryKey
        val id: Long = 0L,
        var timestamp: Long,
        var utcOffset: Long,
        var key: String,
        @TypeConverters
        var value: Any
)