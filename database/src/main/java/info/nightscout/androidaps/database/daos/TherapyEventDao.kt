package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_THERAPY_EVENTS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TherapyEvent
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
internal interface TherapyEventDao : BaseDao<TherapyEvent> {

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE id = :id")
    override fun findById(id: Long): TherapyEvent?

    @Query("DELETE FROM $TABLE_THERAPY_EVENTS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type IN ('PUMP_STARTED', 'PUMP_STOPPED', 'PUMP_PAUSED') AND pumpId < :pumpId AND pumpSerial = :pumpSerial AND pumpType = :pumpType AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun getOperatingModeEventForPumpWithSmallerPumpId(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long): TherapyEvent?

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type = :type AND timestamp = :timestamp")
    fun findByTimestamp(type: TherapyEvent.Type, timestamp: Long): TherapyEvent?

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type = :type AND timestamp <= :timestamp AND (timestamp + duration) > :timestamp AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getTherapyEventActiveAt(type: TherapyEvent.Type, timestamp: Long): TherapyEvent?

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type = :type AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getLastTherapyEventByType(type: TherapyEvent.Type): Maybe<TherapyEvent>

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getTherapyEventsInTimeRange(start: Long, end: Long): Flowable<List<TherapyEvent>>

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type = :type AND timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getTherapyEventsInTimeRange(type: TherapyEvent.Type, start: Long, end: Long): Flowable<List<TherapyEvent>>

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getAllTherapyEvents(): Flowable<List<TherapyEvent>>

}