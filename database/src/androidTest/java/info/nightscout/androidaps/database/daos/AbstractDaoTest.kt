package info.nightscout.androidaps.database.daos

import info.nightscout.androidaps.database.AbstractDatabaseTest
import info.nightscout.androidaps.database.interfaces.DBEntry
import org.junit.Assert
import org.junit.Test

abstract class AbstractDaoTest<T : DBEntry> : AbstractDatabaseTest() {

    abstract fun generateTestEntry() : T

    abstract fun getDao() : BaseDao<T>

    abstract fun copy(entry: T): T

    @Test(expected = IllegalArgumentException::class)
    fun testInsertIDCheck() {
        val entry = generateTestEntry()
        entry.id = 1
        entry.version = 0
        entry.referenceId = 0
        getDao().insertNewEntry(entry)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInsertVersionCheck() {
        val entry = generateTestEntry()
        entry.id = 0
        entry.version = 1
        entry.referenceId = 0
        getDao().insertNewEntry(entry)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInsertReferenceIDCheck() {
        val entry = generateTestEntry()
        entry.id = 0
        entry.referenceId = 1
        entry.version = 0
        getDao().insertNewEntry(entry)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUpdateIDCheck() {
        val entry = generateTestEntry()
        entry.id = 0
        entry.referenceId = 0
        entry.version = 0
        getDao().updateExistingEntry(entry)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUpdateReferenceIDCheck() {
        val entry = generateTestEntry()
        entry.id = 1
        entry.referenceId = 1
        entry.version = 0
        getDao().updateExistingEntry(entry)
    }

    @Test
    fun testInsertUpdate() {
        val entry = generateTestEntry()
        val insertGeneratedId = getDao().insertNewEntry(entry)
        Assert.assertEquals("Generated ID wrong", 1, insertGeneratedId)
        Assert.assertEquals("Wrong ID in entry", 1, entry.id)
        val queried = getDao().findById(insertGeneratedId)
        Assert.assertEquals("Did not get the same entry back from the database", queried, entry)
        Assert.assertNotEquals("dateCreated not updated", 0, entry.dateCreated)
        val beforeUpdate = copy(entry)
        val generatedId = getDao().updateExistingEntry(entry)
        Assert.assertEquals("Version number wrong after update", 1, entry.version)
        val historicEntry = getDao().findById(generatedId)
        Assert.assertNotNull("Historic entry not found", historicEntry)
        Assert.assertEquals("Version number of historic entry wrong", 0, historicEntry!!.version)
        historicEntry.id = beforeUpdate.id
        historicEntry.referenceId = null
        Assert.assertEquals("Historic entry does not equal to old value.", beforeUpdate, historicEntry)
    }

}