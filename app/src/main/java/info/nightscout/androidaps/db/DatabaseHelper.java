package info.nightscout.androidaps.db;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.OverlappingIntervals;
import info.nightscout.androidaps.database.BlockingAppRepository;
import info.nightscout.androidaps.database.entities.TemporaryTarget;
import info.nightscout.androidaps.database.entities.TherapyEvent;
import info.nightscout.androidaps.events.EventCareportalEventChange;
import info.nightscout.androidaps.events.EventExtendedBolusChange;
import info.nightscout.androidaps.events.EventNewBG;
import info.nightscout.androidaps.events.EventProfileNeedsUpdate;
import info.nightscout.androidaps.events.EventReloadProfileSwitchData;
import info.nightscout.androidaps.events.EventReloadTempBasalData;
import info.nightscout.androidaps.events.EventReloadTreatmentData;
import info.nightscout.androidaps.events.EventTempBasalChange;
import info.nightscout.androidaps.events.EventTempTargetChange;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.events.EventNewHistoryData;
import info.nightscout.androidaps.plugins.pump.danaR.activities.DanaRNSHistorySync;
import info.nightscout.androidaps.plugins.pump.insight.database.InsightBolusID;
import info.nightscout.androidaps.plugins.pump.insight.database.InsightHistoryOffset;
import info.nightscout.androidaps.plugins.pump.insight.database.InsightPumpID;
import info.nightscout.androidaps.utils.ProfileSwitchUtilKt;

/**
 * This Helper contains all resource to provide a central DB management functionality. Only methods handling
 * data-structure (and not the DB content) should be contained in here (meaning DDL and not SQL).
 * <p>
 * This class can safely be called from Services, but should not call Services to avoid circular dependencies.
 * One major issue with this (right now) are the scheduled events, which are put into the service. Therefor all
 * direct calls to the corresponding methods (eg. resetDatabases) should be done by a central service.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static Logger log = LoggerFactory.getLogger(L.DATABASE);

    public static final String DATABASE_NAME = "AndroidAPSDb";
    public static final String DATABASE_BGREADINGS = "BgReadings";
    public static final String DATABASE_TEMPORARYBASALS = "TemporaryBasals";
    public static final String DATABASE_EXTENDEDBOLUSES = "ExtendedBoluses";
    public static final String DATABASE_TEMPTARGETS = "TempTargets";
    public static final String DATABASE_DANARHISTORY = "DanaRHistory";
    public static final String DATABASE_DBREQUESTS = "DBRequests";
    public static final String DATABASE_CAREPORTALEVENTS = "CareportalEvents";
    public static final String DATABASE_PROFILESWITCHES = "ProfileSwitches";
    public static final String DATABASE_TDDS = "TDDs";
    public static final String DATABASE_INSIGHT_HISTORY_OFFSETS = "InsightHistoryOffsets";
    public static final String DATABASE_INSIGHT_BOLUS_IDS = "InsightBolusIDs";
    public static final String DATABASE_INSIGHT_PUMP_IDS = "InsightPumpIDs";

    private static final int DATABASE_VERSION = 11;

    public static Long earliestDataChange = null;

    private static final ScheduledExecutorService bgWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledBgPost = null;

    private static final ScheduledExecutorService tempBasalsWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledTemBasalsPost = null;

    private static final ScheduledExecutorService tempTargetWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledTemTargetPost = null;

    private static final ScheduledExecutorService extendedBolusWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledExtendedBolusPost = null;

    private static final ScheduledExecutorService careportalEventWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledCareportalEventPost = null;

    private static final ScheduledExecutorService profileSwitchEventWorker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledProfileSwitchEventPost = null;

    private int oldVersion = 0;
    private int newVersion = 0;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        onCreate(getWritableDatabase(), getConnectionSource());
        //onUpgrade(getWritableDatabase(), getConnectionSource(), 1,1);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            if (L.isEnabled(L.DATABASE))
                log.info("onCreate");
            TableUtils.createTableIfNotExists(connectionSource, TempTarget.class);
            TableUtils.createTableIfNotExists(connectionSource, DanaRHistoryRecord.class);
            TableUtils.createTableIfNotExists(connectionSource, DbRequest.class);
            TableUtils.createTableIfNotExists(connectionSource, TemporaryBasal.class);
            TableUtils.createTableIfNotExists(connectionSource, ExtendedBolus.class);
            TableUtils.createTableIfNotExists(connectionSource, CareportalEvent.class);
            TableUtils.createTableIfNotExists(connectionSource, ProfileSwitch.class);
            TableUtils.createTableIfNotExists(connectionSource, TDD.class);
            TableUtils.createTableIfNotExists(connectionSource, InsightHistoryOffset.class);
            TableUtils.createTableIfNotExists(connectionSource, InsightBolusID.class);
            TableUtils.createTableIfNotExists(connectionSource, InsightPumpID.class);
            database.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT \"" + DATABASE_INSIGHT_BOLUS_IDS + "\", " + System.currentTimeMillis() + " " +
                    "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = \"" + DATABASE_INSIGHT_BOLUS_IDS + "\")");
            database.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT \"" + DATABASE_INSIGHT_PUMP_IDS + "\", " + System.currentTimeMillis() + " " +
                    "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = \"" + DATABASE_INSIGHT_PUMP_IDS + "\")");
        } catch (SQLException e) {
            log.error("Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;

            if (oldVersion < 7) {
                log.info(DatabaseHelper.class.getName(), "onUpgrade");
                TableUtils.dropTable(connectionSource, TempTarget.class, true);
                TableUtils.dropTable(connectionSource, DanaRHistoryRecord.class, true);
                TableUtils.dropTable(connectionSource, DbRequest.class, true);
                TableUtils.dropTable(connectionSource, TemporaryBasal.class, true);
                TableUtils.dropTable(connectionSource, ExtendedBolus.class, true);
                TableUtils.dropTable(connectionSource, CareportalEvent.class, true);
                TableUtils.dropTable(connectionSource, ProfileSwitch.class, true);
                onCreate(database, connectionSource);
            } else if (oldVersion < 10) {
                TableUtils.createTableIfNotExists(connectionSource, InsightHistoryOffset.class);
                TableUtils.createTableIfNotExists(connectionSource, InsightBolusID.class);
                TableUtils.createTableIfNotExists(connectionSource, InsightPumpID.class);
                database.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT \"" + DATABASE_INSIGHT_BOLUS_IDS + "\", " + System.currentTimeMillis() + " " +
                        "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = \"" + DATABASE_INSIGHT_BOLUS_IDS + "\")");
                database.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT \"" + DATABASE_INSIGHT_PUMP_IDS + "\", " + System.currentTimeMillis() + " " +
                        "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = \"" + DATABASE_INSIGHT_PUMP_IDS + "\")");
            } else if (oldVersion < 11) {
                database.execSQL("UPDATE sqlite_sequence SET seq = " + System.currentTimeMillis() + " WHERE name = \"" + DATABASE_INSIGHT_BOLUS_IDS + "\"");
                database.execSQL("UPDATE sqlite_sequence SET seq = " + System.currentTimeMillis() + " WHERE name = \"" + DATABASE_INSIGHT_PUMP_IDS + "\"");
            }
        } catch (SQLException e) {
            log.error("Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.info("Do nothing for downgrading...");
        log.debug("oldVersion: {}, newVersion: {}", oldVersion, newVersion);
    }

    public int getOldVersion() {
        return oldVersion;
    }

    public int getNewVersion() {
        return newVersion;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
    }


    public long size(String database) {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), database);
    }

    // ------------------ getDao -------------------------------------------

    private Dao<DanaRHistoryRecord, String> getDaoDanaRHistory() throws SQLException {
        return getDao(DanaRHistoryRecord.class);
    }

    private Dao<DbRequest, String> getDaoDbRequest() throws SQLException {
        return getDao(DbRequest.class);
    }

    private Dao<ProfileSwitch, Long> getDaoProfileSwitch() throws SQLException {
        return getDao(ProfileSwitch.class);
    }

    public static long roundDateToSec(long date) {
        long rounded = date - date % 1000;
        if (rounded != date)
            if (L.isEnabled(L.DATABASE))
                log.debug("Rounding " + date + " to " + rounded);
        return rounded;
    }
    // -------------------  BgReading handling -----------------------

    public static void scheduleBgChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing EventNewBg");
                MainApp.bus().post(new EventNewBG());
                RxBus.INSTANCE.send(new EventNewBG());
                scheduledBgPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledBgPost != null)
            scheduledBgPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledBgPost = bgWorker.schedule(task, sec, TimeUnit.SECONDS);

    }

    // -------------------  TDD handling -----------------------
    public void createOrUpdateTDD(TDD tdd) {
    }


    // ------------- DbRequests handling -------------------

    public void create(DbRequest dbr) {
        try {
            getDaoDbRequest().create(dbr);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
    }

    public int delete(DbRequest dbr) {
        try {
            return getDaoDbRequest().delete(dbr);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
        return 0;
    }

    public int deleteDbRequest(String nsClientId) {
        try {
            return getDaoDbRequest().deleteById(nsClientId);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
        return 0;
    }

    public void deleteDbRequestbyMongoId(String action, String id) {
        try {
            QueryBuilder<DbRequest, String> queryBuilder = getDaoDbRequest().queryBuilder();
            Where where = queryBuilder.where();
            where.eq("_id", id).and().eq("action", action);
            queryBuilder.limit(10L);
            PreparedQuery<DbRequest> preparedQuery = queryBuilder.prepare();
            List<DbRequest> dbList = getDaoDbRequest().query(preparedQuery);
            for (DbRequest r : dbList) {
                delete(r);
            }
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
    }

    public void deleteAllDbRequests() {
        try {
            TableUtils.clearTable(connectionSource, DbRequest.class);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
    }

    public CloseableIterator getDbRequestInterator() {
        try {
            return getDaoDbRequest().closeableIterator();
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
            return null;
        }
    }

    //  -------------------- TREATMENT HANDLING -------------------

    public static void updateEarliestDataChange(long newDate) {
        if (earliestDataChange == null) {
            earliestDataChange = newDate;
            return;
        }
        if (newDate < earliestDataChange) {
            earliestDataChange = newDate;
        }
    }

    // ---------------- TempTargets handling ---------------

    public List<TempTarget> getTemptargetsDataFromTime(long mills, boolean ascending) {
        return getTemptargetsDataFromTime(mills, Long.MAX_VALUE, ascending);
    }

    public List<TempTarget> getTemptargetsDataFromTime(long from, long to, boolean ascending) {
        List<TempTarget> convertedTTs = new ArrayList<>();
        for (TemporaryTarget temporaryTarget : BlockingAppRepository.INSTANCE.getTemporaryTargetsInTimeRange(from, to)) {
            TempTarget converted = new TempTarget();
            converted.backing = temporaryTarget;
            converted.date = temporaryTarget.getTimestamp();
            converted.durationInMinutes = (int) Math.round(temporaryTarget.getDuration() / 60000D);
            converted.high = temporaryTarget.getTarget();
            converted.low = temporaryTarget.getTarget();
            switch (temporaryTarget.getReason()) {
                case ACTIVITY:
                    converted.reason = MainApp.gs(R.string.activity);
                    break;
                case EATING_SOON:
                    converted.reason = MainApp.gs(R.string.eatingsoon);
                    break;
                case HYPOGLYCEMIA:
                    converted.reason = MainApp.gs(R.string.hypo);
                    break;
                case CUSTOM:
                    converted.reason = MainApp.gs(R.string.manual);
                    break;
            }
            convertedTTs.add(converted);
        }
        if (!ascending) Collections.shuffle(convertedTTs);
        return convertedTTs;
    }

    public static void scheduleTemporaryTargetChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing EventTempTargetChange");
                MainApp.bus().post(new EventTempTargetChange());
                scheduledTemTargetPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledTemTargetPost != null)
            scheduledTemTargetPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledTemTargetPost = tempTargetWorker.schedule(task, sec, TimeUnit.SECONDS);

    }

    // ----------------- DanaRHistory handling --------------------

    public void createOrUpdate(DanaRHistoryRecord record) {
        try {
            getDaoDanaRHistory().createOrUpdate(record);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
        }
    }

    public List<DanaRHistoryRecord> getDanaRHistoryRecordsByType(byte type) {
        List<DanaRHistoryRecord> historyList;
        try {
            QueryBuilder<DanaRHistoryRecord, String> queryBuilder = getDaoDanaRHistory().queryBuilder();
            queryBuilder.orderBy("recordDate", false);
            Where where = queryBuilder.where();
            where.eq("recordCode", type);
            queryBuilder.limit(200L);
            PreparedQuery<DanaRHistoryRecord> preparedQuery = queryBuilder.prepare();
            historyList = getDaoDanaRHistory().query(preparedQuery);
        } catch (SQLException e) {
            log.error("Unhandled exception", e);
            historyList = new ArrayList<>();
        }
        return historyList;
    }

    public void updateDanaRHistoryRecordId(JSONObject trJson) {
        try {
            QueryBuilder<DanaRHistoryRecord, String> queryBuilder = getDaoDanaRHistory().queryBuilder();
            Where where = queryBuilder.where();
            where.ge("bytes", trJson.get(DanaRNSHistorySync.DANARSIGNATURE));
            PreparedQuery<DanaRHistoryRecord> preparedQuery = queryBuilder.prepare();
            List<DanaRHistoryRecord> list = getDaoDanaRHistory().query(preparedQuery);
            if (list.size() == 0) {
                // Record does not exists. Ignore
            } else if (list.size() == 1) {
                DanaRHistoryRecord record = list.get(0);
                if (record._id == null || !record._id.equals(trJson.getString("_id"))) {
                    if (L.isEnabled(L.DATABASE))
                        log.debug("Updating _id in DanaR history database: " + trJson.getString("_id"));
                    record._id = trJson.getString("_id");
                    getDaoDanaRHistory().update(record);
                } else {
                    // already set
                }
            }
        } catch (SQLException | JSONException e) {
            log.error("Unhandled exception: " + trJson.toString(), e);
        }
    }

    // ------------ TemporaryBasal handling ---------------

    public List<TemporaryBasal> getTemporaryBasalsDataFromTime(long mills, boolean ascending) {
        return getTemporaryBasalsDataFromTime(mills, Long.MAX_VALUE, ascending);
    }

    public List<TemporaryBasal> getTemporaryBasalsDataFromTime(long from, long to, boolean ascending) {
        List<TemporaryBasal> convertedTBRs = new ArrayList<>();
        for (info.nightscout.androidaps.database.entities.TemporaryBasal tbr : BlockingAppRepository.INSTANCE.getTemporaryBasalsInTimeRange(from, to)) {
            TemporaryBasal converted = new TemporaryBasal();
            converted.backing = tbr;
            converted.date = tbr.getTimestamp();
            converted.durationInMinutes = (int) Math.round(tbr.getDuration() / 60000D);
            converted.isAbsolute = tbr.isAbsolute();
            if (tbr.isAbsolute()) {
                converted.absoluteRate = tbr.getRate();
            } else {
                converted.percentRate = (int) Math.round(tbr.getRate());
            }
            convertedTBRs.add(converted);
        }
        if (!ascending) Collections.reverse(convertedTBRs);
        return convertedTBRs;
    }

    public static void scheduleTemporaryBasalChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing EventTempBasalChange");
                MainApp.bus().post(new EventReloadTempBasalData());
                MainApp.bus().post(new EventTempBasalChange());
                if (earliestDataChange != null)
                    MainApp.bus().post(new EventNewHistoryData(earliestDataChange));
                earliestDataChange = null;
                scheduledTemBasalsPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledTemBasalsPost != null)
            scheduledTemBasalsPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledTemBasalsPost = tempBasalsWorker.schedule(task, sec, TimeUnit.SECONDS);

    }

    // ------------ ExtendedBolus handling ---------------

    public List<ExtendedBolus> getExtendedBolusDataFromTime(long mills, boolean ascending) {
        List<ExtendedBolus> convertedEBs = new ArrayList<>();
        for (info.nightscout.androidaps.database.entities.ExtendedBolus extendedBolus : BlockingAppRepository.INSTANCE.getExtendedBolusesInTimeRange(mills, Long.MAX_VALUE)) {
            ExtendedBolus converted = new ExtendedBolus();
            converted.backing = extendedBolus;
            converted.date = extendedBolus.getTimestamp();
            converted.durationInMinutes = (int) Math.round(extendedBolus.getDuration() / 60000D);
            converted.insulin = extendedBolus.getAmount();
            convertedEBs.add(converted);
        }
        if (!ascending) Collections.reverse(convertedEBs);
        return convertedEBs;
    }

    public static void scheduleExtendedBolusChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing EventExtendedBolusChange");
                MainApp.bus().post(new EventReloadTreatmentData(new EventExtendedBolusChange()));
                if (earliestDataChange != null)
                    MainApp.bus().post(new EventNewHistoryData(earliestDataChange));
                earliestDataChange = null;
                scheduledExtendedBolusPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledExtendedBolusPost != null)
            scheduledExtendedBolusPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledExtendedBolusPost = extendedBolusWorker.schedule(task, sec, TimeUnit.SECONDS);

    }


    // ------------ CareportalEvent handling ---------------

    private CareportalEvent convertTherapyEvent(TherapyEvent therapyEvent) {
        if (therapyEvent == null) return null;
        try {
            CareportalEvent careportalEvent = new CareportalEvent();
            careportalEvent.backing = therapyEvent;
            careportalEvent.date = therapyEvent.getTimestamp();
            JSONObject jsonObject = new JSONObject();
            switch (therapyEvent.getType()) {
                case FINGER_STICK_BG_VALUE:
                    careportalEvent.eventType = CareportalEvent.BGCHECK;
                    jsonObject.put("units", Constants.MGDL);
                    jsonObject.put("glucose", therapyEvent.getAmount());
                    break;
                case ANNOUNCEMENT:
                    careportalEvent.eventType = CareportalEvent.ANNOUNCEMENT;
                    jsonObject.put("notes", therapyEvent.getNote());
                    break;
                case QUESTION:
                    careportalEvent.eventType = CareportalEvent.QUESTION;
                    jsonObject.put("notes", therapyEvent.getNote());
                    break;
                case NOTE:
                    careportalEvent.eventType = CareportalEvent.NOTE;
                    jsonObject.put("notes", therapyEvent.getNote());
                    break;
                case ACTIVITY:
                    careportalEvent.eventType = CareportalEvent.EXERCISE;
                    jsonObject.put("duration", Math.round(therapyEvent.getDuration() / 1000D));
                    break;
                case SENSOR_INSERTED:
                    careportalEvent.eventType = CareportalEvent.SENSORCHANGE;
                    break;
                case CANNULA_CHANGED:
                    careportalEvent.eventType = CareportalEvent.SITECHANGE;
                    break;
                case APS_OFFLINE:
                    careportalEvent.eventType = CareportalEvent.OPENAPSOFFLINE;
                    break;
                case RESERVOIR_CHANGED:
                    careportalEvent.eventType = CareportalEvent.INSULINCHANGE;
                    break;
                case BATTERY_CHANGED:
                    careportalEvent.eventType = CareportalEvent.PUMPBATTERYCHANGE;
                    break;
            }
            careportalEvent.json = jsonObject.toString();
            return careportalEvent;
        } catch (JSONException e) {
            log.error("Unhandled exception", e);
        }
        return null;
    }

    private TherapyEvent.Type convertType(String type) {
        switch (type) {
            case CareportalEvent.INSULINCHANGE:
                return TherapyEvent.Type.RESERVOIR_CHANGED;
            case CareportalEvent.PUMPBATTERYCHANGE:
                return TherapyEvent.Type.BATTERY_CHANGED;
            case CareportalEvent.SITECHANGE:
                return TherapyEvent.Type.CANNULA_CHANGED;
            case CareportalEvent.SENSORCHANGE:
                return TherapyEvent.Type.SENSOR_INSERTED;
        }
        return null;
    }

    @Nullable
    public CareportalEvent getLastCareportalEvent(String event) {
        return convertTherapyEvent(BlockingAppRepository.INSTANCE.getLastTherapyEventByType(convertType(event)));
    }

    public List<CareportalEvent> getCareportalEventsFromTime(long mills, boolean ascending) {
        List<CareportalEvent> converted = new ArrayList<>();
        for (TherapyEvent therapyEvent : BlockingAppRepository.INSTANCE.getTherapyEventsInTimeRange(mills, Long.MAX_VALUE))
            converted.add(convertTherapyEvent(therapyEvent));
        if (!ascending) Collections.reverse(converted);
        preprocessOpenAPSOfflineEvents(converted);
        return converted;
    }

    public void preprocessOpenAPSOfflineEvents(List<CareportalEvent> list) {
        OverlappingIntervals offlineEvents = new OverlappingIntervals();
        for (int i = 0; i < list.size(); i++) {
            CareportalEvent event = list.get(i);
            if (!event.eventType.equals(CareportalEvent.OPENAPSOFFLINE)) continue;
            offlineEvents.add(event);
        }

    }

    public List<CareportalEvent> getCareportalEvents(long start, long end, boolean ascending) {
        List<CareportalEvent> converted = new ArrayList<>();
        for (TherapyEvent therapyEvent : BlockingAppRepository.INSTANCE.getTherapyEventsInTimeRange(start, end))
            converted.add(convertTherapyEvent(therapyEvent));
        if (!ascending) Collections.reverse(converted);
        preprocessOpenAPSOfflineEvents(converted);
        return converted;
    }

    public List<CareportalEvent> getCareportalEventsFromTime(long mills, String type, boolean ascending) {
        List<CareportalEvent> converted = new ArrayList<>();
        for (TherapyEvent therapyEvent : BlockingAppRepository.INSTANCE.getTherapyEventsInTimeRange(convertType(type), mills, Long.MAX_VALUE))
            converted.add(convertTherapyEvent(therapyEvent));
        if (!ascending) Collections.reverse(converted);
        preprocessOpenAPSOfflineEvents(converted);
        return converted;
    }

    public List<CareportalEvent> getCareportalEvents(boolean ascending) {
        List<CareportalEvent> converted = new ArrayList<>();
        for (TherapyEvent therapyEvent : BlockingAppRepository.INSTANCE.getAllTherapyEvents())
            converted.add(convertTherapyEvent(therapyEvent));
        if (!ascending) Collections.reverse(converted);
        preprocessOpenAPSOfflineEvents(converted);
        return converted;
    }

    public static void scheduleCareportalEventChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing scheduleCareportalEventChange");
                MainApp.bus().post(new EventCareportalEventChange());
                scheduledCareportalEventPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledCareportalEventPost != null)
            scheduledCareportalEventPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledCareportalEventPost = careportalEventWorker.schedule(task, sec, TimeUnit.SECONDS);

    }

    // ---------------- ProfileSwitch handling ---------------

    public List<ProfileSwitch> getProfileSwitchData(boolean ascending) {
        List<ProfileSwitch> converted = new ArrayList<>();
        for (info.nightscout.androidaps.database.entities.ProfileSwitch profileSwitch : BlockingAppRepository.INSTANCE.getAllProfileSwitches())
            converted.add(ProfileSwitchUtilKt.convert(profileSwitch));
        if (!ascending) Collections.reverse(converted);
        return converted;
    }

    public List<ProfileSwitch> getProfileSwitchEventsFromTime(long mills, boolean ascending) {
        return getProfileSwitchEventsFromTime(mills, Long.MAX_VALUE, ascending);
    }

    public List<ProfileSwitch> getProfileSwitchEventsFromTime(long from, long to, boolean ascending) {
        List<ProfileSwitch> converted = new ArrayList<>();
        for (info.nightscout.androidaps.database.entities.ProfileSwitch profileSwitch : BlockingAppRepository.INSTANCE.getProfileSwitchesInTimeRange(from, to))
            converted.add(ProfileSwitchUtilKt.convert(profileSwitch));
        if (!ascending) Collections.reverse(converted);
        return converted;
    }


    public static void scheduleProfileSwitchChange() {
        class PostRunnable implements Runnable {
            public void run() {
                if (L.isEnabled(L.DATABASE))
                    log.debug("Firing EventProfileNeedsUpdate");
                MainApp.bus().post(new EventReloadProfileSwitchData());
                MainApp.bus().post(new EventProfileNeedsUpdate());
                scheduledProfileSwitchEventPost = null;
            }
        }
        // prepare task for execution in 1 sec
        // cancel waiting task to prevent sending multiple posts
        if (scheduledProfileSwitchEventPost != null)
            scheduledProfileSwitchEventPost.cancel(false);
        Runnable task = new PostRunnable();
        final int sec = 1;
        scheduledProfileSwitchEventPost = profileSwitchEventWorker.schedule(task, sec, TimeUnit.SECONDS);

    }
}
