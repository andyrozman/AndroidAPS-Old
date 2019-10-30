package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_PROFILE_SWITCHES
import info.nightscout.androidaps.database.checkSanity
import info.nightscout.androidaps.database.daos.workaround.ProfileSwitchDaoWorkaround
import info.nightscout.androidaps.database.entities.ProfileSwitch
import io.reactivex.Flowable

@Suppress("FunctionName")
@Dao
interface ProfileSwitchDao : ProfileSwitchDaoWorkaround {

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE id = :id")
    override fun findById(id: Long): ProfileSwitch?

    @Query("DELETE FROM $TABLE_PROFILE_SWITCHES")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getProfileSwitchesInTimeRange(start: Long, end: Long): Flowable<List<ProfileSwitch>>

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getAllProfileSwitches(): Flowable<List<ProfileSwitch>>
}

fun ProfileSwitchDao.insertNewEntryImpl(entry: ProfileSwitch): Long {
    if (!entry.basalBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for basal blocks.")
    if (!entry.icBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for IC blocks.")
    if (!entry.isfBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for ISF blocks.")
    if (!entry.targetBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for target blocks.")
    return (this as BaseDao<ProfileSwitch>).insertNewEntryImpl(entry)
}

fun ProfileSwitchDao.updateExistingEntryImpl(entry: ProfileSwitch): Long {
    if (!entry.basalBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for basal blocks.")
    if (!entry.icBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for IC blocks.")
    if (!entry.isfBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for ISF blocks.")
    if (!entry.targetBlocks.checkSanity()) throw IllegalArgumentException("Sanity check failed for target blocks.")
    return (this as BaseDao<ProfileSwitch>).updateExistingEntryImpl(entry)
}