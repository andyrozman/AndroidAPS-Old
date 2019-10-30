package info.nightscout.androidaps.database.daos

import info.nightscout.androidaps.database.entities.Bolus

class BolusDaoTest : AbstractDaoTest<Bolus>() {

    override fun copy(entry: Bolus) = entry.copy()

    override fun getDao() = database.bolusDao

    override fun generateTestEntry() = Bolus(
            timestamp = 0,
            utcOffset = 0,
            amount = 5.0,
            isBasalInsulin = false,
            type = Bolus.Type.NORMAL
    )

}