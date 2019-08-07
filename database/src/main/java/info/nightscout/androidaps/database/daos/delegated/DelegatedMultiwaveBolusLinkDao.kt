package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.MultiwaveBolusLinkDao
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedMultiwaveBolusLinkDao(changes: MutableList<DBEntry>, dao: MultiwaveBolusLinkDao) : DelegatedDao(changes), MultiwaveBolusLinkDao by dao {

    override fun insertNewEntry(entry: MultiwaveBolusLink): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: MultiwaveBolusLink): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}