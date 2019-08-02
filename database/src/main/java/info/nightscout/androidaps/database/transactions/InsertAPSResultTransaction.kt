package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.entities.APSResult
import java.util.*

class InsertAPSResultTransaction(
        val timestamp: Long,
        val algorithm: APSResult.Algorithm,
        val glucoseStatusJson: String,
        val currentTempJson: String,
        val iobDataJson: String,
        val profileJson: String,
        val autosensDataJson: String?,
        val mealDataJson: String,
        val microBolusAllowed: Boolean?,
        val resultJson: String
) : Transaction<Unit>() {

    override fun run() {
        database.apsResultDao.insertNewEntry(APSResult(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                algorithm = algorithm,
                glucoseStatusJson = glucoseStatusJson,
                currentTempJson = currentTempJson,
                iobDataJson = iobDataJson,
                profileJson = profileJson,
                autosensDataJson = autosensDataJson,
                mealDataJson = mealDataJson,
                microBolusAllowed = microBolusAllowed,
                resultJson = resultJson
        ).apply {
            changes.add(this)
        })
    }

}