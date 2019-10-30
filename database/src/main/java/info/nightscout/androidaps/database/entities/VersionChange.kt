package info.nightscout.androidaps.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import info.nightscout.androidaps.database.TABLE_VERSION_CHANGES

@Entity(tableName = TABLE_VERSION_CHANGES)
data class VersionChange(
        @PrimaryKey
        val id: Long = 0L,
        var timestamp: Long,
        var utcOffset: Long,
        var versionCode: Int,
        var versionName: String,
        var gitRemote: String?,
        var commitHash: String?
)