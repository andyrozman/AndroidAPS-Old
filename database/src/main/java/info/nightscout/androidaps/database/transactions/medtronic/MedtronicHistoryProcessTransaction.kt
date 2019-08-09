package info.nightscout.androidaps.database.transactions.medtronic

import com.google.gson.Gson
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime
import info.nightscout.androidaps.database.transactions.Transaction
import org.slf4j.LoggerFactory
import java.util.*

class MedtronicHistoryProcessTransaction(
        private val pumpSerial: String,
        private val tddList : MutableList<PumpHistoryEntry>,
        private val bolusList : MutableList<PumpHistoryEntry>,
        private val temporaryBasalList : MutableList<PumpHistoryEntry>,
        private var oldTbrEntryEdited: Boolean,
        private val suspendResumeList : List<MDTTempBasalProcess>,
        private val logEnabled : Boolean
) : Transaction<Unit>() {

    private val LOG = LoggerFactory.getLogger("DATABASE")
    //private val logEnabled = logEnabledInput
    private val medtronicUtil = MedtronicUtilKotlin()
    private val gson = Gson()

    override  fun run() {

        // TDD
        if (doesCollectionHaveData(tddList)) {
            try {
                processTDDs(tddList)
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing TDD entries: " + ex.message, ex)
                throw ex
            }
        }


        // Bolus
        if (doesCollectionHaveData(bolusList)) {
            try {
                processBolusEntries(bolusList)
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing Bolus entries: " + ex.message, ex)
                throw ex
            }
        }

        // TBR
        if (doesCollectionHaveData(temporaryBasalList)) {
            try {
                processTBREntries(temporaryBasalList)
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing TBR entries: " + ex.message, ex)
                throw ex
            }
        }

        // 'Delivery Suspend'
        if (doesCollectionHaveData(suspendResumeList)) {
            try {
                processSuspends(suspendResumeList)
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing Suspends entries: " + ex.message, ex)
                throw ex
            }
        }

    }

    private fun doesCollectionHaveData(checkList: List<Any>?) : Boolean {
        return (checkList!=null && checkList.isNotEmpty())
    }


    private fun processTDDs(tddsIn: List<PumpHistoryEntry>) {

        val tddsDb = database.totalDailyDoseDao.getTotalDailyDosesByCountAndPump(3, InterfaceIDs.PumpType.MEDTRONIC, pumpSerial)

        for (tdd in tddsIn) {

            val tddDbEntry = findTDD(tdd.atechDateTime, tddsDb)

            val totalsDTO = tdd.dataObject as MDTDailyTotals

            if (tddDbEntry == null) {

                val timestamp = medtronicUtil.toMillisFromATD(tdd.atechDateTime)

                if (logEnabled)
                    LOG.debug("TDD Add: {}", totalsDTO)


                val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()

                database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
                        timestamp = timestamp,
                        utcOffset = utcOffset,
                        basalAmount = totalsDTO.basalInsulin,
                        bolusAmount = totalsDTO.bolusInsulin,
                        totalAmount = totalsDTO.totalInsulin
                ).apply {
                    interfaceIDs.pumpType = InterfaceIDs.PumpType.MEDTRONIC
                    interfaceIDs.pumpSerial = pumpSerial
                    interfaceIDs.pumpId = totalsDTO.pumpId
                })

            } else {

                if (!totalsDTO.isEqual(tddDbEntry)) {

                    if (logEnabled)
                        LOG.debug("TDD Edit: Before: {}", tddDbEntry)

                    tddDbEntry.basalAmount = totalsDTO.basalInsulin
                    tddDbEntry.bolusAmount = totalsDTO.bolusInsulin
                    tddDbEntry.totalAmount = totalsDTO.totalInsulin
                    tddDbEntry.interfaceIDs.pumpId = totalsDTO.pumpId

                    if (logEnabled)
                        LOG.debug("TDD Edit: After: {}", tddDbEntry)

                    database.totalDailyDoseDao.updateExistingEntry(tddDbEntry)
                }
            }
        }

    }


    private fun processBolusEntries(entryList: MutableList<PumpHistoryEntry>) {
        val oldestTimestamp = getOldestTimestamp(entryList)

        val entriesFromHistory = getDatabaseEntriesByLastTimestamp(oldestTimestamp, ProcessHistoryRecord.Bolus)

//        LOG.debug(processHistoryRecord.getDescription() + " List (before filter): {}, FromDb={}", gsonPretty.toJson(entryList),
//                gsonPretty.toJson(entriesFromHistory));

        filterOutAlreadyAddedEntries(entryList, entriesFromHistory)

        if (entryList.isEmpty())
            return

//        LOG.debug(processHistoryRecord.getDescription() + " List (after filter): {}, FromDb={}", gsonPretty.toJson(entryList),
//                gsonPretty.toJson(entriesFromHistory));

        if (!doesCollectionHaveData(entriesFromHistory)) {
            for (treatment in entryList) {
                if (logEnabled)
                    LOG.debug("Add Bolus (no db entry): $treatment")

                addBolus(treatment, null)
            }
        } else {
            for (treatment in entryList) {
                val treatmentDb = findDbEntry(treatment, entriesFromHistory)
                if (logEnabled)
                    LOG.debug("Add Bolus {} - (entryFromDb={}) ", treatment, treatmentDb)

                addBolus(treatment, treatmentDb as Bolus)
            }
        }

    }


    private fun processTBREntries(entryList: List<PumpHistoryEntry>) {

        val oldestTimestamp = getOldestTimestamp(entryList)

        val entriesFromHistory = getDatabaseEntriesByLastTimestamp(oldestTimestamp, ProcessHistoryRecord.TBR)

        if (logEnabled)
            LOG.debug(ProcessHistoryRecord.TBR.description + " List (before filter): {}, FromDb={}", gson.toJson(entryList),
                    gson.toJson(entriesFromHistory))


        var processDTO: MDTTempBasalProcess? = null
        val processList = ArrayList<MDTTempBasalProcess>()

        for (treatment in entryList) {

            val tbr2 = treatment.dataObject as MDTTemporaryBasal

            if (tbr2.isCancelTBR()) {

                if (processDTO != null) {
                    processDTO.itemTwo = tbr2

                    if (oldTbrEntryEdited) {
                        processDTO.processOperation = MDTTempBasalProcess.Operation.Edit
                        oldTbrEntryEdited = false
                    }
                } else {
                    LOG.error("processDTO was null - shouldn't happen. ItemTwo={}", treatment)
                }
            } else {
                if (processDTO != null) {
                    processList.add(processDTO)
                }

                processDTO = MDTTempBasalProcess()
                processDTO.itemOne = tbr2
                processDTO.processOperation = MDTTempBasalProcess.Operation.Add
            }
        }

        if (processDTO != null) {
            processList.add(processDTO)
        }


        if (doesCollectionHaveData(processList)) {

            for (tempBasalProcessDTO in processList) {

                if (tempBasalProcessDTO.processOperation == MDTTempBasalProcess.Operation.Edit) {
                    // edit
                    val tempBasal = findTempBasalWithPumpId(tempBasalProcessDTO.itemOne!!.pumpId, entriesFromHistory)

                    if (tempBasal != null) {

                        tempBasal.duration = tempBasalProcessDTO.calculateDuration()

                        database.temporaryBasalDao.updateExistingEntry(tempBasal)

                        if (logEnabled)
                            LOG.debug("Edit " + ProcessHistoryRecord.TBR.description + " - (entryFromDb={}) ", tempBasal)
                    } else {
                        LOG.error("TempBasal not found, can't edit. Item: {}", tempBasalProcessDTO.itemOne)
                    }

                } else {
                    // add

                    val tbr2 = tempBasalProcessDTO.itemOne

                    //val tbr2 = treatment.getDecodedData().get("Object") as TempBasalPair
                    tbr2!!.duration = tempBasalProcessDTO.calculateDuration()

                    val tempBasal = findTempBasalWithPumpId(tempBasalProcessDTO.itemOne!!.pumpId, entriesFromHistory)

                    if (tempBasal == null) {
                        val treatmentDb = findDbEntry(tbr2, entriesFromHistory)

                        if (logEnabled)
                            LOG.debug("Add " + ProcessHistoryRecord.TBR.description + " {} - (entryFromDb={}) ", tbr2, treatmentDb)

                        addTBR(tbr2, treatmentDb as TemporaryBasal?)
                    } else {
                        // this shouldn't happen
                        if (tempBasal.duration != tempBasalProcessDTO.calculateDuration()) {
                            LOG.debug("Found entry with wrong duration (shouldn't happen)... updating")

                            tempBasal.duration = tempBasalProcessDTO.calculateDuration()
                            database.temporaryBasalDao.updateExistingEntry(tempBasal)
                        }

                    }
                } // if
            } // for

        } // collection

    }


    private fun processSuspends(tempBasalProcessList: List<MDTTempBasalProcess>) {
        for (tempBasalProcess in tempBasalProcessList) {

            val tempBasal: TemporaryBasal? = database.temporaryBasalDao.getTemporaryBasalByPumpId(InterfaceIDs.PumpType.MEDTRONIC, pumpSerial, tempBasalProcess.itemOne!!.pumpId)

            if (tempBasal == null) {
                val timestamp = tryToGetByLocalTime(tempBasalProcess.itemOne!!.atechDateTime)

                addTemporaryBasalDb(timestamp, 0.0, tempBasalProcess.calculateDuration(), tempBasalProcess.itemOne!!.pumpId)

            }
        }
    }


    private fun getOldestTimestamp(treatments: List<PumpHistoryEntry>?): Long {

        var dt = java.lang.Long.MAX_VALUE
        var currentTreatment: PumpHistoryEntry? = null

        for (treatment in treatments!!) {

            if (treatment.atechDateTime < dt) {
                dt = treatment.atechDateTime
                currentTreatment = treatment
            }
        }

        try {

            val oldestEntryTime = medtronicUtil.toGregorianCalendar(dt)
            oldestEntryTime.add(Calendar.MINUTE, -2)

            return oldestEntryTime.timeInMillis

        } catch (ex: Exception) {
            LOG.error("Problem decoding date from last record: {}" + currentTreatment!!)
            val gcnow = GregorianCalendar()
            gcnow.add(Calendar.MINUTE, -8)
            return gcnow.timeInMillis // default return of 8 minutes
        }

    }


    private fun getDatabaseEntriesByLastTimestamp(startTimestamp: Long, processHistoryRecord: ProcessHistoryRecord): MutableList<out DBEntryWithTime> {
        return if (processHistoryRecord == ProcessHistoryRecord.Bolus) {
            database.bolusDao.getBolusesStartingWithTimeForPump(startTimestamp, InterfaceIDs.PumpType.MEDTRONIC, pumpSerial)
        } else {
            database.temporaryBasalDao.getTemporaryBasalsStartingWithTimeForPump(startTimestamp, InterfaceIDs.PumpType.MEDTRONIC, pumpSerial)
        }
    }


    private fun findTDD(atechDateTime: Long, tddsDb: List<TotalDailyDose>): TotalDailyDose? {

        for (tdd in tddsDb) {

            if (medtronicUtil.isSameDayATDAndMillis(atechDateTime, tdd.timestamp)) {
                return tdd
            }
        }

        return null
    }


    private fun tryToGetByLocalTime(atechDateTime: Long): Long {
        return medtronicUtil.toMillisFromATD(atechDateTime)
    }


    private fun filterOutAlreadyAddedEntries(entryList: MutableList<PumpHistoryEntry>, treatmentsFromHistory: MutableList<out DBEntryWithTime>) {

        if (!doesCollectionHaveData(treatmentsFromHistory))
            return

        val removeTreatmentsFromHistory = ArrayList<DBEntry>()

        for (treatment in treatmentsFromHistory) {

            val pumpId : Long? = treatment.interfaceIDs.pumpId

            if (pumpId !=null && pumpId > 0) {

                var selectedBolus: PumpHistoryEntry? = null

                for (bolus in entryList) {
                    if (bolus.pumpId == pumpId) {
                        selectedBolus = bolus
                        break
                    }
                }

                if (selectedBolus != null) {
                    entryList.remove(selectedBolus)

                    removeTreatmentsFromHistory.add(treatment)
                }
            }
        }

        treatmentsFromHistory.removeAll(removeTreatmentsFromHistory)
    }


    private fun addBolus(bolus: PumpHistoryEntry, bolusDb: Bolus?) {

        val bolusDTO = bolus.dataObject as MDTBolus

        val timestamp = tryToGetByLocalTime(bolus.atechDateTime)

        if (bolusDb==null) {

            when (bolusDTO.bolusType) {
                MDTBolus.Type.NORMAL -> {

                    addBolusDb(timestamp,
                            bolusDTO.amount,
                            bolusDTO.carbs,
                            bolusDTO.pumpId,
                            null
                    )

                    if (logEnabled)
                        LOG.debug("addBolus - [date={},pumpId={}, insulin={}]", timestamp,
                                bolusDTO.pumpId, bolusDTO.amount)
                }

                MDTBolus.Type.EXTENDED -> {

                    addBolusDb(timestamp,
                            bolusDTO.amount,
                            bolusDTO.carbs,
                            bolusDTO.pumpId,
                            bolusDTO.duration
                    )

                    if (logEnabled)
                        LOG.debug("addBolus - Extended [date={},pumpId={}, insulin={}, duration={}]", timestamp,
                                bolusDTO.pumpId, bolusDTO.amount, bolusDTO.duration)

                }
            }

        } else {

            if (!bolusDTO.isEqual(bolusDb)) {
                bolusDb.amount = bolusDTO.amount
                bolusDb.interfaceIDs.pumpId = bolusDTO.pumpId
            }

            database.bolusDao.updateExistingEntry(bolusDb)

            if (logEnabled)
                LOG.debug("editBolus - [date={},pumpId={}, insulin={}]", timestamp,
                        bolusDTO.pumpId, bolusDTO.amount)

        }
    }


    private fun addBolusDb(timestamp: Long, insulin: Double, carbs: Double, pumpId: Long, duration: Long?) {

        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()

        if (duration==null) {

            var entries = 1
            val bolusDBId = database.bolusDao.insertNewEntry(Bolus(
                    timestamp = timestamp,
                    utcOffset = utcOffset,
                    amount = insulin,
                    type = Bolus.Type.NORMAL,
                    basalInsulin = false
            ).apply {
                interfaceIDs.pumpType = InterfaceIDs.PumpType.MEDTRONIC
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
            })
            val carbsDBId = if (carbs > 0) {
                entries += 1
                database.carbsDao.insertNewEntry(Carbs(
                        timestamp = timestamp,
                        utcOffset = utcOffset,
                        amount = carbs,
                        duration = 0
                ).apply {
                })
            } else {
                null
            }
            if (entries > 1) {
                database.mealLinkDao.insertNewEntry(MealLink(
                        bolusId = bolusDBId,
                        carbsId = carbsDBId,
                        bolusCalcResultId = null
                ).apply {
                })
            }
        } else {

            database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                    timestamp = timestamp,
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    amount = insulin,
                    duration = (duration * 60000),
                    emulatingTempBasal = false
            ).apply {
                interfaceIDs.pumpType = InterfaceIDs.PumpType.MEDTRONIC
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
            })

        }

    }


    private fun findTempBasalWithPumpId(pumpId: Long, entriesFromHistory: List<DBEntry>): TemporaryBasal? {

        for (dbObjectBase in entriesFromHistory) {
            val tbr = dbObjectBase as TemporaryBasal

            if (tbr.interfaceIDs.pumpId == pumpId) {
                return tbr
            }
        }

        val tempBasal = database.temporaryBasalDao.getTemporaryBasalByPumpId(InterfaceIDs.PumpType.MEDTRONIC, pumpSerial, pumpId)
        return tempBasal
    }


    private fun addTBR(treatment: MDTTemporaryBasal, temporaryBasalDb: TemporaryBasal?) {

        var operation = "addTBR"

        if (temporaryBasalDb == null) {
            addTemporaryBasalDb(tryToGetByLocalTime(treatment.atechDateTime), treatment.amount, treatment.duration * 60000L, treatment.pumpId)
        } else {

            temporaryBasalDb.duration = treatment.duration
            temporaryBasalDb.interfaceIDs.pumpId = treatment.pumpId
            operation = "editTBR"

            database.temporaryBasalDao.updateExistingEntry(temporaryBasalDb)
        }

        if (logEnabled)
            LOG.debug("$operation - [date={},pumpId={}, rate={} {}, duration={}]", //
                    treatment.atechDateTime,
                    treatment.pumpId, //
                    String.format(Locale.ENGLISH, "%.2f", treatment.amount),
                    "U/h", //
                    treatment.duration)
    }


    private fun addTemporaryBasalDb(timestamp: Long, rate: Double, duration: Long, pumpId: Long) {
        database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                type = TemporaryBasal.Type.NORMAL,
                absolute = true,
                rate = rate,
                duration = duration
        ).apply {
            interfaceIDs.pumpType = InterfaceIDs.PumpType.MEDTRONIC
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = pumpId
        })
    }


    /**
     * findDbEntry - finds Db entries in database, while theoretically this should have same dateTime they
     * don't. Entry on pump is few seconds before treatment in AAPS, and on manual boluses on pump there
     * is no treatment at all. For now we look fro tratment that was from 0s - 1m59s within pump entry.
     *
     * @param treatment Pump Entry
     * @param entriesFromHistory entries from history
     *
     * @return DbObject from AAPS (if found)
     */
    private fun findDbEntry(treatment: DbObjectMDT, entriesFromHistory: List<DBEntryWithTime>): DBEntryWithTime? {

        val proposedTime = medtronicUtil.toMillisFromATD(treatment.atechDateTime)

        if (entriesFromHistory.isEmpty()) {
            return null
        } else if (entriesFromHistory.size == 1) {
            return entriesFromHistory[0]
        }

        var min = 0
        while (min < 2) {

            var sec = 0
            while (sec <= 50) {

                if (min == 1 && sec == 50) {
                    sec = 59
                }

                val diff = sec * 1000

                val outList = ArrayList<DBEntryWithTime>()

                for (treatment1 in entriesFromHistory) {

                    if (treatment1.timestamp > proposedTime - diff && treatment1.timestamp < proposedTime + diff) {
                        outList.add(treatment1)
                    }
                }

                //                LOG.debug("Entries: (timeDiff=[min={},sec={}],count={},list={})", min, sec, outList.size(),
                //                        gsonPretty.toJson(outList));

                if (outList.size == 1) {
                    return outList[0]
                }

                if (min == 0 && sec == 10 && outList.size > 1) {
                    if (logEnabled)
                        LOG.error(getLogPrefix() + "Too many entries (with too small diff): (timeDiff=[min={},sec={}],count={},list={})",
                                min, sec, outList.size, gson.toJson(outList))
                }
                sec += 10
            }
            min += 1
        }

        return null
    }

    fun getLogPrefix() : String {
        return "MedtronicHistoryProcessTransaction::"
    }


    data class PumpHistoryEntry(
            override val atechDateTime: Long,
            val type: EntryType = EntryType.None,
            val pumpId: Long? = 0,
            val dataObject: Any? = null
    ) : DbObjectMDT(atechDateTime) {
        enum class EntryType {
            None,
            TDD,
            Bolus,
            TemporaryBasal,
            SuspendResume
        }
    }


    open class DbObjectMDT(
            open val atechDateTime: Long
    )


    data class MDTTemporaryBasal(
            override val atechDateTime: Long,
            var amount: Double,
            var duration: Long,
            var pumpId: Long
    ) : DbObjectMDT(atechDateTime) {

        fun isCancelTBR() : Boolean {
            var mu = MedtronicUtilKotlin()
            return mu.isSame(amount, 0.0) && duration == 0L
        }


    }

    data class MDTBolus(
            override val atechDateTime: Long,
            val bolusType: Type,
            val amount: Double,
            val carbs: Double,
            val duration: Long = 0,
            val pumpId: Long
    ) : DbObjectMDT(atechDateTime) {
        enum class Type {
            NORMAL,
            EXTENDED
        }

        fun isEqual(bolus: Bolus) : Boolean {
            return bolus.amount == amount &&
                    bolus.interfaceIDs.pumpId == pumpId
        }
    }

    data class MDTDailyTotals(
            override val atechDateTime: Long,
            val bolusInsulin: Double,
            val basalInsulin: Double,
            val totalInsulin: Double,
            val pumpId: Long
    ) : DbObjectMDT(atechDateTime) {

        fun isEqual(totalDailyDose: TotalDailyDose) : Boolean {
            return totalDailyDose.basalAmount == basalInsulin &&
                    totalDailyDose.bolusAmount == bolusInsulin &&
                    totalDailyDose.totalAmount == totalInsulin &&
                    totalDailyDose.interfaceIDs.pumpId == pumpId
        }


    }


    data class MDTTempBasalProcess(
            var itemOne: MDTTemporaryBasal? = null,
            var itemTwo: MDTTemporaryBasal? = null,
            var processOperation : Operation = Operation.Undefined
    ) {
        enum class Operation {
            Undefined,
            Add,
            Edit
        }

        fun calculateDuration() : Long {

            var duration = 0L
            var medtronicUtil = MedtronicUtilKotlin()

            if (itemTwo == null) {
                duration = itemOne!!.duration
            } else {
                duration = medtronicUtil.getATechDateDiferenceAsMinutes(itemOne!!.atechDateTime, itemTwo!!.atechDateTime) * 1L
            }

            duration *= 60000L

            return duration
        }

    }


    private enum class ProcessHistoryRecord private constructor(val description: String) {
        Bolus("Bolus"),
        TBR("TBR"),
        Suspend("Suspend")
    }


}