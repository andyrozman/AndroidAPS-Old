package info.nightscout.androidaps.plugins.pump.medtronic.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.database.BlockingAppRepository;
import info.nightscout.androidaps.database.embedments.InterfaceIDs;
import info.nightscout.androidaps.database.entities.Bolus;
import info.nightscout.androidaps.database.interfaces.DBEntry;
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime;
import info.nightscout.androidaps.database.transactions.medtronic.MedtronicHistoryProcessTransaction;
import info.nightscout.androidaps.database.transactions.pump.PumpExtendedBolusTransaction;
import info.nightscout.androidaps.database.transactions.pump.PumpInsertMealBolusTransaction;
import info.nightscout.androidaps.database.transactions.pump.PumpInsertUpdateBolusTransaction;
import info.nightscout.androidaps.database.transactions.pump.PumpInsertUpdateTemporaryBasalTransaction;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.db.ExtendedBolus;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TDD;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.configBuilder.DetailedBolusInfoStorage;
import info.nightscout.androidaps.plugins.pump.common.utils.DateTimeUtil;
import info.nightscout.androidaps.plugins.pump.common.utils.StringUtil;
import info.nightscout.androidaps.plugins.pump.insight.descriptors.BolusType;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.history.pump.MedtronicPumpHistoryDecoder;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.history.pump.PumpHistoryEntry;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.history.pump.PumpHistoryEntryType;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.history.pump.PumpHistoryResult;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.BasalProfile;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.BolusDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.BolusWizardDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.ClockDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.DailyTotalsDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.TempBasalProcessDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.defs.PumpBolusType;
import info.nightscout.androidaps.plugins.pump.medtronic.driver.MedtronicPumpStatus;
import info.nightscout.androidaps.plugins.pump.medtronic.util.MedtronicConst;
import info.nightscout.androidaps.plugins.pump.medtronic.util.MedtronicUtil;
import info.nightscout.androidaps.plugins.treatments.Treatment;
import info.nightscout.androidaps.utils.SP;


/**
 * Created by andy on 10/12/18.
 */

// TODO: After release we need to refactor how data is retrieved from pump, each entry in history needs to be marked, and sorting
//  needs to happen according those markings, not on time stamp (since AAPS can change time anytime it drifts away). This
//  needs to include not returning any records if TZ goes into -x area. To fully support this AAPS would need to take note of
//  all times that time changed (TZ, DST, etc.). Data needs to be returned in batches (time_changed batches, so that we can
//  handle it. It would help to assign sort_ids to items (from oldest (1) to newest (x)


public class MedtronicHistoryData {

    private static final Logger LOG = LoggerFactory.getLogger(L.PUMP);

    private List<PumpHistoryEntry> allHistory = null;
    private List<PumpHistoryEntry> newHistory = null;

    private Long lastHistoryRecordTime;
    private boolean isInit = false;

    private Gson gson;

    private long lastIdUsed = 0;
    private MedtronicPumpStatus pumpStatus;


    public MedtronicHistoryData() {
        this.allHistory = new ArrayList<>();
        this.gson = MedtronicUtil.gsonInstance;

        if (this.gson == null) {
            this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        }
    }


    /**
     * Add New History entries
     *
     * @param result PumpHistoryResult instance
     */
    public void addNewHistory(PumpHistoryResult result) {

        List<PumpHistoryEntry> validEntries = result.getValidEntries();

        List<PumpHistoryEntry> newEntries = new ArrayList<>();

        for (PumpHistoryEntry validEntry : validEntries) {

            if (!this.allHistory.contains(validEntry)) {
                newEntries.add(validEntry);
            }
        }

        this.newHistory = newEntries;

        showLogs("List of history (before filtering): [" + this.newHistory.size() + "]", gson.toJson(this.newHistory));
    }


    private static void showLogs(String header, String data) {

        if (!isLogEnabled())
            return;

        if (header != null) {
            LOG.debug(header);
        }

        if (StringUtils.isNotBlank(data)) {
            for (final String token : StringUtil.splitString(data, 3500)) {
                LOG.debug("{}", token);
            }
        } else {
            LOG.debug("No data.");
        }
    }


    public List<PumpHistoryEntry> getAllHistory() {
        return this.allHistory;
    }


    private String getPumpSerial() {
        return pumpStatus.serialNumber;
    }


    public void filterNewEntries() {

        List<PumpHistoryEntry> newHistory2 = new ArrayList<>();
        List<PumpHistoryEntry> TBRs = new ArrayList<>();
        List<PumpHistoryEntry> bolusEstimates = new ArrayList<>();
        long atechDate = DateTimeUtil.toATechDate(new GregorianCalendar());

        //LOG.debug("Filter new entries: Before {}", newHistory);

        if (!isCollectionEmpty(newHistory)) {

            for (PumpHistoryEntry pumpHistoryEntry : newHistory) {

                if (!this.allHistory.contains(pumpHistoryEntry)) {

                    PumpHistoryEntryType type = pumpHistoryEntry.getEntryType();

                    if (type == PumpHistoryEntryType.TempBasalRate || type == PumpHistoryEntryType.TempBasalDuration) {
                        TBRs.add(pumpHistoryEntry);
                    } else if (type == PumpHistoryEntryType.BolusWizard || type == PumpHistoryEntryType.BolusWizard512) {
                        bolusEstimates.add(pumpHistoryEntry);
                        newHistory2.add(pumpHistoryEntry);
                    } else {

                        if (type == PumpHistoryEntryType.EndResultTotals) {
                            if (!DateTimeUtil.isSameDay(atechDate, pumpHistoryEntry.atechDateTime)) {
                                newHistory2.add(pumpHistoryEntry);
                            }
                        } else {
                            newHistory2.add(pumpHistoryEntry);
                        }
                    }
                }
            }

            TBRs = preProcessTBRs(TBRs);

            if (bolusEstimates.size() > 0) {
                extendBolusRecords(bolusEstimates, newHistory2);
            }

            newHistory2.addAll(TBRs);

            this.newHistory = newHistory2;

            sort(this.newHistory);
        }

        if (isLogEnabled())
            LOG.debug("New History entries found: {}", this.newHistory.size());

        showLogs("List of history (after filtering): [" + this.newHistory.size() + "]", gson.toJson(this.newHistory));

    }

    private void extendBolusRecords(List<PumpHistoryEntry> bolusEstimates, List<PumpHistoryEntry> newHistory2) {

        List<PumpHistoryEntry> boluses = getFilteredItems(newHistory2, PumpHistoryEntryType.Bolus);

        for (PumpHistoryEntry bolusEstimate : bolusEstimates) {
            for (PumpHistoryEntry bolus : boluses) {
                if (bolusEstimate.atechDateTime.equals(bolus.atechDateTime)) {
                    bolus.addDecodedData("Estimate", bolusEstimate.getDecodedData().get("Object"));
                }
            }
        }
    }


    public void finalizeNewHistoryRecords() {

        if ((newHistory == null) || (newHistory.size() == 0))
            return;

        PumpHistoryEntry pheLast = newHistory.get(0);

        // find last entry
        for (PumpHistoryEntry pumpHistoryEntry : newHistory) {
            if (pumpHistoryEntry.atechDateTime != null && pumpHistoryEntry.isAfter(pheLast.atechDateTime)) {
                pheLast = pumpHistoryEntry;
            }
        }

        // add new entries
        Collections.reverse(newHistory);

        for (PumpHistoryEntry pumpHistoryEntry : newHistory) {

            if (!this.allHistory.contains(pumpHistoryEntry)) {
                lastIdUsed++;
                pumpHistoryEntry.id = lastIdUsed;
                this.allHistory.add(pumpHistoryEntry);
            }

        }


        if (pheLast == null) // if we don't have any valid record we don't do the filtering and setting
            return;

        this.setLastHistoryRecordTime(pheLast.atechDateTime);
        SP.putLong(MedtronicConst.Statistics.LastPumpHistoryEntry, pheLast.atechDateTime);

        LocalDateTime dt = null;

        try {
            dt = DateTimeUtil.toLocalDateTime(pheLast.atechDateTime);
        } catch (Exception ex) {
            LOG.error("Problem decoding date from last record: {}" + pheLast);
        }

        if (dt != null) {

            dt = dt.minusDays(1); // we keep 24 hours

            long dtRemove = DateTimeUtil.toATechDate(dt);

            List<PumpHistoryEntry> removeList = new ArrayList<>();

            for (PumpHistoryEntry pumpHistoryEntry : allHistory) {

                if (!pumpHistoryEntry.isAfter(dtRemove)) {
                    removeList.add(pumpHistoryEntry);
                }
            }

            this.allHistory.removeAll(removeList);

            this.sort(this.allHistory);

            if (isLogEnabled())
                LOG.debug("All History records [afterFilterCount={}, removedItemsCount={}, newItemsCount={}]",
                        allHistory.size(), removeList.size(), newHistory.size());
        } else {
            LOG.error("Since we couldn't determine date, we don't clean full history. This is just workaround.");
        }

        this.newHistory.clear();
    }


    public boolean hasRelevantConfigurationChanged() {
        return getStateFromFilteredList( //
                PumpHistoryEntryType.ChangeBasalPattern, //
                PumpHistoryEntryType.ClearSettings, //
                PumpHistoryEntryType.SaveSettings, //
                PumpHistoryEntryType.ChangeMaxBolus, //
                PumpHistoryEntryType.ChangeMaxBasal, //
                PumpHistoryEntryType.ChangeTempBasalType);
    }


    private boolean isCollectionEmpty(List col) {
        return (col == null || col.isEmpty());
    }

    private boolean isCollectionNotEmpty(List col) {
        return (col != null && !col.isEmpty());
    }


    public boolean isPumpSuspended() {

        List<PumpHistoryEntry> items = getDataForPumpSuspends();

        showLogs("isPumpSuspended: ", MedtronicUtil.gsonInstance.toJson(items));

        if (isCollectionNotEmpty(items)) {

            PumpHistoryEntryType pumpHistoryEntryType = items.get(0).getEntryType();

            boolean isSuspended = !(pumpHistoryEntryType == PumpHistoryEntryType.TempBasalCombined || //
                    pumpHistoryEntryType == PumpHistoryEntryType.BasalProfileStart || //
                    pumpHistoryEntryType == PumpHistoryEntryType.Bolus || //
                    pumpHistoryEntryType == PumpHistoryEntryType.Resume || //
                    pumpHistoryEntryType == PumpHistoryEntryType.Prime);

            if (isLogEnabled())
                LOG.debug("isPumpSuspended. Last entry type={}, isSuspended={}", pumpHistoryEntryType, isSuspended);

            return isSuspended;
        } else
            return false;

    }


    private List<PumpHistoryEntry> getDataForPumpSuspends() {

        List<PumpHistoryEntry> newAndAll = new ArrayList<>();

        if (isCollectionNotEmpty(this.allHistory)) {
            newAndAll.addAll(this.allHistory);
        }

        if (isCollectionNotEmpty(this.newHistory)) {

            for (PumpHistoryEntry pumpHistoryEntry : newHistory) {
                if (!newAndAll.contains(pumpHistoryEntry)) {
                    newAndAll.add(pumpHistoryEntry);
                }
            }
        }

        if (newAndAll.isEmpty())
            return newAndAll;

        this.sort(newAndAll);

        List<PumpHistoryEntry> newAndAll2 = getFilteredItems(newAndAll, //
                PumpHistoryEntryType.Bolus, //
                PumpHistoryEntryType.TempBasalCombined, //
                PumpHistoryEntryType.Prime, //
                PumpHistoryEntryType.Suspend, //
                PumpHistoryEntryType.Resume, //
                PumpHistoryEntryType.Rewind, //
                PumpHistoryEntryType.NoDeliveryAlarm, //
                PumpHistoryEntryType.BasalProfileStart);

        newAndAll2 = filterPumpSuspend(newAndAll2, 10);

        return newAndAll2;
    }


    private List<PumpHistoryEntry> filterPumpSuspend(List<PumpHistoryEntry> newAndAll, int filterCount) {

        if (newAndAll.size() <= filterCount) {
            return newAndAll;
        }

        List<PumpHistoryEntry> newAndAllOut = new ArrayList<>();

        for (int i = 0; i < filterCount; i++) {
            newAndAllOut.add(newAndAll.get(i));
        }

        return newAndAllOut;
    }


    public void processNewHistoryData() {

        // TDD
        List<PumpHistoryEntry> tdds = getFilteredItems(PumpHistoryEntryType.EndResultTotals, getTDDType());

        if (isLogEnabled())
            LOG.debug("ProcessHistoryData: TDD [count={}, items={}]", tdds.size(), gson.toJson(tdds));

        if (isCollectionNotEmpty(tdds)) {
            tdds = prepareTDDs(tdds);
        }

        // Bolus
        List<PumpHistoryEntry> treatments = getFilteredItems(PumpHistoryEntryType.Bolus);

        if (isLogEnabled())
            LOG.debug("ProcessHistoryData: Bolus [count={}, items={}]", treatments.size(), gson.toJson(treatments));

        // TBR
        List<PumpHistoryEntry> tbrs = getFilteredItems(PumpHistoryEntryType.TempBasalCombined);
        boolean oldEntryAdded = false;

        if (isCollectionNotEmpty(treatments)) {
            oldEntryAdded = prepareTBRs(treatments); // add carbs if wizard entries exists
        }

        if (isLogEnabled())
            LOG.debug("ProcessHistoryData: TBRs Processed [count={}, items={}]", tbrs.size(), gson.toJson(tbrs));

        // 'Delivery Suspend'
        List<TempBasalProcessDTO> suspends = null;

        try {
            suspends = getSuspends();
        } catch (Exception ex) {
            LOG.error("ProcessHistoryData: Error getting Suspend entries: " + ex.getMessage(), ex);
            throw ex;
        }

        if (isLogEnabled())
            LOG.debug("ProcessHistoryData: 'Delivery Suspend' Processed [count={}, items={}]", suspends.size(),
                    gson.toJson(suspends));

        LOG.info("ProcessHistoryData: Data prepared for Db Transaction [TDD={}, Bolus={}, TemporaryBasal={}, SuspendResume={}]",
                tdds.size(),
                treatments.size(),
                tbrs.size(),
                suspends == null ? "Error" : suspends.size()
        );

        BlockingAppRepository.INSTANCE.runTransactionForResult(new MedtronicHistoryProcessTransaction(
                getPumpSerial(),
                convertPumpHistoryEntriesToKotlin(tdds),
                convertPumpHistoryEntriesToKotlin(treatments),
                convertPumpHistoryEntriesToKotlin(tbrs),
                oldEntryAdded,
                convertSuspendResumeEntriesToKotlin(suspends),
                isLogEnabled()
        ));



    }

    private List<MedtronicHistoryProcessTransaction.MDTTempBasalProcess> convertSuspendResumeEntriesToKotlin(List<TempBasalProcessDTO> listInput) {

        List<MedtronicHistoryProcessTransaction.MDTTempBasalProcess> outList = new ArrayList<>();

        if (isCollectionNotEmpty(listInput)) {
            for (TempBasalProcessDTO tempBasalProcessDTO : listInput) {

                MedtronicHistoryProcessTransaction.MDTTempBasalProcess.Operation operation = MedtronicHistoryProcessTransaction.MDTTempBasalProcess.Operation.Undefined;

                switch(tempBasalProcessDTO.processOperation) {

                    case Add:
                        operation = MedtronicHistoryProcessTransaction.MDTTempBasalProcess.Operation.Add;
                        break;

                    case Edit:
                        operation = MedtronicHistoryProcessTransaction.MDTTempBasalProcess.Operation.Edit;
                        break;

                    default:
                        break;
                }

                outList.add(
                        new MedtronicHistoryProcessTransaction.MDTTempBasalProcess(
                                createKotlinTempBasalEntry(tempBasalProcessDTO.itemOne),
                                tempBasalProcessDTO.itemTwo == null ? null : createKotlinTempBasalEntry(tempBasalProcessDTO.itemTwo),
                                operation
                        )
                );
            }
        }

        return outList;
    }


    private List<PumpHistoryEntry> prepareTDDs(List<PumpHistoryEntry> tddsIn) {

        List<PumpHistoryEntry> tdds = filterTDDs(tddsIn);
        return tdds;
    }


    private boolean prepareTBRs(List<PumpHistoryEntry> entryList) {

        Collections.reverse(entryList);

        TempBasalPair tbr = (TempBasalPair) entryList.get(0).getDecodedDataEntry("Object");

        boolean readOldItem = false;

        if (tbr.isCancelTBR()) {
            PumpHistoryEntry oneMoreEntryFromHistory = getOneMoreEntryFromHistory(PumpHistoryEntryType.TempBasalCombined);

            if (oneMoreEntryFromHistory != null) {
                entryList.add(0, oneMoreEntryFromHistory);
                readOldItem = true;
            } else {
                entryList.remove(0);
            }
        }

        return readOldItem;

    }


    private List<MedtronicHistoryProcessTransaction.PumpHistoryEntry> convertPumpHistoryEntriesToKotlin(List<PumpHistoryEntry> inList) {
        List<MedtronicHistoryProcessTransaction.PumpHistoryEntry> outList = new ArrayList<>();

        for (PumpHistoryEntry pumpHistoryEntry : inList) {

            MedtronicHistoryProcessTransaction.PumpHistoryEntry.EntryType entryType = MedtronicHistoryProcessTransaction.PumpHistoryEntry.EntryType.None;
            Long pumpId = pumpHistoryEntry.getPumpId();
            Object dataObject = null;

            switch (pumpHistoryEntry.getEntryType()) {

                case Bolus: {
                    entryType = MedtronicHistoryProcessTransaction.PumpHistoryEntry.EntryType.Bolus;

                    BolusDTO dto = (BolusDTO)pumpHistoryEntry.getDecodedDataEntry("Object");
                    Integer carbs = 0;

                    if (pumpHistoryEntry.containsDecodedData("Estimate")) {
                        BolusWizardDTO bolusWizard = (BolusWizardDTO) pumpHistoryEntry.getDecodedDataEntry("Estimate");
                        carbs = bolusWizard.carbs;
                    }

                    dataObject =
                            new MedtronicHistoryProcessTransaction.MDTBolus(
                                    pumpHistoryEntry.atechDateTime,
                                    PumpBolusType.isNormalBolus(dto.getBolusType()) ?
                                            MedtronicHistoryProcessTransaction.MDTBolus.Type.NORMAL :
                                            MedtronicHistoryProcessTransaction.MDTBolus.Type.EXTENDED,
                                    dto.getDeliveredAmount(),
                                    carbs,
                                    dto.getDuration(),
                                    pumpId
                            );

                } break;


                case TempBasalCombined: {
                    entryType = MedtronicHistoryProcessTransaction.PumpHistoryEntry.EntryType.TemporaryBasal;

                    dataObject = createKotlinTempBasalEntry(pumpHistoryEntry);

                } break;


                case EndResultTotals:
                case DailyTotals515:
                case DailyTotals522:
                case DailyTotals523: {
                    entryType = MedtronicHistoryProcessTransaction.PumpHistoryEntry.EntryType.TDD;

                    DailyTotalsDTO dto = (DailyTotalsDTO)pumpHistoryEntry.getDecodedDataEntry("Object");

                    dataObject =
                            new MedtronicHistoryProcessTransaction.MDTDailyTotals(
                                    pumpHistoryEntry.atechDateTime,
                                    dto.getInsulinBolus(),
                                    dto.getInsulinBasal(),
                                    dto.getInsulinTotal(),
                                    pumpId
                            );

                } break;

                default:
                    LOG.warn("Unsupported entryType: " + pumpHistoryEntry.getEntryType());

            }

            outList.add(new MedtronicHistoryProcessTransaction.PumpHistoryEntry(
                    pumpHistoryEntry.atechDateTime,
                    entryType,
                    pumpId,
                    dataObject
            ));

        }

        return outList;
    }


    private MedtronicHistoryProcessTransaction.MDTTemporaryBasal createKotlinTempBasalEntry(PumpHistoryEntry pumpHistoryEntry) {
        TempBasalPair dto = (TempBasalPair)pumpHistoryEntry.getDecodedDataEntry("Object");

        return new MedtronicHistoryProcessTransaction.MDTTemporaryBasal(
                        pumpHistoryEntry.atechDateTime,
                        dto.getInsulinRate(),
                        dto.getDurationMinutes(),
                        pumpHistoryEntry.getPumpId()
                );
    }




    public void setPumpStatusObject(MedtronicPumpStatus pumpStatusLocal) {
        pumpStatus = pumpStatusLocal;
    }


    private List<TempBasalProcessDTO> getSuspends() {

        List<TempBasalProcessDTO> outList = new ArrayList<>();

        // suspend/resume
        outList.addAll(getSuspendResumeRecords());
        // no_delivery/prime & rewind/prime
        outList.addAll(getNoDeliveryRewindPrimeRecords());

        return outList;
    }


    private List<TempBasalProcessDTO> getSuspendResumeRecords() {
        List<PumpHistoryEntry> filteredItems = getFilteredItems(this.newHistory, //
                PumpHistoryEntryType.Suspend, //
                PumpHistoryEntryType.Resume);

        List<TempBasalProcessDTO> outList = new ArrayList<>();

        if (filteredItems.size() > 0) {

            List<PumpHistoryEntry> filtered2Items = new ArrayList<>();

            if ((filteredItems.size() % 2 == 0) && (filteredItems.get(0).getEntryType() == PumpHistoryEntryType.Resume)) {
                // full resume suspends (S R S R)
                filtered2Items.addAll(filteredItems);
            } else if ((filteredItems.size() % 2 == 0) && (filteredItems.get(0).getEntryType() == PumpHistoryEntryType.Suspend)) {
                // not full suspends, need to retrive one more record and discard first one (R S R S) -> ([S] R S R [xS])
                filteredItems.remove(0);

                PumpHistoryEntry oneMoreEntryFromHistory = getOneMoreEntryFromHistory(PumpHistoryEntryType.Suspend);
                if (oneMoreEntryFromHistory != null) {
                    filteredItems.add(oneMoreEntryFromHistory);
                } else {
                    filteredItems.remove(filteredItems.size() - 1); // remove last (unpaired R)
                }

                filtered2Items.addAll(filteredItems);
            } else {
                if (filteredItems.get(0).getEntryType() == PumpHistoryEntryType.Resume) {
                    // get one more from history (R S R) -> ([S] R S R)

                    PumpHistoryEntry oneMoreEntryFromHistory = getOneMoreEntryFromHistory(PumpHistoryEntryType.Suspend);
                    if (oneMoreEntryFromHistory != null) {
                        filteredItems.add(oneMoreEntryFromHistory);
                    } else {
                        filteredItems.remove(filteredItems.size() - 1); // remove last (unpaired R)
                    }

                    filtered2Items.addAll(filteredItems);
                } else {
                    // remove last and have paired items
                    filteredItems.remove(0);
                    filtered2Items.addAll(filteredItems);
                }
            }

            if (filtered2Items.size() > 0) {
                sort(filtered2Items);
                Collections.reverse(filtered2Items);

                for (int i = 0; i < filtered2Items.size(); i += 2) {
                    TempBasalProcessDTO dto = new TempBasalProcessDTO();

                    dto.itemOne = filtered2Items.get(i);
                    dto.itemTwo = filtered2Items.get(i + 1);

                    dto.processOperation = TempBasalProcessDTO.Operation.Add;

                    outList.add(dto);
                }
            }
        }

        return outList;
    }


    private List<TempBasalProcessDTO> getNoDeliveryRewindPrimeRecords() {
        List<PumpHistoryEntry> primeItems = getFilteredItems(this.newHistory, //
                PumpHistoryEntryType.Prime);

        List<TempBasalProcessDTO> outList = new ArrayList<>();

        if (primeItems.size() == 0)
            return outList;

        List<PumpHistoryEntry> filteredItems = getFilteredItems(this.newHistory, //
                PumpHistoryEntryType.Prime,
                PumpHistoryEntryType.Rewind,
                PumpHistoryEntryType.NoDeliveryAlarm,
                PumpHistoryEntryType.Bolus,
                PumpHistoryEntryType.TempBasalCombined
        );

        List<PumpHistoryEntry> tempData = new ArrayList<>();
        boolean startedItems = false;
        boolean finishedItems = false;

        for (PumpHistoryEntry filteredItem : filteredItems) {
            if (filteredItem.getEntryType() == PumpHistoryEntryType.Prime) {
                startedItems = true;
            }

            if (startedItems) {
                if (filteredItem.getEntryType() == PumpHistoryEntryType.Bolus ||
                        filteredItem.getEntryType() == PumpHistoryEntryType.TempBasalCombined) {
                    finishedItems = true;
                    break;
                }

                tempData.add(filteredItem);
            }
        }


        if (!finishedItems) {

            List<PumpHistoryEntry> filteredItemsOld = getFilteredItems(this.allHistory, //
                    PumpHistoryEntryType.Rewind,
                    PumpHistoryEntryType.NoDeliveryAlarm,
                    PumpHistoryEntryType.Bolus,
                    PumpHistoryEntryType.TempBasalCombined
            );

            for (PumpHistoryEntry filteredItem : filteredItemsOld) {

                if (filteredItem.getEntryType() == PumpHistoryEntryType.Bolus ||
                        filteredItem.getEntryType() == PumpHistoryEntryType.TempBasalCombined) {
                    finishedItems = true;
                    break;
                }

                tempData.add(filteredItem);
            }
        }


        if (!finishedItems) {
            showLogs("NoDeliveryRewindPrimeRecords: Not finished Items: ", gson.toJson(tempData));
            return outList;
        }

        showLogs("NoDeliveryRewindPrimeRecords: Records to evaluate: ", gson.toJson(tempData));

        List<PumpHistoryEntry> items = getFilteredItems(tempData, //
                PumpHistoryEntryType.Prime
        );


        TempBasalProcessDTO processDTO = new TempBasalProcessDTO();

        processDTO.itemTwo = items.get(0);

        items = getFilteredItems(tempData, //
                PumpHistoryEntryType.NoDeliveryAlarm
        );

        if (items.size() > 0) {

            processDTO.itemOne = items.get(items.size() - 1);
            processDTO.processOperation = TempBasalProcessDTO.Operation.Add;

            outList.add(processDTO);
            return outList;
        }


        items = getFilteredItems(tempData, //
                PumpHistoryEntryType.Rewind
        );

        if (items.size() > 0) {

            processDTO.itemOne = items.get(0);
            processDTO.processOperation = TempBasalProcessDTO.Operation.Add;

            outList.add(processDTO);
            return outList;
        }

        return outList;
    }


    private PumpHistoryEntry getOneMoreEntryFromHistory(PumpHistoryEntryType entryType) {
        List<PumpHistoryEntry> filteredItems = getFilteredItems(this.allHistory, entryType);

        return filteredItems.size() == 0 ? null : filteredItems.get(0);
    }


    private List<PumpHistoryEntry> filterTDDs(List<PumpHistoryEntry> tdds) {
        List<PumpHistoryEntry> tddsOut = new ArrayList<>();

        for (PumpHistoryEntry tdd : tdds) {
            if (tdd.getEntryType() != PumpHistoryEntryType.EndResultTotals) {
                tddsOut.add(tdd);
            }
        }

        return tddsOut.size() == 0 ? tdds : tddsOut;
    }


    private PumpHistoryEntryType getTDDType() {

        if (MedtronicUtil.getMedtronicPumpModel() == null) {
            return PumpHistoryEntryType.EndResultTotals;
        }

        switch (MedtronicUtil.getMedtronicPumpModel()) {

            case Medtronic_515:
            case Medtronic_715:
                return PumpHistoryEntryType.DailyTotals515;

            case Medtronic_522:
            case Medtronic_722:
                return PumpHistoryEntryType.DailyTotals522;

            case Medtronic_523_Revel:
            case Medtronic_723_Revel:
            case Medtronic_554_Veo:
            case Medtronic_754_Veo:
                return PumpHistoryEntryType.DailyTotals523;

            default: {
                return PumpHistoryEntryType.EndResultTotals;
            }
        }
    }


    public boolean hasBasalProfileChanged() {

        List<PumpHistoryEntry> filteredItems = getFilteredItems(PumpHistoryEntryType.ChangeBasalProfile_NewProfile);

        if (isLogEnabled())
            LOG.debug("hasBasalProfileChanged. Items: " + gson.toJson(filteredItems));

        return (filteredItems.size() > 0);
    }


    public void processLastBasalProfileChange(MedtronicPumpStatus mdtPumpStatus) {

        List<PumpHistoryEntry> filteredItems = getFilteredItems(PumpHistoryEntryType.ChangeBasalProfile_NewProfile);

        if (isLogEnabled())
            LOG.debug("processLastBasalProfileChange. Items: " + filteredItems);

        PumpHistoryEntry newProfile = null;
        Long lastDate = null;

        if (filteredItems.size() == 1) {
            newProfile = filteredItems.get(0);
        } else if (filteredItems.size() > 1) {

            for (PumpHistoryEntry filteredItem : filteredItems) {

                if (lastDate == null || lastDate < filteredItem.atechDateTime) {
                    newProfile = filteredItem;
                    lastDate = newProfile.atechDateTime;
                }
            }
        }

        if (newProfile != null) {
            if (isLogEnabled())
                LOG.debug("processLastBasalProfileChange. item found, setting new basalProfileLocally: " + newProfile);
            BasalProfile basalProfile = (BasalProfile) newProfile.getDecodedData().get("Object");

            mdtPumpStatus.basalsByHour = basalProfile.getProfilesByHour();
        }
    }


    public boolean hasPumpTimeChanged() {
        return getStateFromFilteredList(PumpHistoryEntryType.NewTimeSet, //
                PumpHistoryEntryType.ChangeTime);
    }


    public void setLastHistoryRecordTime(Long lastHistoryRecordTime) {

        // this.previousLastHistoryRecordTime = this.lastHistoryRecordTime;
        this.lastHistoryRecordTime = lastHistoryRecordTime;
    }


    public void setIsInInit(boolean init) {
        this.isInit = init;
    }


    // HELPER METHODS

    private void sort(List<PumpHistoryEntry> list) {
        Collections.sort(list, new PumpHistoryEntry.Comparator());
    }


    private List<PumpHistoryEntry> preProcessTBRs(List<PumpHistoryEntry> TBRs_Input) {
        List<PumpHistoryEntry> TBRs = new ArrayList<>();

        Map<String, PumpHistoryEntry> map = new HashMap<>();

        for (PumpHistoryEntry pumpHistoryEntry : TBRs_Input) {
            if (map.containsKey(pumpHistoryEntry.DT)) {
                MedtronicPumpHistoryDecoder.decodeTempBasal(map.get(pumpHistoryEntry.DT), pumpHistoryEntry);
                pumpHistoryEntry.setEntryType(PumpHistoryEntryType.TempBasalCombined);
                TBRs.add(pumpHistoryEntry);
                map.remove(pumpHistoryEntry.DT);
            } else {
                map.put(pumpHistoryEntry.DT, pumpHistoryEntry);
            }
        }

        return TBRs;
    }


    private List<PumpHistoryEntry> getFilteredItems(PumpHistoryEntryType... entryTypes) {
        return getFilteredItems(this.newHistory, entryTypes);
    }


    private boolean getStateFromFilteredList(PumpHistoryEntryType... entryTypes) {
        if (isInit) {
            return false;
        } else {
            List<PumpHistoryEntry> filteredItems = getFilteredItems(entryTypes);

            if (isLogEnabled())
                LOG.debug("Items: " + filteredItems);

            return filteredItems.size() > 0;
        }
    }


    private List<PumpHistoryEntry> getFilteredItems(List<PumpHistoryEntry> inList, PumpHistoryEntryType... entryTypes) {

        // LOG.debug("InList: " + inList.size());
        List<PumpHistoryEntry> outList = new ArrayList<>();

        if (inList != null && inList.size() > 0) {
            for (PumpHistoryEntry pumpHistoryEntry : inList) {

                if (!isEmpty(entryTypes)) {
                    for (PumpHistoryEntryType pumpHistoryEntryType : entryTypes) {

                        if (pumpHistoryEntry.getEntryType() == pumpHistoryEntryType) {
                            outList.add(pumpHistoryEntry);
                            break;
                        }
                    }
                } else {
                    outList.add(pumpHistoryEntry);
                }
            }
        }

        // LOG.debug("OutList: " + outList.size());

        return outList;
    }


    private boolean isEmpty(PumpHistoryEntryType... entryTypes) {
        return (entryTypes == null || (entryTypes.length == 1 && entryTypes[0] == null));
    }


    private String getLogPrefix() {
        return "MedtronicHistoryData::";
    }

    private static boolean isLogEnabled() {
        return (L.isEnabled(L.PUMP));
    }

}
