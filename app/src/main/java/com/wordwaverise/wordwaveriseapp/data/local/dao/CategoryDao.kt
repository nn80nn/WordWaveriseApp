package com.wordwaverise.wordwaveriseapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.wordwaverise.wordwaveriseapp.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE serverId IS NULL AND name = :name LIMIT 1")
    suspend fun getUnlinkedByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Query(
        """
        UPDATE categories
        SET serverId = :serverId, name = :name, color = :color,
            groupServerId = :groupServerId, groupName = :groupName, readOnly = :readOnly
        WHERE id = :id
        """
    )
    suspend fun linkToServer(
        id: Long,
        serverId: Int,
        name: String,
        color: String?,
        groupServerId: Int? = null,
        groupName: String? = null,
        readOnly: Boolean = false
    )

    /**
     * Repairs rows left behind by the old sync, which inserted a fresh local row for
     * every server category on every run. Words are moved onto the surviving (oldest)
     * row before the copies are dropped, so no word loses its category.
     */
    @Query(
        """
        UPDATE saved_words SET categoryId = (
            SELECT MIN(c.id) FROM categories c
            WHERE c.serverId = (SELECT o.serverId FROM categories o WHERE o.id = saved_words.categoryId)
        )
        WHERE categoryId IN (SELECT id FROM categories WHERE serverId IS NOT NULL)
        """
    )
    suspend fun remapWordsToOldestDuplicate()

    @Query(
        """
        DELETE FROM categories WHERE serverId IS NOT NULL AND id NOT IN (
            SELECT MIN(id) FROM categories WHERE serverId IS NOT NULL GROUP BY serverId
        )
        """
    )
    suspend fun deleteDuplicateServerCategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    /** Папки, удалённые на другом устройстве. Созданные офлайн (`serverId IS NULL`) остаются. */
    @Query("DELETE FROM categories WHERE serverId IS NOT NULL AND serverId NOT IN (:serverIds)")
    suspend fun deleteMissingFromServer(serverIds: List<Int>)

    @Query("DELETE FROM categories WHERE serverId IS NOT NULL")
    suspend fun deleteAllSynced()

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE saved_words SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun unassignWords(categoryId: Long)
}
