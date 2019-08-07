package info.nightscout.androidaps.database.daos

import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_TOTAL_DAILY_DOSES
import info.nightscout.androidaps.database.entities.TotalDailyDose
import io.reactivex.Single

@Suppress("FunctionName")
interface TotalDailyDoseDao : BaseDao<TotalDailyDose> {

    @Query("SELECT * FROM $TABLE_TOTAL_DAILY_DOSES WHERE id = :id")
    override fun findById(id: Long): TotalDailyDose?

    @Query("DELETE FROM $TABLE_TOTAL_DAILY_DOSES")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_TOTAL_DAILY_DOSES WHERE referenceId IS NULL and valid = 1 ORDER BY timestamp DESC LIMIT :amount")
    fun getTotalDailyDoses(amount: Int): Single<List<TotalDailyDose>>

    @Query("SELECT * FROM $TABLE_TOTAL_DAILY_DOSES WHERE referenceId IS NULL and valid = 1 AND pumpType = :pumpType AND pumpSerial = :pumpSerial ORDER BY timestamp DESC LIMIT :amount")
    fun getTotalDailyDosesByCountAndPump(amount: Long, pumpType: InterfaceIDs.PumpType, pumpSerial: String): List<TotalDailyDose>

}
