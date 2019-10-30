package info.nightscout.androidaps.database.daos

import info.nightscout.androidaps.database.entities.TemporaryBasal
import java.util.concurrent.TimeUnit

class TemporaryBasalDaoTest : AbstractDaoTest<TemporaryBasal>() {

    override fun copy(entry: TemporaryBasal) = entry.copy()

    override fun getDao() = database.temporaryBasalDao

    override fun generateTestEntry() = TemporaryBasal(
            timestamp = 0,
            utcOffset = 0,
            isAbsolute = false,
            duration = TimeUnit.HOURS.toMillis(1),
            rate = 120.0,
            type = TemporaryBasal.Type.NORMAL
    )

}