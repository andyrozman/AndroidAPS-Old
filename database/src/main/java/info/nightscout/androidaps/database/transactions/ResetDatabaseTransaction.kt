package info.nightscout.androidaps.database.transactions

class ResetDatabaseTransaction : Transaction<Unit>() {
    override fun run() {
        database.clearAllTables()
    }
}