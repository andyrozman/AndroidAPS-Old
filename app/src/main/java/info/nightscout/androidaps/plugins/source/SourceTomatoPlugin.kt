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
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.utils.SP
import org.slf4j.LoggerFactory

/**
 * Created by mike on 05.08.2016.
 */
object SourceTomatoPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.tomato)
        .preferencesId(R.xml.pref_poctech)
        .shortName(R.string.tomato_short)
        .description(R.string.description_source_tomato)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        if (L.isEnabled(L.BGSOURCE))
            log.debug("Received Tomato Data")
        BlockingAppRepository.runTransactionForResult(CgmSourceTransaction(listOf(CgmSourceTransaction.GlucoseValue(
                timestamp = bundle.getLong("com.fangies.tomatofn.Extras.Time"),
                value = bundle.getDouble("com.fangies.tomatofn.Extras.BgEstimate"),
                trendArrow = GlucoseValue.TrendArrow.NONE,
                raw = null,
                noise = null,
                sourceSensor = GlucoseValue.SourceSensor.TOMATO
        )), listOf(), null)).firstOrNull()?.let {
            if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false)) {
                NSUpload.uploadBg(it, "AndroidAPS-Tomato")
            }
            if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false)) {
                NSUpload.sendToXdrip(it)
            }
        }
    }

}
