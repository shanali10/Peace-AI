package com.shanalimughal.mentalhealthai.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mental_health_chat")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val response: String
) {
    // No-argument constructor
    constructor() : this(0, "", "")
}