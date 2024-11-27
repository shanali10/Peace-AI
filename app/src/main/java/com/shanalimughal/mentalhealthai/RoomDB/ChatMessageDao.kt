package com.shanalimughal.mentalhealthai.RoomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM mental_health_chat ORDER BY id DESC")
    suspend fun getAllMessages(): List<ChatMessage>

    @Insert
    suspend fun insertMessage(chatMessage: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chatMessages: List<ChatMessage>)

    @Query("DELETE FROM mental_health_chat")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM mental_health_chat WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)
}
