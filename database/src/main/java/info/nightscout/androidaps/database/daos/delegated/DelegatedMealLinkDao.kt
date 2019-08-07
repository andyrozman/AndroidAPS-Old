package info.nightscout.androidaps.database.daos.delegated

import info.nightscout.androidaps.database.daos.MealLinkDao
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.interfaces.DBEntry

class DelegatedMealLinkDao(changes: MutableList<DBEntry>, dao: MealLinkDao) : DelegatedDao(changes), MealLinkDao by dao {

    override fun insertNewEntry(entry: MealLink): Long {
        changes.add(entry)
        return super.insertNewEntry(entry)
    }

    override fun updateExistingEntry(entry: MealLink): Long {
        changes.add(entry)
        return super.updateExistingEntry(entry)
    }
}