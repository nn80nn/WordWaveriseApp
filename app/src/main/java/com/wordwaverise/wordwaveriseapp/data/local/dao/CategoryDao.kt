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
            parentServerId = :parentServerId, bookServerId = :bookServerId,
            groupServerId = :groupServerId, groupName = :groupName, readOnly = :readOnly
        WHERE id = :id
        """
    )
    suspend fun linkToServer(
        id: Long,
        serverId: Int,
        name: String,
        color: String?,
        parentServerId: Int? = null,
        bookServerId: Int? = null,
        groupServerId: Int? = null,
        groupName: String? = null,
        readOnly: Boolean = false
    )

    /**
     * Вложенность переписывается на каждой синхронизации вместе со всем остальным
     * ([linkToServer]), но перемещение надо показать сразу — ждать следующего прохода
     * значит показывать человеку папку там, откуда он её только что убрал.
     */
    @Query("UPDATE categories SET parentServerId = :parentServerId WHERE id = :id")
    suspend fun setParent(id: Long, parentServerId: Int?)

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

}
