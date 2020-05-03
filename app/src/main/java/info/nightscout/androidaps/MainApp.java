package info.nightscout.androidaps;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import androidx.annotation.StringRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.dependencyInjection.DaggerAppComponent;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.configBuilder.PluginStore;
import info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerUtils;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.receivers.BTReceiver;
import info.nightscout.androidaps.receivers.ChargingStateReceiver;
import info.nightscout.androidaps.receivers.DataReceiver;
import info.nightscout.androidaps.receivers.KeepAliveReceiver;
import info.nightscout.androidaps.receivers.NetworkChangeReceiver;
import info.nightscout.androidaps.receivers.TimeDateOrTZChangeReceiver;
import info.nightscout.androidaps.services.Intents;
import info.nightscout.androidaps.utils.ActivityMonitor;
import info.nightscout.androidaps.utils.LocaleHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

public class MainApp extends DaggerApplication {

    static MainApp sInstance;
    private static Resources sResources;

    static DatabaseHelper sDatabaseHelper = null;

    @Inject PluginStore pluginStore;
    @Inject AAPSLogger aapsLogger;
    @Inject ActivityMonitor activityMonitor;
    @Inject VersionCheckerUtils versionCheckersUtils;
    @Inject SP sp;

    @Inject ConfigBuilderPlugin configBuilderPlugin;
    @Inject KeepAliveReceiver.KeepAliveManager keepAliveManager;
    @Inject List<PluginBase> plugins;

    @Override
    public void onCreate() {
        super.onCreate();

        aapsLogger.debug("onCreate");
        sInstance = this;
        sResources = getResources();
        LocaleHelper.INSTANCE.update(this);
        sDatabaseHelper = OpenHelperManager.getHelper(sInstance, DatabaseHelper.class);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            if (ex instanceof InternalError) {
                // usually the app trying to spawn a thread while being killed
                return;
            }
            aapsLogger.error("Uncaught exception crashing app", ex);
        });

        registerActivityLifecycleCallbacks(activityMonitor);

        JodaTimeAndroid.init(this);

        aapsLogger.debug("Version: " + BuildConfig.VERSION_NAME);
        aapsLogger.debug("BuildVersion: " + BuildConfig.BUILDVERSION);
        aapsLogger.debug("Remote: " + BuildConfig.REMOTE);

        registerLocalBroadcastReceiver();

        //trigger here to see the new version on app start after an update
        versionCheckersUtils.triggerCheckVersion();

        // Register all tabs in app here
        pluginStore.setPlugins(plugins);
        configBuilderPlugin.initialize();

        NSUpload.uploadAppStart();

        new Thread(() -> keepAliveManager.setAlarm(this)).start();
        doMigrations();
    }


    private void doMigrations() {

    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerAppComponent
                .builder()
                .application(this)
                .build();
    }

    private void registerLocalBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_NEW_TREATMENT);
        filter.addAction(Intents.ACTION_CHANGED_TREATMENT);
        filter.addAction(Intents.ACTION_REMOVED_TREATMENT);
        filter.addAction(Intents.ACTION_NEW_SGV);
        filter.addAction(Intents.ACTION_NEW_PROFILE);
        filter.addAction(Intents.ACTION_NEW_MBG);
        filter.addAction(Intents.ACTION_NEW_CAL);
        LocalBroadcastManager.getInstance(this).registerReceiver(new DataReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(new TimeDateOrTZChangeReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(new NetworkChangeReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(new ChargingStateReceiver(), filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(new BTReceiver(), filter);
    }

    @Deprecated
    public static String gs(@StringRes int id) {
        return sResources.getString(id);
    }

    @Deprecated
    public static MainApp instance() {
        return sInstance;
    }

    public static DatabaseHelper getDbHelper() {
        return sDatabaseHelper;
    }

    @Override
    public void onTerminate() {
        aapsLogger.debug(LTag.CORE, "onTerminate");
        unregisterActivityLifecycleCallbacks(activityMonitor);
        keepAliveManager.cancelAlarm(this);
        super.onTerminate();
    }
}
