package com.shanalimughal.mentalhealthai.Models

class AffirmationModel(
    var affirmationPlaceholder: Int = 0,
    var affirmationTitle: String = "",
    var affirmationTheme: List<String> = emptyList(),
    var preferredAffirmation: String = "",
) {
    // Empty constructor
    constructor() : this(0, "", emptyList(), "")
}