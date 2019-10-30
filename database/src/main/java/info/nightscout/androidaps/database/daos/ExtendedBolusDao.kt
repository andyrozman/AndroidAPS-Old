package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_EXTENDED_BOLUSES
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.ExtendedBolus
import io.reactivex.Flowable

@Suppress("FunctionName")
@Dao
internal interface ExtendedBolusDao : BaseDao<ExtendedBolus> {

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE id = :id")
    override fun findById(id: Long): ExtendedBolus?

    @Query("DELETE FROM $TABLE_EXTENDED_BOLUSES")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND startId IS NULL AND endId IS NULL AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun findByPumpId_StartAndEndIDsAreNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): ExtendedBolus?

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE pumpType = :pumpType AND pumpId = :pumpId AND pumpSerial = :pumpSerial AND startId IS NOT NULL AND endId IS NULL AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun findByPumpId_StartIdIsNotNull_EndIdIsNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): ExtendedBolus?

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun findByPumpId(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): ExtendedBolus?

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getExtendedBolusesInTimeRange(start: Long, end: Long): Flowable<List<ExtendedBolus>>

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE timestamp <= :timestamp AND (timestamp + duration) > :timestamp AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getExtendedBolusActiveAt(timestamp: Long): ExtendedBolus?
}