package com.shanalimughal.mentalhealthai.Models

class PostModel(
    var id: String = "",
    var postId: String = "",
    var userName: String = "",
    var userProfileUrl: String = "cant be empty",
    val postDescription: String = "",
    val postTime: String = "",
    val postLikes: Int = 0,
    val postComments: Int = 0
) {
    // Constructor with postId
    constructor(
        id: String,
        userName: String,
        userProfileUrl: String,
        postDescription: String,
        postTime: String,
        postLikes: Int,
        postComments: Int
    ) : this(id, "", userName, userProfileUrl, postDescription, postTime, postLikes, postComments)

    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "cant be empty", "", "", 0, 0)
}
