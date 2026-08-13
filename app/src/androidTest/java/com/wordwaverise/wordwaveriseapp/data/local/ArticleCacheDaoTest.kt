package com.wordwaverise.wordwaveriseapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordwaverise.wordwaveriseapp.data.local.entity.ArticleCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleCacheDaoTest {

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

    @Test
    fun storesAndReadsBackAnArticle() = runTest {
        val dao = db.articleCacheDao()
        dao.put(ArticleCacheEntity(key = "resilient", payload = """{"annotationStatus":"READY"}"""))

        assertEquals("""{"annotationStatus":"READY"}""", dao.get("resilient")?.payload)
        assertNull("an unknown word must miss, not return something else", dao.get("eloquent"))
    }

    @Test
    fun rewritingAKeyReplacesItRatherThanPilingUp() = runTest {
        val dao = db.articleCacheDao()
        dao.put(ArticleCacheEntity(key = "resilient", payload = "first"))
        dao.put(ArticleCacheEntity(key = "resilient", payload = "second"))

        assertEquals(1, dao.count())
        assertEquals("second", dao.get("resilient")?.payload)
    }
}
