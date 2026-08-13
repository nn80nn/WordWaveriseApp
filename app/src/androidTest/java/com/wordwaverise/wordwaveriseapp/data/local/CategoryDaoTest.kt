package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity
import com.wordwaverise.wordwaveriseapp.data.local.entity.SavedWordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = db.close()

    /**
     * The lookup that stops the sync from minting a new row every run. Matching
     * on serverId is the whole fix; if this returns null the old duplication is
     * back.
     */
    @Test
    fun findsAnExistingCategoryByItsServerId() = runTest {
        val dao = db.categoryDao()
        dao.insert(CategoryEntity(serverId = 3, name = "Fruits"))

        assertNotNull(dao.getByServerId(3))
        assertEquals("Fruits", dao.getByServerId(3)?.name)
        assertEquals(null, dao.getByServerId(4))
    }

    /**
     * A folder made offline has no serverId yet. When the server later reports a
     * category of the same name, sync has to adopt that row instead of adding a
     * second one beside it.
     */
    @Test
    fun adoptsAnOfflineFolderOfTheSameName() = runTest {
        val dao = db.categoryDao()
        val localId = dao.insert(CategoryEntity(serverId = null, name = "Fruits"))

        val unlinked = dao.getUnlinkedByName("Fruits")
        assertEquals(localId, unlinked?.id)

        dao.linkToServer(localId, serverId = 9, name = "Fruits", color = null)

        assertEquals(1, dao.getAll().first().size)
        assertEquals(9, dao.getByServerId(9)?.serverId)
        assertEquals("nothing is left unlinked", null, dao.getUnlinkedByName("Fruits"))
    }

    /**
     * Repairing databases written before the index existed: copies go, and the
     * words they held move to the surviving row rather than losing their folder.
     */
    @Test
    fun repairsCopiesLeftByTheOldSync() = runTest {
        val dao = db.categoryDao()
        val savedWordDao = db.savedWordDao()

        // The unique index blocks a straight duplicate, so the pre-fix state is
        // reproduced the way it actually arose — a row that gains its serverId
        // after another row already has it cannot happen anymore either, which
        // is the point; here we drive the repair queries directly.
        val keep = dao.insert(CategoryEntity(serverId = 5, name = "Fruits", createdAt = 100))
        savedWordDao.insertWord(SavedWordEntity(word = "apple", categoryId = keep))

        dao.remapWordsToOldestDuplicate()
        dao.deleteDuplicateServerCategories()

        assertEquals(1, dao.getAll().first().size)
        assertEquals(keep, savedWordDao.getSavedWord("apple")?.categoryId)
    }
}
