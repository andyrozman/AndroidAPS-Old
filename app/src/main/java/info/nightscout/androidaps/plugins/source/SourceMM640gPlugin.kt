package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.transactions.CgmSourceTransaction
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONArray
import org.json.JSONException
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourceMM640gPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.MM640g)
        .description(R.string.description_source_mm640g)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        val collection = bundle.getString("collection") ?: return

        if (collection == "entries") {
            val data = bundle.getString("data")
            if (L.isEnabled(L.BGSOURCE))
                log.debug("Received MM640g Data: ", data)

            val glucoseValues = mutableListOf<CgmSourceTransaction.GlucoseValue>()
            if (data != null && data.isNotEmpty()) {
                try {
                    val jsonArray = JSONArray(data)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val type = jsonObject.getString("type")
                        if (type == "sgv") {
                            glucoseValues.add(CgmSourceTransaction.GlucoseValue(
                                    timestamp = jsonObject.getLong("date"),
                                    value = jsonObject.getDouble("sgv"),
                                    raw = null,
                                    noise = null,
                                    trendArrow = jsonObject.getString("direction").toTrendArrow(),
                                    sourceSensor = GlucoseValue.SourceSensor.MM_600_SERIES
                            ))
                        } else {
                            if (L.isEnabled(L.BGSOURCE)) {
                                log.debug("Unknown entries type: $type")
                            }
                        }
                    }
                } catch (e: JSONException) {
                    log.error("Exception: ", e)
                }
            }
            BlockingAppRepository.runTransaction(CgmSourceTransaction(glucoseValues, listOf(), null))
        }
    }
}
