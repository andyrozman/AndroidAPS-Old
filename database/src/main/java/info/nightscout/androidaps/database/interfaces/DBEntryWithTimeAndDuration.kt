package info.nightscout.androidaps.database.interfaces

import kotlin.math.min

interface DBEntryWithTimeAndDuration : DBEntryWithTime, DBEntryWithDuration

@JvmOverloads
fun DBEntryWithTimeAndDuration.getRemainingDuration(current: Long = System.currentTimeMillis()) = min(0L, timestamp + duration - current)