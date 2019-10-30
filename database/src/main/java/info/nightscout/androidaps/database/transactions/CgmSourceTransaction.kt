package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.entities.TherapyEvent
import java.util.*

/**
 * Inserts data from a CGM source into the database
 */
class CgmSourceTransaction(
        private val glucoseValues: List<GlucoseValue>,
        private val calibrations: List<Calibration>,
        private val sensorInsertionTime: Long?
) : Transaction<List<GlucoseValue>>() {

    override fun run(): List<info.nightscout.androidaps.database.entities.GlucoseValue> {
        val insertedGlucoseValues = mutableListOf<info.nightscout.androidaps.database.entities.GlucoseValue>()
        glucoseValues.forEach {
            val current = database.glucoseValueDao.findByTimestamp(it.timestamp)
            val glucoseValue = GlucoseValue(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    raw = it.raw,
                    value = it.value,
                    noise = it.noise,
                    trendArrow = it.trendArrow,
                    sourceSensor = it.sourceSensor
            )
            glucoseValue.interfaceIDs.nightscoutId = it.nightscoutId
            when {
                current == null -> {
                    database.glucoseValueDao.insertNewEntry(glucoseValue)
                    insertedGlucoseValues.add(glucoseValue)
                }
                current.contentEqualsTo(glucoseValue) -> return@forEach
                else -> {
                    glucoseValue.id = current.id
                    database.glucoseValueDao.updateExistingEntry(glucoseValue)
                }
            }
        }
        calibrations.forEach {
            if (database.therapyEventDao.findByTimestamp(TherapyEvent.Type.FINGER_STICK_BG_VALUE, it.timestamp) == null) {
                database.therapyEventDao.insertNewEntry(TherapyEvent(
                        timestamp = it.timestamp,
                        utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                        type = TherapyEvent.Type.FINGER_STICK_BG_VALUE,
                        amount = it.value
                ))
            }
        }
        sensorInsertionTime?.let {
            if (database.therapyEventDao.findByTimestamp(TherapyEvent.Type.SENSOR_INSERTED, it) == null) {
                database.therapyEventDao.insertNewEntry(TherapyEvent(
                        timestamp = it,
                        utcOffset = TimeZone.getDefault().getOffset(it).toLong(),
                        type = TherapyEvent.Type.SENSOR_INSERTED
                ))
            }
        }
        return insertedGlucoseValues
    }

    data class GlucoseValue(
            val timestamp: Long,
            val value: Double,
            val raw: Double?,
            val noise: Double?,
            val trendArrow: info.nightscout.androidaps.database.entities.GlucoseValue.TrendArrow,
            val sourceSensor: info.nightscout.androidaps.database.entities.GlucoseValue.SourceSensor,
            val nightscoutId: String? = null
    )

    data class Calibration(
            val timestamp: Long,
            val value: Double
    )
}