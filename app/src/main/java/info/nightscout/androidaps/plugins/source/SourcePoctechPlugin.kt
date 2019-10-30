package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.transactions.CgmSourceTransaction
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONArray
import org.json.JSONException
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourcePoctechPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.poctech)
        .preferencesId(R.xml.pref_poctech)
        .description(R.string.description_source_poctech)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        val data = bundle.getString("data")
        if (L.isEnabled(L.BGSOURCE))
            log.debug("Received Poctech Data", data)

        try {
            val jsonArray = JSONArray(data)
            if (L.isEnabled(L.BGSOURCE))
                log.debug("Received Poctech Data size:" + jsonArray.length())
            val glucoseValues = mutableListOf<CgmSourceTransaction.GlucoseValue>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                glucoseValues.add(CgmSourceTransaction.GlucoseValue(
                        timestamp = json.getLong("date"),
                        value = json.getDouble("current").let {
                            if (JsonHelper.safeGetString(json, "units", Constants.MGDL) == "mmol/L")
                                it * Constants.MMOLL_TO_MGDL
                            else it
                        },
                        trendArrow = json.getString("direction").toTrendArrow(),
                        raw = null,
                        noise = null,
                        sourceSensor = GlucoseValue.SourceSensor.POCTECH_NATIVE
                ))
            }
            BlockingAppRepository.runTransactionForResult(CgmSourceTransaction(glucoseValues, listOf(), null)).forEach {
                if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false)) {
                    NSUpload.uploadBg(it, "AndroidAPS-Poctech")
                }
                if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false)) {
                    NSUpload.sendToXdrip(it)
                }
            }
        } catch (e: JSONException) {
            log.error("Exception: ", e)
        }

    }

}
