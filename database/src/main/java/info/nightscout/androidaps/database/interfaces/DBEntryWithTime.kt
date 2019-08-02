package info.nightscout.androidaps.database.interfaces

interface DBEntryWithTime : DBEntry {
    var timestamp: Long
    var utcOffset: Long
}