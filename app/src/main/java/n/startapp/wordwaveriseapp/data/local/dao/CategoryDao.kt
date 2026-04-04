package n.startapp.wordwaveriseapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import n.startapp.wordwaveriseapp.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE saved_words SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun unassignWords(categoryId: Long)
}
