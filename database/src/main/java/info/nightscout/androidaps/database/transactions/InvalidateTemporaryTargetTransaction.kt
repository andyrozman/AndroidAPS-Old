package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateTemporaryTargetTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val tempBasal = database.temporaryTargetDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TemporaryTarget with the specified ID.")
        tempBasal.valid = false
        database.temporaryTargetDao.updateExistingEntry(tempBasal)
    }
}