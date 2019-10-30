package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.transactions.CgmSourceTransaction
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSgv
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.determineSourceSensor
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourceNSClientPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.nsclientbg)
        .description(R.string.description_source_ns_client)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private var lastBGTimeStamp: Long = 0
    private var isAdvancedFilteringEnabled = false

    override fun advancedFilteringSupported(): Boolean {
        return isAdvancedFilteringEnabled
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE) && !SP.getBoolean(R.string.key_ns_autobackfill, true))
            return

        val bundles = intent.extras

        try {
            val glucoseValues = mutableListOf<CgmSourceTransaction.GlucoseValue>()
            if (bundles!!.containsKey("sgv")) {
                val sgvstring = bundles.getString("sgv")
                if (L.isEnabled(L.BGSOURCE))
                    log.debug("Received NS Data: " + sgvstring!!)

                val sgvJson = JSONObject(sgvstring)
                glucoseValues.add(createGlucoseValue(sgvJson))
            }

            if (bundles.containsKey("sgvs")) {
                val sgvstring = bundles.getString("sgvs")
                if (L.isEnabled(L.BGSOURCE))
                    log.debug("Received NS Data: " + sgvstring!!)
                val jsonArray = JSONArray(sgvstring)
                for (i in 0 until jsonArray.length()) {
                    val sgvJson = jsonArray.getJSONObject(i)
                    glucoseValues.add(createGlucoseValue(sgvJson))
                }
            }
            BlockingAppRepository.runTransaction(CgmSourceTransaction(glucoseValues, listOf(), null))
        } catch (e: Exception) {
            log.error("Unhandled exception", e)
        }

        SP.putBoolean(R.string.key_ObjectivesbgIsAvailableInNS, true);
    }

    private fun createGlucoseValue(sgvJson: JSONObject): CgmSourceTransaction.GlucoseValue {
        val nsSgv = NSSgv(sgvJson)

        val source = JsonHelper.safeGetString(sgvJson, "device", "none")
        detectSource(source, JsonHelper.safeGetLong(sgvJson, "mills"))
        return CgmSourceTransaction.GlucoseValue(
                timestamp = nsSgv.mills,
                value = nsSgv.mgdl.toDouble(),
                raw = null,
                noise = null,
                trendArrow = nsSgv.direction.toTrendArrow(),
                sourceSensor = source.determineSourceSensor(),
                nightscoutId = nsSgv.id
        )
    }

    private fun detectSource(source: String, timeStamp: Long) {
        if (timeStamp > lastBGTimeStamp) {
            isAdvancedFilteringEnabled = source.contains("G5 Native") || source.contains("G6 Native") || source.contains("AndroidAPS-Dexcom")
            lastBGTimeStamp = timeStamp
        }
    }
}
