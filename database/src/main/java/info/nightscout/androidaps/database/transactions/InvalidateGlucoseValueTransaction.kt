package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateGlucoseValueTransaction(val id: Long) : Transaction<Unit>() {
    override fun run() {
        val glucoseValue = database.glucoseValueDao.findById(id)
                ?: throw IllegalArgumentException("There is no such GlucoseValue with the specified ID.")
        glucoseValue.valid = false
        database.glucoseValueDao.updateExistingEntry(glucoseValue)
    }
}