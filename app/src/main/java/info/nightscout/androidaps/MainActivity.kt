package info.nightscout.androidaps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.PersistableBundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joanzapata.iconify.Iconify
import com.joanzapata.iconify.fonts.FontAwesomeModule
import info.nightscout.androidaps.activities.NoSplashAppCompatActivity
import info.nightscout.androidaps.activities.PreferencesActivity
import info.nightscout.androidaps.activities.SingleFragmentActivity
import info.nightscout.androidaps.activities.StatsActivity
import info.nightscout.androidaps.events.EventAppExit
import info.nightscout.androidaps.events.EventPreferenceChange
import info.nightscout.androidaps.events.EventRebuildTabs
import info.nightscout.androidaps.historyBrowser.HistoryBrowseActivity
import info.nightscout.androidaps.interfaces.ActivePluginProvider
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerUtils
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSettingsStatus
import info.nightscout.androidaps.plugins.general.smsCommunicator.SmsCommunicatorPlugin
import info.nightscout.androidaps.setupwizard.SetupWizardActivity
import info.nightscout.androidaps.utils.tabs.TabPageAdapter
import info.nightscout.androidaps.utils.AndroidPermission
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.LocaleHelper
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.buildHelper.BuildHelper
import info.nightscout.androidaps.utils.extensions.isRunningRealPumpTest
import info.nightscout.androidaps.utils.protection.ProtectionCheck
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject
import kotlin.system.exitProcess

class MainActivity : NoSplashAppCompatActivity() {
    private val disposable = CompositeDisposable()

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var rxBus: RxBusWrapper
    @Inject lateinit var androidPermission: AndroidPermission
    @Inject lateinit var sp: SP
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var versionCheckerUtils: VersionCheckerUtils
    @Inject lateinit var smsCommunicatorPlugin: SmsCommunicatorPlugin
    @Inject lateinit var loopPlugin: LoopPlugin
    @Inject lateinit var nsSettingsStatus: NSSettingsStatus
    @Inject lateinit var buildHelper: BuildHelper
    @Inject lateinit var activePlugin: ActivePluginProvider
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var protectionCheck: ProtectionCheck

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var pluginPreferencesMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Iconify.with(FontAwesomeModule())
        LocaleHelper.update(applicationContext)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, main_drawer_layout, R.string.open_navigation, R.string.close_navigation).also {
            main_drawer_layout.addDrawerListener(it)
            it.syncState()
        }

        // initialize screen wake lock
        processPreferenceChange(EventPreferenceChange(resourceHelper.gs(R.string.key_keep_screen_on)))
        main_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                checkPluginPreferences(main_pager)
            }
        })

        //Check here if loop plugin is disabled. Else check via constraints
        if (!loopPlugin.isEnabled(PluginType.LOOP)) versionCheckerUtils.triggerCheckVersion()
        fabricPrivacy.setUserStats()
        setupViews()
        disposable.add(rxBus
            .toObservable(EventRebuildTabs::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.recreate) recreate()
                else setupViews()
                setWakeLock()
            }) { fabricPrivacy::logException }
        )
        disposable.add(rxBus
            .toObservable(EventPreferenceChange::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ processPreferenceChange(it) }) { fabricPrivacy::logException }
        )
        if (!sp.getBoolean(R.string.key_setupwizard_processed, false) && !isRunningRealPumpTest()) {
            val intent = Intent(this, SetupWizardActivity::class.java)
            startActivity(intent)
        }
        androidPermission.notifyForStoragePermission(this)
        androidPermission.notifyForBatteryOptimizationPermission(this)
        if (Config.PUMPDRIVERS) {
            androidPermission.notifyForLocationPermissions(this)
            androidPermission.notifyForSMSPermissions(this, smsCommunicatorPlugin)
            androidPermission.notifyForSystemWindowPermissions(this)
        }
    }

    private fun checkPluginPreferences(viewPager: ViewPager2) {
        pluginPreferencesMenuItem?.isEnabled = (viewPager.adapter as TabPageAdapter).getPluginAt(viewPager.currentItem).preferencesId != -1
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        actionBarDrawerToggle.syncState()
    }

    public override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    override fun onResume() {
        super.onResume()
        protectionCheck.queryProtection(this, ProtectionCheck.Protection.APPLICATION, null,
            Runnable {
                OKDialog.show(this, "", resourceHelper.gs(R.string.authorizationfailed), Runnable { finish() })
            },
            Runnable {
                OKDialog.show(this, "", resourceHelper.gs(R.string.authorizationfailed), Runnable { finish() })
            })
    }

    private fun setWakeLock() {
        val keepScreenOn = sp.getBoolean(R.string.key_keep_screen_on, false)
        if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun processPreferenceChange(ev: EventPreferenceChange) {
        if (ev.isChanged(resourceHelper, R.string.key_keep_screen_on)) setWakeLock()
    }

    private fun setupViews() {
        // Menu
        val pageAdapter = TabPageAdapter(this)
        main_navigation_view.setNavigationItemSelectedListener { true }
        val menu = main_navigation_view.menu.also { it.clear() }
        for (p in activePlugin.pluginsList) {
            pageAdapter.registerNewFragment(p)
            if (p.isEnabled() && p.hasFragment() && !p.isFragmentVisible() && !p.pluginDescription.neverVisible) {
                val menuItem = menu.add(p.name)
                menuItem.isCheckable = true
                menuItem.setOnMenuItemClickListener {
                    val intent = Intent(this, SingleFragmentActivity::class.java)
                    intent.putExtra("plugin", activePlugin.pluginsList.indexOf(p))
                    startActivity(intent)
                    main_drawer_layout.closeDrawers()
                    true
                }
            }
        }
        main_pager.adapter = pageAdapter
        checkPluginPreferences(main_pager)

        // Tabs
        if (sp.getBoolean(R.string.key_short_tabtitles, false)) {
            tabs_normal.visibility = View.GONE
            tabs_compact.visibility = View.VISIBLE
            toolbar.layoutParams = LinearLayout.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, resources.getDimension(R.dimen.compact_height).toInt())
            TabLayoutMediator(tabs_compact, main_pager) { tab, position ->
                tab.text = (main_pager.adapter as TabPageAdapter).getPluginAt(position).nameShort
            }.attach()
        } else {
            tabs_normal.visibility = View.VISIBLE
            tabs_compact.visibility = View.GONE
            val typedValue = TypedValue()
            if (theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
                toolbar.layoutParams = LinearLayout.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT,
                    TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics))
            }
            TabLayoutMediator(tabs_normal, main_pager) { tab, position ->
                tab.text = (main_pager.adapter as TabPageAdapter).getPluginAt(position).name
            }.attach()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty()) {
            if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
                when (requestCode) {
                    AndroidPermission.CASE_STORAGE                                                                                                                                        ->                         //show dialog after permission is granted
                        OKDialog.show(this, "", resourceHelper.gs(R.string.alert_dialog_storage_permission_text))

                    AndroidPermission.CASE_LOCATION, AndroidPermission.CASE_SMS, AndroidPermission.CASE_BATTERY, AndroidPermission.CASE_PHONE_STATE, AndroidPermission.CASE_SYSTEM_WINDOW -> {
                    }
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        pluginPreferencesMenuItem = menu.findItem(R.id.nav_plugin_preferences)
        checkPluginPreferences(main_pager)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_preferences        -> {
                protectionCheck.queryProtection(this, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(this, PreferencesActivity::class.java)
                    i.putExtra("id", -1)
                    startActivity(i)
                })
                return true
            }

            R.id.nav_historybrowser     -> {
                startActivity(Intent(this, HistoryBrowseActivity::class.java))
                return true
            }

            R.id.nav_setupwizard        -> {
                startActivity(Intent(this, SetupWizardActivity::class.java))
                return true
            }

            R.id.nav_about              -> {
                var message = "Build: ${BuildConfig.BUILDVERSION}\n"
                message += "Flavor: ${BuildConfig.FLAVOR}${BuildConfig.BUILD_TYPE}\n"
                message += "${resourceHelper.gs(R.string.configbuilder_nightscoutversion_label)} ${nsSettingsStatus.nightscoutVersionName}"
                if (buildHelper.isEngineeringMode()) message += "\n${resourceHelper.gs(R.string.engineering_mode_enabled)}"
                message += resourceHelper.gs(R.string.about_link_urls)
                val messageSpanned = SpannableString(message)
                Linkify.addLinks(messageSpanned, Linkify.WEB_URLS)
                AlertDialog.Builder(this)
                    .setTitle(resourceHelper.gs(R.string.app_name) + " " + BuildConfig.VERSION)
                    .setIcon(resourceHelper.getIcon())
                    .setMessage(messageSpanned)
                    .setPositiveButton(resourceHelper.gs(R.string.ok), null)
                    .create().also {
                        it.show()
                        (it.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    }
                return true
            }

            R.id.nav_exit               -> {
                aapsLogger.debug(LTag.CORE, "Exiting")
                rxBus.send(EventAppExit())
                finish()
                System.runFinalization()
                exitProcess(0)
            }

            R.id.nav_plugin_preferences -> {
                val plugin = (main_pager.adapter as TabPageAdapter).getPluginAt(main_pager.currentItem)
                protectionCheck.queryProtection(this, ProtectionCheck.Protection.PREFERENCES, Runnable {
                    val i = Intent(this, PreferencesActivity::class.java)
                    i.putExtra("id", plugin.preferencesId)
                    startActivity(i)
                })
                return true
            }
/*
            R.id.nav_survey             -> {
                startActivity(Intent(this, SurveyActivity::class.java))
                return true
            }
*/
            R.id.nav_stats              -> {
                startActivity(Intent(this, StatsActivity::class.java))
                return true
            }
        }
        return actionBarDrawerToggle.onOptionsItemSelected(item)
    }
}