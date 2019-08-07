package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_MEAL_LINKS
import info.nightscout.androidaps.database.TABLE_MULTIWAVE_BOLUS_LINKS
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink

@Suppress("FunctionName")
@Dao
interface MultiwaveBolusLinkDao : BaseDao<MultiwaveBolusLink> {

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE id = :id")
    override fun findById(id: Long): MultiwaveBolusLink?

    @Query("DELETE FROM $TABLE_MEAL_LINKS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE bolusId = :bolusId AND extendedBolusId = :extendedBolusId AND referenceId IS NULL")
    fun findByBolusIDs(bolusId: Long, extendedBolusId: Long): MultiwaveBolusLink?
}