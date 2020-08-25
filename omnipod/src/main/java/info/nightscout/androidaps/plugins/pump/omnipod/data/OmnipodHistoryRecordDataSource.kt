package info.nightscout.androidaps.plugins.pump.omnipod.data

import androidx.paging.ItemKeyedDataSource
import info.nightscout.androidaps.db.OmnipodHistoryRecord
import info.nightscout.androidaps.interfaces.DatabaseHelperInterface
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import java.lang.Exception

class OmnipodHistoryRecordDataSource(val databaseHelper: DatabaseHelperInterface, val aapsLogger: AAPSLogger) : ItemKeyedDataSource<Long, OmnipodHistoryRecord>() {

    override fun getKey(item: OmnipodHistoryRecord): Long {
        return item.date
    }

    override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<OmnipodHistoryRecord>) {
        aapsLogger.info(LTag.PUMP,"Fetching initial data")
        try {
            val results = databaseHelper.getFilteredOmnipodHistoryRecords(
                params.requestedInitialKey ?: 0,
                params.requestedLoadSize.toLong(),
                null,
                false)
            aapsLogger.info(LTag.PUMP, "Got %d results", results.size)
            callback.onResult(results)
        } catch (e : Exception) {
            aapsLogger.error(LTag.PUMP,e.toString())
        }
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<OmnipodHistoryRecord>) {
        val results = databaseHelper.getFilteredOmnipodHistoryRecords(
            params.key,
            params.requestedLoadSize.toLong(),
            null,
            false)
        callback.onResult(results)
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<OmnipodHistoryRecord>) {
        val results = databaseHelper.getFilteredOmnipodHistoryRecords(
            params.key,
            params.requestedLoadSize.toLong(),
            null,
            true)
        callback.onResult(results)
    }

}