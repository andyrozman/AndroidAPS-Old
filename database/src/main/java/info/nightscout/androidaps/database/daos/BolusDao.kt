package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_BOLUSES
import info.nightscout.androidaps.database.TABLE_TEMPORARY_BASALS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.TemporaryBasal
import io.reactivex.Flowable

@Suppress("FunctionName")
@Dao
abstract class BolusDao : BaseDao<Bolus>() {

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE id = :id")
    abstract override fun findById(id: Long): Bolus?

    @Query("DELETE FROM $TABLE_BOLUSES")
    abstract override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND startId IS NULL AND endId IS NULL AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun findByPumpId_StartAndEndIDsAreNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): Bolus?

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE pumpType = :pumpType AND pumpId = :pumpId AND pumpSerial = :pumpSerial AND startId IS NOT NULL AND endId IS NULL AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun findByPumpId_StartIdIsNotNull_EndIdIsNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): Bolus?

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun findByPumpId(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): Bolus?

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getBolusesInTimeRange(start: Long, end: Long): List<Bolus>

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE timestamp >= :start AND referenceId IS NULL AND pumpType = :pumpType AND pumpSerial = :pumpSerial AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getBolusesStartingWithTimeForPump(start: Long, pumpType: InterfaceIDs.PumpType, pumpSerial: Long): List<Bolus>

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun getBolusByPumpId(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long): Bolus?

    @Query("SELECT * FROM $TABLE_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND timestamp = :timestamp AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun getBolusByTimeAndPump(timestamp: Long, pumpType: InterfaceIDs.PumpType, pumpSerial: String): Bolus?

}