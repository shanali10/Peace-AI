package com.shanalimughal.mentalhealthai.Models

class ChatbotModel(
    val id: Long = 0, // Add this line
    val userPrompt: String = "",
    val response: String = ""
) {
    constructor() : this(0, "", "")
}
