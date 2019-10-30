package info.nightscout.androidaps.database.daos

import info.nightscout.androidaps.database.entities.ExtendedBolus
import java.util.concurrent.TimeUnit

class ExtendedBolusDaoTest : AbstractDaoTest<ExtendedBolus>() {

    override fun copy(entry: ExtendedBolus) = entry.copy()

    override fun getDao() = database.extendedBolusDao

    override fun generateTestEntry() = ExtendedBolus(
            timestamp = 0,
            utcOffset = 0,
            amount = 0.0,
            duration = TimeUnit.HOURS.toMillis(1),
            isEmulatingTempBasal = false
    )

}