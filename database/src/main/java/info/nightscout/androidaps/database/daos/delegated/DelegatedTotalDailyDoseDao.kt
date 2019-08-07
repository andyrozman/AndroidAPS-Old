package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.TotalDailyDoseDao
import info.nightscout.androidaps.database.entities.TotalDailyDose
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedTotalDailyDoseDao(changes: MutableList<DBEntry>, dao: TotalDailyDoseDao) : DelegatedDao(changes), TotalDailyDoseDao by dao {

    override fun insertNewEntry(entry: TotalDailyDose): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: TotalDailyDose): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}