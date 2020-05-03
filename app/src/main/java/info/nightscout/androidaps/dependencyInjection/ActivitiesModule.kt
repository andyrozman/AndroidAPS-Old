package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.MainActivity
import info.nightscout.androidaps.activities.*
import info.nightscout.androidaps.historyBrowser.HistoryBrowseActivity
import info.nightscout.androidaps.plugins.general.maintenance.activities.LogSettingActivity
import info.nightscout.androidaps.plugins.general.overview.activities.QuickWizardListActivity
import info.nightscout.androidaps.plugins.general.smsCommunicator.activities.SmsCommunicatorOtpActivity
import info.nightscout.androidaps.plugins.pump.common.dialog.RileyLinkBLEScanActivity
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.dialog.RileyLinkStatusActivity
import info.nightscout.androidaps.plugins.pump.danaR.activities.DanaRHistoryActivity
import info.nightscout.androidaps.plugins.pump.danaR.activities.DanaRUserOptionsActivity
import info.nightscout.androidaps.plugins.pump.danaRS.activities.BLEScanActivity
import info.nightscout.androidaps.plugins.pump.danaRS.activities.PairingHelperActivity
import info.nightscout.androidaps.plugins.pump.insight.activities.InsightAlertActivity
import info.nightscout.androidaps.plugins.pump.insight.activities.InsightPairingActivity
import info.nightscout.androidaps.plugins.pump.insight.activities.InsightPairingInformationActivity
import info.nightscout.androidaps.plugins.pump.medtronic.dialog.MedtronicHistoryActivity
import info.nightscout.androidaps.setupwizard.SetupWizardActivity

@Module
@Suppress("unused")
abstract class ActivitiesModule {

    @ContributesAndroidInjector abstract fun contributesBLEScanActivity(): BLEScanActivity
    @ContributesAndroidInjector abstract fun contributeBolusProgressHelperActivity(): BolusProgressHelperActivity
    @ContributesAndroidInjector abstract fun contributeDanaRHistoryActivity(): DanaRHistoryActivity
    @ContributesAndroidInjector abstract fun contributeDanaRUserOptionsActivity(): DanaRUserOptionsActivity
    @ContributesAndroidInjector abstract fun contributeErrorHelperActivity(): ErrorHelperActivity
    @ContributesAndroidInjector abstract fun contributesHistoryBrowseActivity(): HistoryBrowseActivity
    @ContributesAndroidInjector abstract fun contributesInsightAlertActivity(): InsightAlertActivity
    @ContributesAndroidInjector abstract fun contributesInsightPairingActivity(): InsightPairingActivity
    @ContributesAndroidInjector abstract fun contributesInsightPairingInformationActivity(): InsightPairingInformationActivity
    @ContributesAndroidInjector abstract fun contributesLogSettingActivity(): LogSettingActivity
    @ContributesAndroidInjector abstract fun contributeMainActivity(): MainActivity
    @ContributesAndroidInjector abstract fun contributesMedtronicHistoryActivity(): MedtronicHistoryActivity
    @ContributesAndroidInjector abstract fun contributesPairingHelperActivity(): PairingHelperActivity
    @ContributesAndroidInjector abstract fun contributesPreferencesActivity(): PreferencesActivity
    @ContributesAndroidInjector abstract fun contributesQuickWizardListActivity(): QuickWizardListActivity
    @ContributesAndroidInjector abstract fun contributesRequestDexcomPermissionActivity(): RequestDexcomPermissionActivity
    @ContributesAndroidInjector abstract fun contributesRileyLinkStatusActivity(): RileyLinkStatusActivity
    @ContributesAndroidInjector abstract fun contributesRileyLinkBLEScanActivity(): RileyLinkBLEScanActivity
    @ContributesAndroidInjector abstract fun contributesSetupWizardActivity(): SetupWizardActivity
    @ContributesAndroidInjector abstract fun contributesSingleFragmentActivity(): SingleFragmentActivity
    @ContributesAndroidInjector abstract fun contributesSmsCommunicatorOtpActivity(): SmsCommunicatorOtpActivity
    @ContributesAndroidInjector abstract fun contributesStatsActivity(): StatsActivity
    @ContributesAndroidInjector abstract fun contributesSurveyActivity(): SurveyActivity
    @ContributesAndroidInjector abstract fun contributesTDDStatsActivity(): TDDStatsActivity
}