package com.shanalimughal.mentalhealthai.Models

class MeditationModel(
    var meditationPlaceHolder: Int = 0,
    var meditationTitle: String = "",
    var meditationGoals: List<String> = emptyList(),
    var hoursOfSleep: String,
    var meditationDuration: String = "",
    var meditationDescription: String = ""
) {
    // Empty constructor
    constructor() : this(0, "", emptyList(), "", "", "")
}
