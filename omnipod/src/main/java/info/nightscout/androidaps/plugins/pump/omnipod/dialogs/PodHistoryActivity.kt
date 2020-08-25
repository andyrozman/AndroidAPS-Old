package info.nightscout.androidaps.plugins.pump.omnipod.dialogs

import android.os.Bundle
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.nightscout.androidaps.activities.NoSplashAppCompatActivity
import info.nightscout.androidaps.db.OmnipodHistoryRecord
import info.nightscout.androidaps.interfaces.DatabaseHelperInterface
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.omnipod.data.OmnipodHistoryRecordDataSource
import info.nightscout.androidaps.plugins.pump.omnipod.defs.state.PodStateManager
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil
import info.nightscout.androidaps.plugins.pump.omnipod.R
import info.nightscout.androidaps.utils.resources.ResourceHelper
import javax.inject.Inject

class PodHistoryActivity : NoSplashAppCompatActivity() {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var omnipodUtil: OmnipodUtil
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var databaseHelper: DatabaseHelperInterface
    @Inject lateinit var podStateManager: PodStateManager
    @Inject lateinit var omnipodHistoryRecordsAdapter: OmnipodHistoryRecordsAdapter

    private var historyTypeSpinner: Spinner? = null
    //private var statusView: TextView? = null
    //private var recyclerView: RecyclerView? = null
    //private var linearLayoutManager: LinearLayoutManager? = null

    var showingType: OldPodHistoryActivity.TypeList? = null
    var selectedGroup = PumpHistoryEntryGroup.All
    var fullHistoryList: List<OmnipodHistoryRecord> = ArrayList()
    var filteredHistoryList: List<OmnipodHistoryRecord> = ArrayList()

    var manualChange = false

    var typeListFull: List<OldPodHistoryActivity.TypeList>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.omnipod_pod_history_activity)
        historyTypeSpinner = findViewById(R.id.omnipod_historytype)
        val statusView : TextView = findViewById(R.id.omnipod_historystatus)
        val recyclerView : RecyclerView = findViewById(R.id.omnipod_history_recyclerview)
        recyclerView.setHasFixedSize(true)
        //linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        //recyclerViewAdapter = OldPodHistoryActivity.RecyclerViewAdapter(filteredHistoryList)
        recyclerView.adapter = omnipodHistoryRecordsAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,DividerItemDecoration.VERTICAL))
        statusView.visibility = View.GONE

        val dataSourceFactory = object : DataSource.Factory<Long,OmnipodHistoryRecord>() {
            override fun create(): DataSource<Long, OmnipodHistoryRecord> {
                return OmnipodHistoryRecordDataSource(databaseHelper,aapsLogger)
            }
        }

        val records = LivePagedListBuilder(dataSourceFactory, PagedList.Config.Builder().setPageSize(30).build()).build()

        records.observe(this, Observer<PagedList<OmnipodHistoryRecord>>{ pagedList -> omnipodHistoryRecordsAdapter.submitList(pagedList) } )

        // typeListFull = getTypeList(PumpHistoryEntryGroup.getTranslatedList(resourceHelper))
        // // val spinnerAdapter = ArrayAdapter(this, R.layout.spinner_centered, typeListFull)
        // historyTypeSpinner.setAdapter(spinnerAdapter)
        // historyTypeSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
        //     override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        //         if (manualChange) return
        //         val selected = historyTypeSpinner.getSelectedItem() as OldPodHistoryActivity.TypeList
        //         OldPodHistoryActivity.showingType = selected
        //         OldPodHistoryActivity.selectedGroup = selected.entryGroup
        //         filterHistory(OldPodHistoryActivity.selectedGroup)
        //     }
        //
        //     override fun onNothingSelected(parent: AdapterView<*>?) {
        //         if (manualChange) return
        //         filterHistory(PumpHistoryEntryGroup.All)
        //     }
        // })
    }

}