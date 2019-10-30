package info.nightscout.androidaps.database.daos

import info.nightscout.androidaps.database.entities.links.APSResultLink

class APSResultLinkDaoTest : AbstractDaoTest<APSResultLink>() {

    override fun copy(entry: APSResultLink) = entry.copy()

    override fun getDao() = database.apsResultLinkDao

    override fun generateTestEntry() = APSResultLink(
           apsResultId = 1
    )

}