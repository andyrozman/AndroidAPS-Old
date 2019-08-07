package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateTemporaryBasalTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val tempBasal = database.temporaryBasalDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TemporaryBasal with the specified ID.")
        tempBasal.valid = false
        database.temporaryBasalDao.updateExistingEntry(tempBasal)
    }
}