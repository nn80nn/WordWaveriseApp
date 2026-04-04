package n.startapp.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey
    val word: String,
    val savedAt: Long = System.currentTimeMillis(),
    val serverId: Int? = null,
    val isSynced: Boolean = false,
    val categoryId: Long? = null
)
