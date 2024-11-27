package com.shanalimughal.mentalhealthai.Models

class CommentsModel(
    val postId: String = "",
    val userId: String = "",
    var userName: String = "",
    var profileUrl: String = "cant be empty",
    val commentTitle: String = "",
    val commentTime: String = ""
) {
    // Constructor with postId
    constructor(
        userId: String,
        userName: String,
        profileUrl: String,
        commentTitle: String,
        commentTime: String
    ) : this("", userId, userName, profileUrl, commentTitle, commentTime)

    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "cant be empty", "", "")
}