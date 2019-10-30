package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULTS
import info.nightscout.androidaps.database.TABLE_APS_RESULT_LINKS
import info.nightscout.androidaps.database.entities.links.APSResultLink

@Suppress("FunctionName")
@Dao
internal interface APSResultLinkDao : BaseDao<APSResultLink> {

    @Query("SELECT * FROM $TABLE_APS_RESULT_LINKS WHERE id = :id")
    override fun findById(id: Long): APSResultLink?

    @Query("DELETE FROM $TABLE_APS_RESULTS")
    override fun deleteAllEntries()
}