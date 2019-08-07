package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.BolusCalculatorResultDao
import info.nightscout.androidaps.database.entities.BolusCalculatorResult
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedBolusCalculatorResultDao(changes: MutableList<DBEntry>, dao: BolusCalculatorResultDao) : DelegatedDao(changes), BolusCalculatorResultDao by dao {

    override fun insertNewEntry(entry: BolusCalculatorResult): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: BolusCalculatorResult): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}