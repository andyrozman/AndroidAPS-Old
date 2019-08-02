package info.nightscout.androidaps.database.interfaces

interface DBEntryWithDuration : DBEntry {
    var duration: Long

    val durationUnknown get() = duration == Long.MAX_VALUE
}