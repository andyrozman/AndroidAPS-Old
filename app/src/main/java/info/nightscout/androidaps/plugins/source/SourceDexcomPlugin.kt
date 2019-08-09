package info.nightscout.androidaps.plugins.source

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.activities.RequestDexcomPermissionActivity
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.transactions.GlucoseValuesTransaction
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.toTrendArrow
import org.slf4j.LoggerFactory

object SourceDexcomPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.dexcom_app_patched)
        .shortName(R.string.dexcom_short)
        .preferencesId(R.xml.pref_bgsource)
        .description(R.string.description_source_dexcom)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private val PACKAGE_NAMES = arrayOf("com.dexcom.cgm.region1.mgdl", "com.dexcom.cgm.region1.mmol",
            "com.dexcom.cgm.region2.mgdl", "com.dexcom.cgm.region2.mmol",
            "com.dexcom.g6.region1.mmol", "com.dexcom.g6.region2.mgdl",
            "com.dexcom.g6.region3.mgdl", "com.dexcom.g6.region3.mmol")

    const val PERMISSION = "com.dexcom.cgm.EXTERNAL_PERMISSION"

    override fun advancedFilteringSupported(): Boolean {
        return true
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(MainApp.instance(), PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MainApp.instance(), RequestDexcomPermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            MainApp.instance().startActivity(intent)
        }
    }

    fun findDexcomPackageName(): String? {
        val packageManager = MainApp.instance().packageManager;
        for (packageInfo in packageManager.getInstalledPackages(0)) {
            if (PACKAGE_NAMES.contains(packageInfo.packageName)) return packageInfo.packageName
        }
        return null
    }

    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return
        try {
            val glucoseValuesBundle = intent.getBundleExtra("glucoseValues")!!
            val glucoseValues = mutableListOf<GlucoseValuesTransaction.GlucoseValue>()
            for (i in 0 until glucoseValuesBundle.size()) {
                val glucoseValueBundle = glucoseValuesBundle.getBundle(i.toString())!!
                glucoseValues.add(GlucoseValuesTransaction.GlucoseValue(
                        timestamp = glucoseValueBundle.getLong("timestamp") * 1000,
                        value = glucoseValueBundle.getInt("glucoseValue").toDouble(),
                        noise = null,
                        raw = null,
                        trendArrow = glucoseValueBundle.getString("trendArrow")!!.toTrendArrow(),
                        sourceSensor = GlucoseValue.SourceSensor.DEXCOM_NATIVE_UNKNOWN
                ))
            }
            val meters = intent.getBundleExtra("meters")
            val calibrations = mutableListOf<GlucoseValuesTransaction.Calibration>()
            for (i in 0 until meters.size()) {
                val meter = meters.getBundle(i.toString())!!
                calibrations.add(GlucoseValuesTransaction.Calibration(meter.getLong("timestamp") * 1000,
                        meter.getInt("meterValue").toDouble()))
            }
            val sensorStartTime = if (SP.getBoolean(R.string.key_dexcom_lognssensorchange, false)) {
                if (intent.hasExtra("sensorInsertionTime")) {
                    intent.getLongExtra("sensorInsertionTime", 0) * 1000
                } else {
                    null
                }
            } else {
                null
            }
            BlockingAppRepository.runTransactionForResult(GlucoseValuesTransaction(glucoseValues, calibrations, sensorStartTime)).forEach {
                if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false)) {
                    NSUpload.uploadBg(it, "AndroidAPS-Poctech")
                }
                if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false)) {
                    NSUpload.sendToXdrip(it)
                }
            }
        } catch (e: Exception) {
            log.error("Error while processing intent from Dexcom App", e)
        }
    }
}
