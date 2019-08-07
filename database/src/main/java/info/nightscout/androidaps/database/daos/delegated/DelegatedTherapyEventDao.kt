package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.TherapyEventDao
import info.nightscout.androidaps.database.entities.TherapyEvent
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedTherapyEventDao(changes: MutableList<DBEntry>, dao: TherapyEventDao) : DelegatedDao(changes), TherapyEventDao by dao {

    override fun insertNewEntry(entry: TherapyEvent): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: TherapyEvent): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}