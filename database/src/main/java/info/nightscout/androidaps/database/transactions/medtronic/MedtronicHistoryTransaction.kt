package info.nightscout.androidaps.database.transactions.medtronic

import info.nightscout.androidaps.database.transactions.Transaction
import info.nightscout.androidaps.database.transactions.medtronic.entry.MDTPumpHistoryEntry

class MedtronicHistoryTransaction(
        val pumpSerial: String,
        val tddList : List<PumpHistoryEntry>? = null,
        val bolusList : List<MDTPumpHistoryEntry>? = null,
        val temporaryBasalList : List<MDTPumpHistoryEntry>? = null,
        val suspendResumeList : List<MDTPumpHistoryEntry>? = null
) : Transaction<Unit>() {

    override  fun run() {

    }



    data class PumpHistoryEntry(
            val atechDateTime: Long,
            val type: EntryType,
            val pumpId: Long,
            val dataObject: DbObjectMDT
    ) {
        enum class EntryType {
            TDD,
            Bolus,
            TemporaryBasal,
            SuspendResume
        }
    }


    open class DbObjectMDT(
            open val atechDateTime: Long?
    )


    data class TemporaryBasal(
            override val atechDateTime: Long?,
            val start: Boolean?,
            val eventId: Long?,
            val timestamp: Long?,
            val duration: Long?,
            val percentage: Int?
    ) : DbObjectMDT(atechDateTime)

    data class Bolus(
            val start: Boolean,
            val eventId: Long,
            val type: Type,
            val timestamp: Long,
            val bolusId: Int,
            val immediateAmount: Double,
            val duration: Long,
            val extendedAmount: Double
    ) {
        enum class Type {
            STANDARD,
            MULTIWAVE,
            EXTENDED
        }
    }



}