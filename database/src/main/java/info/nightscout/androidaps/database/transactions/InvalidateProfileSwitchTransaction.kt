package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateProfileSwitchTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val profileSwitch = database.profileSwitchDao.findById(id)
                ?: throw IllegalArgumentException("There is no such ProfileSwitch with the specified ID.")
        profileSwitch.valid = false
        database.profileSwitchDao.updateExistingEntry(profileSwitch)
    }
}