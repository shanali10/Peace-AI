package com.shanalimughal.mentalhealthai.Models

class TherapySummaryModel(
    var id: String = "",
    val summary: String = "",
    val date: String = ""
) {
    constructor(
        summary: String,
        date: String
    ) : this("", summary, date)

    constructor() : this("", "", "")
}