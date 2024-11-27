package com.shanalimughal.mentalhealthai.Models

data class UserPreferencesOnBoradModel(
    val profession: String,
    val mood: String,
    val goals: List<String>,
    val sleepHours: Int,
    val exerciseFrequency: String
)
