package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_VERSION_CHANGES
import info.nightscout.androidaps.database.entities.VersionChange

@Dao
interface VersionChangeDao {

    @Query("SELECT * FROM $TABLE_VERSION_CHANGES ORDER BY id DESC LIMIT 1")
    fun getMostRecentVersionChange(): VersionChange?

    @Insert
    fun insert(versionChange: VersionChange)

}