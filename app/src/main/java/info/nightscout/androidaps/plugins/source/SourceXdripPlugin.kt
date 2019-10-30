package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.transactions.CgmSourceTransaction
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.BundleLogger
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.services.Intents
import info.nightscout.androidaps.utils.determineSourceSensor
import info.nightscout.androidaps.utils.toTrendArrow
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourceXdripPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.xdrip)
        .description(R.string.description_source_xdrip)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private var advancedFiltering: Boolean = false

    override fun advancedFilteringSupported(): Boolean {
        return advancedFiltering
    }

    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return

        try {
            val bundle = intent.extras!!

            if (L.isEnabled(L.BGSOURCE))
                log.debug("Received xDrip data: " + BundleLogger.log(bundle))

            val source = bundle.getString(Intents.XDRIP_DATA_SOURCE_DESCRIPTION)
            log.debug("TrendArrow: " + bundle.getString(Intents.EXTRA_BG_SLOPE_NAME))
            this.advancedFiltering = source?.let {
                it.contains("G5 Native") || it.contains("G6 Native")
            } ?: false
            BlockingAppRepository.runTransaction(CgmSourceTransaction(listOf(CgmSourceTransaction.GlucoseValue(
                    timestamp = bundle.getLong(Intents.EXTRA_TIMESTAMP),
                    value = bundle.getDouble(Intents.EXTRA_BG_ESTIMATE),
                    raw = bundle.getDouble(Intents.EXTRA_RAW),
                    sourceSensor = source.determineSourceSensor(),
                    noise = null,
                    trendArrow = bundle.getString(Intents.EXTRA_BG_SLOPE_NAME)!!.toTrendArrow()
            )), listOf(), null))
        } catch (e: Throwable) {
            log.error("Error while processing intent", e)
        }
    }
}
