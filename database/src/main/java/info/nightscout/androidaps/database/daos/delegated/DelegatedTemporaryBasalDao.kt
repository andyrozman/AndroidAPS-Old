package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.TemporaryBasalDao
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedTemporaryBasalDao(changes: MutableList<DBEntry>, dao: TemporaryBasalDao) : DelegatedDao(changes), TemporaryBasalDao by dao {

    override fun insertNewEntry(entry: TemporaryBasal): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: TemporaryBasal): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}