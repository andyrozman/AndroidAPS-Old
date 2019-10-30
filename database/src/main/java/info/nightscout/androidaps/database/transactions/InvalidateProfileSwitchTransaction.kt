package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the ProfileSwitch with the specified id
 */
class InvalidateProfileSwitchTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val profileSwitch = database.profileSwitchDao.findById(id)
                ?: throw IllegalArgumentException("There is no such ProfileSwitch with the specified ID.")
        profileSwitch.isValid = false
        database.profileSwitchDao.updateExistingEntry(profileSwitch)
    }
}