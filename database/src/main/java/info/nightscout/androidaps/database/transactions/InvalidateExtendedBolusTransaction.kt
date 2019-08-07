package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateExtendedBolusTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val extendedBolus = database.extendedBolusDao.findById(id)
                ?: throw IllegalArgumentException("There is no such ExtendedBolus with the specified ID.")
        extendedBolus.valid = false
        database.extendedBolusDao.updateExistingEntry(extendedBolus)
    }
}