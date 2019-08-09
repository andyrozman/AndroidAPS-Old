package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.AppRepository.database
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertUpdateTemporaryBasalTransaction(
        val timestamp: Long,
        val durationMs: Long = 0,
        val durationMin: Long = 0,
        val absolute: Boolean,
        val rate: Double,
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String,
        val pumpId: Long = 0
) : Transaction<Unit>() {

    override fun run() {

        var currentTemporaryBasal : TemporaryBasal?

        var duration : Long

        if (durationMs>0)
            duration = durationMs
        else
            duration = durationMin * 60000L

        if (pumpId > 0) {
            currentTemporaryBasal = database.temporaryBasalDao.getTemporaryBasalByPumpId(pumpType, pumpSerial, pumpId)
        } else {
            currentTemporaryBasal = database.temporaryBasalDao.getTemporaryBasalByTimeAndPump(timestamp, pumpType, pumpSerial)
        }

        if (currentTemporaryBasal==null) {
            database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    timestamp = timestamp,
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    type = TemporaryBasal.Type.NORMAL,
                    absolute = absolute,
                    rate = rate,
                    duration = duration
            ).apply {
                interfaceIDs.pumpType = pumpType
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
            })
        } else {
            database.temporaryBasalDao.updateExistingEntry(TemporaryBasal(
                    timestamp = currentTemporaryBasal.timestamp,
                    utcOffset = currentTemporaryBasal.utcOffset,
                    type = currentTemporaryBasal.type,
                    absolute = absolute,
                    rate = rate,
                    duration = duration
            ).apply {
                interfaceIDs.pumpType = pumpType
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
            })
        }
    }
}