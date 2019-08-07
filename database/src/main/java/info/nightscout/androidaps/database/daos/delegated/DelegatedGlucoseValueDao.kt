package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.GlucoseValueDao
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedGlucoseValueDao(changes: MutableList<DBEntry>, dao: GlucoseValueDao) : DelegatedDao(changes), GlucoseValueDao by dao {

    override fun insertNewEntry(entry: GlucoseValue): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: GlucoseValue): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}