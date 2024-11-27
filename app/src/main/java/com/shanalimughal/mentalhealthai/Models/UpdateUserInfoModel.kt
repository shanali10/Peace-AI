package com.shanalimughal.mentalhealthai.Models

data class UpdateUserInfoModel(
    val userName: String,
    val profession: String,
    val mood: String,
    val goals: List<String>,
    val sleepHours: Int,
    val exerciseFrequency: String
)
