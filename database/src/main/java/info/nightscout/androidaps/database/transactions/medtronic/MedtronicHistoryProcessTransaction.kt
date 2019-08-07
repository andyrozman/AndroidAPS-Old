package info.nightscout.androidaps.database.transactions.medtronic

import com.google.gson.Gson
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.entities.TotalDailyDose
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime
import info.nightscout.androidaps.database.transactions.Transaction
import info.nightscout.androidaps.database.transactions.pump.PumpExtendedBolusTransaction
import info.nightscout.androidaps.database.transactions.pump.PumpInsertMealBolusTransaction
import info.nightscout.androidaps.database.transactions.pump.PumpInsertUpdateBolusTransaction
import info.nightscout.androidaps.database.transactions.pump.PumpInsertUpdateTemporaryBasalTransaction
import org.slf4j.LoggerFactory
import java.util.*

class MedtronicHistoryProcessTransaction(
        val pumpSerial: String,
        val tddList : List<PumpHistoryEntry>,
        val bolusList : List<PumpHistoryEntry>,
        val temporaryBasalList : List<PumpHistoryEntry>,
        var oldTbrEntryEdited: Boolean,
        val suspendResumeList : List<TempBasalProcessDTO>,
        val logEnabledInput : Boolean,
        val pumpTime : PumpClock
) : Transaction<Unit>() {

    val LOG = LoggerFactory.getLogger("DATABASE")
    val logEnabled = logEnabledInput
    val medtronicUtil = MedtronicUtilKotlin();
    val gson = Gson();

    override  fun run() {

        // TDD FIXME
        if (doesCollectionHaveData(tddList)) {
            try {
                processTDDs(tddList);
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing TDD entries: " + ex.message, ex);
                throw ex;
            }
        }


        // Bolus FIXME
        if (doesCollectionHaveData(bolusList)) {
            try {
                processBolusEntries(bolusList);
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing Bolus entries: " + ex.message, ex);
                throw ex;
            }
        }

        // TBR FIXME
        if (doesCollectionHaveData(temporaryBasalList)) {
            try {
                processTBREntries(temporaryBasalList);
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing TBR entries: " + ex.message, ex);
                throw ex;
            }
        }

        // 'Delivery Suspend' FIXME
        if (doesCollectionHaveData(suspendResumeList)) {
            try {
                processSuspends(suspendResumeList);
            } catch (ex: Exception) {
                LOG.error("MedtronicHistoryProcessTransaction: Error processing Suspends entries: " + ex.message, ex);
                throw ex;
            }
        }

    }

    fun doesCollectionHaveData(checkList: List<Any>?) : Boolean {
        return (checkList!=null && checkList.isNotEmpty())
    }


    fun processTDDs(tddsIn: List<PumpHistoryEntry>) : Unit {

        val tddsDb = database.totalDailyDoseDao.getTotalDailyDosesByCountAndPump(3, InterfaceIDs.PumpType.MEDTRONIC, pumpSerial)

        for (tdd in tddsIn) {

            val tddDbEntry = findTDD(tdd.atechDateTime, tddsDb)

            val totalsDTO = tdd.dataObject as MDTDailyTotals

            if (tddDbEntry == null) {

                val timestamp = medtronicUtil.toMillisFromATD(tdd.atechDateTime);

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
                    changes.add(this)
                })

            } else {

                if (!totalsDTO.doesEqual(tddDbEntry)) {
                    totalsDTO.setTDD(tddDbEntry)

                    if (logEnabled)
                        LOG.debug("TDD Edit: {}", tddDbEntry)

                    database.totalDailyDoseDao.updateExistingEntry(TotalDailyDose(
                            timestamp = tddDbEntry.timestamp,
                            utcOffset = tddDbEntry.utcOffset,
                            basalAmount = totalsDTO.basalInsulin,
                            bolusAmount = totalsDTO.bolusInsulin,
                            totalAmount = totalsDTO.totalInsulin
                    ).apply {
                        interfaceIDs.pumpType = InterfaceIDs.PumpType.MEDTRONIC
                        interfaceIDs.pumpSerial = pumpSerial
                        interfaceIDs.pumpId = totalsDTO.pumpId
                        changes.add(this)
                    })
                }
            }
        }

    }


    fun processBolusEntries(entryList: List<PumpHistoryEntry>?) : Unit {
        val oldestTimestamp = getOldestTimestamp(entryList)

        val entriesFromHistory = getDatabaseEntriesByLastTimestamp(oldestTimestamp, ProcessHistoryRecord.Bolus)

//        LOG.debug(processHistoryRecord.getDescription() + " List (before filter): {}, FromDb={}", gsonPretty.toJson(entryList),
//                gsonPretty.toJson(entriesFromHistory));

        filterOutAlreadyAddedEntries(entryList, entriesFromHistory)

        if (entryList!!.isEmpty())
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
                val treatmentDb = findDbEntry(treatment, entriesFromHistory!!)
                if (logEnabled)
                    LOG.debug("Add Bolus {} - (entryFromDb={}) ", treatment, treatmentDb)

                addBolus(treatment, treatmentDb as Bolus)
            }
        }

    }


    fun processTBREntries(entryList: List<PumpHistoryEntry>) : Unit {

        val oldestTimestamp = getOldestTimestamp(entryList)

        val entriesFromHistory = getDatabaseEntriesByLastTimestamp(oldestTimestamp, ProcessHistoryRecord.TBR)

        if (logEnabled)
            LOG.debug(ProcessHistoryRecord.TBR.description + " List (before filter): {}, FromDb={}", gson.toJson(entryList),
                    gson.toJson(entriesFromHistory))


        var processDTO: TempBasalProcessDTO? = null
        val processList = ArrayList<TempBasalProcessDTO>()

        for (treatment in entryList) {

            val tbr2 = treatment.getDecodedDataEntry("Object") as TempBasalPair

            if (tbr2!!.isCancelTBR()) {

                if (processDTO != null) {
                    processDTO.itemTwo = treatment

                    if (oldTbrEntryEdited) {
                        processDTO.processOperation = TempBasalProcessDTO.Operation.Edit
                        oldTbrEntryEdited = false
                    }
                } else {
                    LOG.error("processDTO was null - shouldn't happen. ItemTwo={}", treatment)
                }
            } else {
                if (processDTO != null) {
                    processList.add(processDTO)
                }

                processDTO = TempBasalProcessDTO()
                processDTO.itemOne = treatment
                processDTO.processOperation = TempBasalProcessDTO.Operation.Add
            }
        }

        if (processDTO != null) {
            processList.add(processDTO)
        }


        if (doesCollectionHaveData(processList)) {

            for (tempBasalProcessDTO in processList) {

                if (tempBasalProcessDTO.processOperation == TempBasalProcessDTO.Operation.Edit) {
                    // edit
                    val tempBasal = findTempBasalWithPumpId(tempBasalProcessDTO.itemOne.getPumpId()!!, entriesFromHistory!!)

                    if (tempBasal != null) {

                        tempBasal!!.durationInMinutes = tempBasalProcessDTO.getDuration()

                        databaseHelper.createOrUpdate(tempBasal);

                        if (logEnabled)
                            LOG.debug("Edit " + ProcessHistoryRecord.TBR.getDescription() + " - (entryFromDb={}) ", tempBasal)
                    } else {
                        LOG.error("TempBasal not found. Item: {}", tempBasalProcessDTO.itemOne)
                    }

                } else {
                    // add

                    val treatment = tempBasalProcessDTO.itemOne

                    val tbr2 = treatment.getDecodedData().get("Object") as TempBasalPair
                    tbr2.setDurationMinutes(tempBasalProcessDTO.getDuration())

                    val tempBasal = findTempBasalWithPumpId(tempBasalProcessDTO.itemOne.getPumpId()!!, entriesFromHistory!!)

                    if (tempBasal == null) {
                        val treatmentDb = findDbEntry(treatment, entriesFromHistory!!)

                        if (logEnabled)
                            LOG.debug("Add " + ProcessHistoryRecord.TBR.getDescription() + " {} - (entryFromDb={}) ", treatment, treatmentDb)

                        addTBR(treatment, treatmentDb as info.nightscout.androidaps.database.entities.TemporaryBasal)
                    } else {
                        // this shouldn't happen
                        if (tempBasal!!.durationInMinutes != tempBasalProcessDTO.getDuration()) {
                            LOG.debug("Found entry with wrong duration (shouldn't happen)... updating")
                            tempBasal!!.durationInMinutes = tempBasalProcessDTO.getDuration()
                        }

                    }
                } // if
            } // for

        } // collection

    }


    fun processSuspends(tempBasalProcessList: List<TempBasalProcessDTO>?) : Unit {
        for (tempBasalProcess in tempBasalProcessList) {
            //TODO: Fix Medtronic driver
            //TemporaryBasal tempBasal = databaseHelper.findTempBasalByPumpId(tempBasalProcess.itemOne.getPumpId());
            var tempBasal: TemporaryBasal? = databaseHelper.findTempBasalByPumpId(tempBasalProcess.itemOne.getPumpId())

            if (tempBasal == null) {
                // add
                tempBasal = TemporaryBasal()
                tempBasal.date = tryToGetByLocalTime(tempBasalProcess.itemOne.atechDateTime)

                tempBasal.source = Source.PUMP
                tempBasal.pumpId = tempBasalProcess.itemOne.getPumpId()!!
                tempBasal.durationInMinutes = tempBasalProcess.getDuration()
                tempBasal.absoluteRate = 0.0
                tempBasal.isAbsolute = true

                tempBasalProcess.itemOne.setLinkedObject(tempBasal)
                tempBasalProcess.itemTwo.setLinkedObject(tempBasal)

                //TODO: Fix Medtronic driver
                databaseHelper.createOrUpdate(tempBasal);

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

        //LocalDateTime oldestEntryTime = null;

        try {

            val oldestEntryTime = medtronicUtil.toGregorianCalendar(dt)
            oldestEntryTime.add(Calendar.MINUTE, -2)

            return oldestEntryTime.getTimeInMillis()

        } catch (ex: Exception) {
            LOG.error("Problem decoding date from last record: {}" + currentTreatment!!)
            return 8 // default return of 8 minutes
        }


    }


    private fun getDatabaseEntriesByLastTimestamp(startTimestamp: Long, processHistoryRecord: ProcessHistoryRecord): List<DBEntryWithTime>? {
        return if (processHistoryRecord == ProcessHistoryRecord.Bolus) {
            return database.bolusDao.getBolusesStartingWithTimeForPump(startTimestamp, InterfaceIDs.PumpType.MEDTRONIC, pumpSerial.toLong())
        } else {
            //TODO: Fix Medtronic driver
            //return databaseHelper.getTemporaryBasalsDataFromTime(startTimestamp, true);
            null
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


    private fun filterOutAlreadyAddedEntries(entryList: List<PumpHistoryEntry>, treatmentsFromHistory: List<out DBEntryWithTime>) {

        if (!doesCollectionHaveData(treatmentsFromHistory))
            return

        val removeTreatmentsFromHistory = ArrayList<DBEntry>()

        for (treatment in treatmentsFromHistory) {

            if (treatment.interfaceIDs.pumpId !=null && treatment.interfaceIDs.pumpId.toLong() > 0) {

                var selectedBolus: PumpHistoryEntry? = null

                for (bolus in entryList) {
                    if (bolus.pumpId === treatment.interfaceIDs.pumpId) {
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


    private fun addBolus(bolus: PumpHistoryEntry, treatment: Bolus?) {

        val bolusDTO = bolus.dataObject as BolusDTO

        if (treatment ==
                null) {

            when (bolusDTO.getBolusType()) {
                PumpBolusType.Normal -> {
                    val detailedBolusInfo = DetailedBolusInfo()

                    detailedBolusInfo.date = tryToGetByLocalTime(bolus.atechDateTime)
                    detailedBolusInfo.source = Source.PUMP
                    detailedBolusInfo.pumpId = bolus.getPumpId()!!
                    detailedBolusInfo.insulin = bolusDTO.getDeliveredAmount()!!

                    addCarbsFromEstimate(detailedBolusInfo, bolus)

                    bolus.setLinkedObject(detailedBolusInfo)

                    BlockingAppRepository.runTransactionForResult(PumpInsertMealBolusTransaction(
                            tryToGetByLocalTime(bolus.atechDateTime),
                            bolusDTO.getDeliveredAmount()!!,
                            detailedBolusInfo.carbs,
                            info.nightscout.androidaps.database.entities.Bolus.Type.NORMAL,
                            InterfaceIDs.PumpType.MEDTRONIC,
                            getPumpSerial(),
                            bolus.getPumpId()!!, null
                    ))

                    if (logEnabled)
                        LOG.debug("addBolus - [date={},pumpId={}, insulin={}, newRecord={}]", detailedBolusInfo.date,
                                detailedBolusInfo.pumpId, detailedBolusInfo.insulin, detailedBolusInfo)
                }

                PumpBolusType.Audio, PumpBolusType.Extended -> {
                    val extendedBolus = ExtendedBolus()
                    extendedBolus.date = tryToGetByLocalTime(bolus.atechDateTime)
                    extendedBolus.source = Source.PUMP
                    extendedBolus.insulin = bolusDTO.getDeliveredAmount()!!
                    extendedBolus.pumpId = bolus.getPumpId()!!
                    extendedBolus.isValid = true
                    extendedBolus.durationInMinutes = bolusDTO.getDuration()!!

                    bolus.setLinkedObject(extendedBolus)

                    BlockingAppRepository.runTransactionForResult(PumpExtendedBolusTransaction(
                            tryToGetByLocalTime(bolus.atechDateTime),
                            bolusDTO.getDeliveredAmount()!!,
                            0L,
                            bolusDTO.getDuration()!!,
                            false,
                            InterfaceIDs.PumpType.MEDTRONIC,
                            getPumpSerial(),
                            bolus.getPumpId()!!
                    ))

                    if (logEnabled)
                        LOG.debug("addBolus - Extended [date={},pumpId={}, insulin={}, duration={}]", extendedBolus.date,
                                extendedBolus.pumpId, extendedBolus.insulin, extendedBolus.durationInMinutes)

                }
            }

        } else {

            var detailedBolusInfo = DetailedBolusInfoStorage.findDetailedBolusInfo(treatment!!.date)
            if (detailedBolusInfo == null) {
                detailedBolusInfo = DetailedBolusInfo()
            }

            detailedBolusInfo!!.date = treatment!!.date
            detailedBolusInfo!!.source = Source.PUMP
            detailedBolusInfo!!.pumpId = bolus.getPumpId()!!
            detailedBolusInfo!!.insulin = bolusDTO.getDeliveredAmount()!!
            detailedBolusInfo!!.carbs = treatment!!.carbs

            addCarbsFromEstimate(detailedBolusInfo, bolus)

            BlockingAppRepository.runTransactionForResult(PumpInsertUpdateBolusTransaction(
                    tryToGetByLocalTime(bolus.atechDateTime),
                    bolusDTO.getDeliveredAmount()!!,
                    detailedBolusInfo!!.carbs,
                    info.nightscout.androidaps.database.entities.Bolus.Type.NORMAL,
                    InterfaceIDs.PumpType.MEDTRONIC,
                    getPumpSerial(),
                    bolus.getPumpId()!!, null
            ))

            bolus.setLinkedObject(detailedBolusInfo)

            if (logEnabled)
                LOG.debug("editBolus - [date={},pumpId={}, insulin={}, newRecord={}]", detailedBolusInfo!!.date,
                        detailedBolusInfo!!.pumpId, detailedBolusInfo!!.insulin, detailedBolusInfo)

        }
    }


    private fun addTBR(treatment: PumpHistoryEntry, temporaryBasalDbInput: TemporaryBasal?) {

        val tbr = treatment.dataObject as MDTTemporaryBasal

        var operation = "editTBR"

        var date: Long = 0

        if (temporaryBasalDbInput == null) {
            date = tryToGetByLocalTime(treatment.atechDateTime)
            operation = "addTBR"
        } else {
            date = temporaryBasalDbInput.timestamp
        }

        val temporaryBasalX = TemporaryBasal()
        temporaryBasalX.source = Source.PUMP
        temporaryBasalX.pumpId = treatment.getPumpId()!!
        temporaryBasalX.durationInMinutes = tbr.getDurationMinutes()
        temporaryBasalX.absoluteRate = tbr.getInsulinRate()
        temporaryBasalX.isAbsolute = !tbr.isPercent()
        temporaryBasalX.date = date

        treatment.setLinkedObject(temporaryBasalX)


        BlockingAppRepository.runTransactionForResult(PumpInsertUpdateTemporaryBasalTransaction(
                date,
                0L,
                tbr.getDurationMinutes().toLong(),
                !tbr.isPercent(),
                tbr.getInsulinRate(),
                InterfaceIDs.PumpType.MEDTRONIC,
                getPumpSerial(),
                treatment.getPumpId()!!
        ))


        var currentTemporaryBasal : TemporaryBasal? = null

        var duration = tbr.duration!! * 60000L

        // TODO

        currentTemporaryBasal = database.temporaryBasalDao.getTemporaryBasalByPumpId(pumpType, pumpSerial, treatment.pumpId)

        if (currentTemporaryBasal==null) {
            database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    timestamp = timestamp,
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    type = TemporaryBasal.Type.NORMAL,
                    absolute = absolute,
                    rate = rate,
                    duration = duration
            ).apply {
                interfaceIDs.pumpType = pumpType
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
                changes.add(this)
            })
        } else {
            database.temporaryBasalDao.updateExistingEntry(TemporaryBasal(
                    timestamp = currentTemporaryBasal.timestamp,
                    utcOffset = currentTemporaryBasal.utcOffset,
                    type = currentTemporaryBasal.type,
                    absolute = absolute,
                    rate = rate,
                    duration = duration
            ).apply {
                interfaceIDs.pumpType = pumpType
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
                changes.add(this)
            })
        }



        if (logEnabled)
            LOG.debug("$operation - [date={},pumpId={}, rate={} {}, duration={}]", //
                    temporaryBasalX.date, //
                    temporaryBasalX.pumpId, //
                    if (temporaryBasalX.isAbsolute)
                        String.format(Locale.ENGLISH, "%.2f", temporaryBasalX.absoluteRate)
                    else
                        String.format(Locale.ENGLISH, "%d", temporaryBasalX.percentRate), //
                    if (temporaryBasalX.isAbsolute) "U/h" else "%", //
                    temporaryBasalX.durationInMinutes)
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

        if (entriesFromHistory.size == 0) {
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
                        LOG.error("Too many entries (with too small diff): (timeDiff=[min={},sec={}],count={},list={})",
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
            val atechDateTime: Long,
            val type: EntryType,
            val pumpId: Long,
            val dataObject: Any
    ) {
        enum class EntryType {
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
            val start: Boolean?,
            val eventId: Long?,
            val timestamp: Long?,
            val duration: Long?,
            val percentage: Int?
    ) : DbObjectMDT(atechDateTime)

    data class MDTBolus(
            override val atechDateTime: Long,
            val start: Boolean,
            val eventId: Long,
            val type: Type,
            val timestamp: Long,
            val bolusId: Int,
            val immediateAmount: Double,
            val duration: Long,
            val extendedAmount: Double,
            val pumpId: Long
    ) : DbObjectMDT(atechDateTime) {
        enum class Type {
            STANDARD,
            MULTIWAVE,
            EXTENDED
        }
    }

    data class MDTDailyTotals(
            override val atechDateTime: Long,
            val bolusInsulin: Double,
            val basalInsulin: Double,
            val totalInsulin: Double,
            val pumpId: Long
    ) : DbObjectMDT(atechDateTime)


    data class PumpClock(
            val timeD: Long
    )


    data class TempBasalProcessDTO(
            var itemOne: MDTTemporaryBasal? = null,
            var itemTwo: MDTTemporaryBasal? = null,
            var processOperation : Operation = Operation.Undefined
    ) {
        enum class Operation {
            Undefined,
            Add,
            Edit
        }
    }


    private enum class ProcessHistoryRecord private constructor(val description: String) {
        Bolus("Bolus"),
        TBR("TBR"),
        Suspend("Suspend")

    }


}