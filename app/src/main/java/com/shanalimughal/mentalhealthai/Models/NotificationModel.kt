package com.shanalimughal.mentalhealthai.Models

class NotificationModel(
    var postId: String = "",
    var userId: String = "",
    var userProfileUrl: String = "cant be empty",
    var notificationDescription: String = "",
    val notificationTime: String = ""
) {
    constructor(
        userId: String,
        userProfileUrl: String,
        notificationDescription: String,
        notificationTime: String
    ) : this("", userId, userProfileUrl, notificationDescription, notificationTime)

    constructor() : this("", "", "cant be empty", "", "")
}