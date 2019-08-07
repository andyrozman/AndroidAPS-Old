package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_PROFILE_SWITCHES
import info.nightscout.androidaps.database.checkSanity
import info.nightscout.androidaps.database.entities.ProfileSwitch
import io.reactivex.Flowable

@Suppress("FunctionName")
@Dao
interface ProfileSwitchDao : BaseDao<ProfileSwitch> {

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE id = :id")
    override fun findById(id: Long): ProfileSwitch?

    @Query("DELETE FROM $TABLE_PROFILE_SWITCHES")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    fun getProfileSwitchesInTimeRange(start: Long, end: Long): Flowable<List<ProfileSwitch>>

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    fun getAllProfileSwitches(): Flowable<List<ProfileSwitch>>

    override fun insertNewEntry(entry: ProfileSwitch): Long {
        if (!entry.basalBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for basal blocks.")
        if (!entry.icBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for IC blocks.")
        if (!entry.isfBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for ISF blocks.")
        if (!entry.targetBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for target blocks.")
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: ProfileSwitch): Long {
        if (!entry.basalBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for basal blocks.")
        if (!entry.icBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for IC blocks.")
        if (!entry.isfBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for ISF blocks.")
        if (!entry.targetBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for target blocks.")
        return super.updateExistingEntry(entry)
    }
}