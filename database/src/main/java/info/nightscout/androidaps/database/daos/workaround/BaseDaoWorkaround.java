package info.nightscout.androidaps.database.daos.workaround;

import androidx.room.Transaction;

import info.nightscout.androidaps.database.daos.BaseDao;
import info.nightscout.androidaps.database.daos.BaseDaoKt;
import info.nightscout.androidaps.database.interfaces.DBEntry;

public interface BaseDaoWorkaround<T extends DBEntry> {

    /**
     * Inserts a new entry
     * @return The ID of the newly generated entry
     */
    @Transaction
    default long insertNewEntry(T entry) {
        return BaseDaoKt.insertNewEntryImpl((BaseDao<T>) this, entry);
    }

    /**
     * Updates an existing entry
     * @return The ID of the newly generated HISTORIC entry
     */
    @Transaction
    default long updateExistingEntry(T entry) {
        return BaseDaoKt.updateExistingEntryImpl((BaseDao<T>) this, entry);
    }

}
