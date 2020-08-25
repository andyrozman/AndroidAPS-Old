package info.nightscout.androidaps.plugins.pump.omnipod.dialogs

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import info.nightscout.androidaps.db.OmnipodHistoryRecord
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper
import javax.inject.Inject

class OmnipodHistoryRecordsAdapter @Inject constructor() : PagedListAdapter<OmnipodHistoryRecord, OmnipodHistoryRecordViewHolder>(DIFF_CALLBACK) {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var omnipodUtil: OmnipodUtil
    @Inject lateinit var resourceHelper: ResourceHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OmnipodHistoryRecordViewHolder {
        return OmnipodHistoryRecordViewHolder.create(parent ,aapsLogger, omnipodUtil, resourceHelper)
    }

    override fun onBindViewHolder(holder: OmnipodHistoryRecordViewHolder, position: Int) {
        val record: OmnipodHistoryRecord? = getItem(position)
        holder.bind(record)
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OmnipodHistoryRecord>() {

            override fun areItemsTheSame(oldItem: OmnipodHistoryRecord, newItem: OmnipodHistoryRecord) =
                oldItem.compareTo(newItem) == 0

            override fun areContentsTheSame(oldItem: OmnipodHistoryRecord, newItem: OmnipodHistoryRecord) =
                areItemsTheSame(oldItem,newItem)

        }

    }

}