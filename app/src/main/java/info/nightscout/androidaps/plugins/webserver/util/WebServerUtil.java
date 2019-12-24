package info.nightscout.androidaps.plugins.webserver.util;

import android.content.Context;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.plugins.treatments.Treatment;
import info.nightscout.androidaps.plugins.webserver.data.TreatmentObject;
import info.nightscout.androidaps.utils.SP;

public class WebServerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerUtil.class);

    public static final long SECOND_IN_MS = 1_000;
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;

    static final SimpleDateFormat formatNightscout = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    private static final Map<String, Long> rateLimits = new HashMap<>();
    private static final boolean debug_wakelocks = false;

    private static List<String> OBJECT_BASAL = Arrays.asList("", "");
    private static List<String> OBJECT_BOLUS = Arrays.asList("", "");

    // booleans
    public static boolean getBooleanDefaultFalse(final String pref) {
        return SP.getBoolean(pref, false);
    }

    // strings
    public static String getStringDefaultBlank(final String pref) {
        return SP.getString(pref, "");
    }

    public static boolean isMgDl() {
        return "mg/dl".equals(SP.getString(R.string.key_units, "mg/dl"));
    }


    public static String getString(final String pref, final String def) {
        return SP.getString(pref, def);
    }

    public static double roundDouble(final double value, int places) {
        if (places < 0) throw new IllegalArgumentException("Invalid decimal places");
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));
    }

    public static double tolerantParseDouble(final String str, final double def) {
        if (str == null) return def;
        try {
            return Double.parseDouble(str.replace(",", "."));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String toNightscoutFormat(long date) {
        formatNightscout.setTimeZone(TimeZone.getDefault());
        return formatNightscout.format(date);
    }


    public static long tsl() {
        return System.currentTimeMillis();
    }

    // return true if below rate limit
    public static synchronized boolean ratelimit(String name, int seconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (tsl() - rateLimits.get(name) < (seconds * 1000L))) {
            LOGGER.debug(name + " rate limited: " + seconds + " seconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, tsl());
        return true;
    }


    public static PowerManager.WakeLock getWakeLock(final String name, int millis) {
        final PowerManager pm = (PowerManager) MainApp.instance().getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
        wl.acquire(millis);
        if (debug_wakelocks) LOGGER.debug("getWakeLock: " + name + " " + wl.toString());
        return wl;
    }


    public static void releaseWakeLock(PowerManager.WakeLock wl) {
        if (debug_wakelocks) LOGGER.debug("releaseWakeLock: " + wl.toString());
        if (wl == null) return;
        if (wl.isHeld()) {
            try {
                wl.release();
            } catch (Exception e) {
                LOGGER.error("Error releasing wakelock: " + e);
            }
        }
    }

    public static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //
        }
    }

    public static BigDecimal getDelta(BgReading reading, BgReading reading2) {
        return new BigDecimal((reading2.value - reading.value)  * 1000).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal getMmolValue(double value) {
        return new BigDecimal(value / MMOLL_TO_MGDL).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    public static TreatmentObject convert(TemporaryBasal temporaryBasal) {
        return null;
    }

    public static TreatmentObject convert(Treatment treatment) {
        return null;
    }
}
