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
import info.nightscout.androidaps.logging.BundleLogger
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.utils.toTrendArrow
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourceGlimpPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.Glimp)
        .description(R.string.description_source_glimp)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        if (L.isEnabled(L.BGSOURCE))
            log.debug("Received Glimp Data: " + BundleLogger.log(bundle))

        BlockingAppRepository.runTransactionForResult(CgmSourceTransaction(
                listOf(CgmSourceTransaction.GlucoseValue(
                        timestamp = bundle.getLong("myTimestamp"),
                        value = bundle.getDouble("mySGV"),
                        noise = null,
                        raw = null,
                        trendArrow = bundle.getString("myTrend")!!.toTrendArrow(),
                        sourceSensor = GlucoseValue.SourceSensor.GLIMP
                )), listOf(), null)).firstOrNull()?.let {
        }
    }
}
