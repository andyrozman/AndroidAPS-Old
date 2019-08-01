package info.nightscout.androidaps.database.transactions.medtronic

import info.nightscout.androidaps.database.transactions.Transaction

class MedtronicHistoryTransaction(
        val pumpSerial: String,
        val tdds : List<PumpHistoryEntry>
) : Transaction<Unit>() {

    override  fun run() {

    }

}