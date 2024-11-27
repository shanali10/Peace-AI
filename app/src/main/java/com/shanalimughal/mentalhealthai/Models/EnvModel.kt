package com.shanalimughal.mentalhealthai.Models

class EnvModel(
    var envPlaceHolder: Int = 0,
    var envTitle: String = "",
    var envInterestedAreas: List<String> = emptyList(),
    var envImportance: String,
    var envPreferredInfo: String = ""
) {
    // Empty constructor
    constructor() : this(0, "", emptyList(), "", "")
}