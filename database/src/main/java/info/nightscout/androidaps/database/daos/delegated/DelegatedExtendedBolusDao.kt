package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.ExtendedBolusDao
import info.nightscout.androidaps.database.entities.ExtendedBolus
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedExtendedExtendedBolusDao(changes: MutableList<DBEntry>, dao: ExtendedBolusDao) : DelegatedDao(changes), ExtendedBolusDao by dao {

    override fun insertNewEntry(entry: ExtendedBolus): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: ExtendedBolus): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}