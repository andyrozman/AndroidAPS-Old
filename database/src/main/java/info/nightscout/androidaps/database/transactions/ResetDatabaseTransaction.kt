package info.nightscout.androidaps.database.transactions

/**
 * Resets the database; deletes all entries
 */
class ResetDatabaseTransaction : Transaction<Unit>() {
    override fun run() {
        database.clearAllTables()
    }
}