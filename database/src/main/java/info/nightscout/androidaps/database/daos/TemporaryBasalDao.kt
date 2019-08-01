package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_TEMPORARY_BASALS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import io.reactivex.Flowable

@Suppress("FunctionName")
@Dao
abstract class TemporaryBasalDao : BaseDao<TemporaryBasal>() {

    @Query("SELECT * FROM $TABLE_TEMPORARY_BASALS WHERE id = :id")
    abstract override fun findById(id: Long): TemporaryBasal?

    @Query("DELETE FROM $TABLE_TEMPORARY_BASALS")
    abstract override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_TEMPORARY_BASALS WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND startId < :endId AND pumpId IS NULL AND endId IS NULL AND ABS(timestamp - :timestamp) <= 86400000 ORDER BY startId DESC LIMIT 1")
    abstract fun getWithSmallerStartId_Within24Hours_WithPumpSerial_PumpAndEndIdAreNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, timestamp: Long, endId: Long): TemporaryBasal?

    @Query("SELECT * FROM $TABLE_TEMPORARY_BASALS WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getTemporaryBasalsInTimeRange(start: Long, end: Long): Flowable<List<TemporaryBasal>>

    @Query("SELECT * FROM $TABLE_TEMPORARY_BASALS WHERE timestamp <= :timestamp AND (timestamp + duration) > :timestamp AND referenceId IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getTemporaryBasalActiveAt(timestamp: Long): TemporaryBasal?

    @Query("SELECT * FROM $TABLE_TEMPORARY_BASALS WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND timestamp <= :timestamp AND (timestamp + duration) > :timestamp AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun getTemporaryBasalActiveAtForPump(timestamp: Long, pumpType: InterfaceIDs.PumpType, pumpSerial: String): TemporaryBasal?

}