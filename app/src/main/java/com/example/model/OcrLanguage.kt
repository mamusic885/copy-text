package com.example.model

enum class OcrLanguage(
    val displayName: String,
    val scriptDescription: String,
    val code: String
) {
    AUTO(
        displayName = "Auto Detect",
        scriptDescription = "English, Hindi, Punjabi",
        code = "auto"
    ),
    ENGLISH(
        displayName = "English",
        scriptDescription = "Latin Script",
        code = "en"
    ),
    HINDI(
        displayName = "Hindi (हिन्दी)",
        scriptDescription = "Devanagari Script",
        code = "hi"
    ),
    PUNJABI(
        displayName = "Punjabi (ਪੰਜਾਬੀ)",
        scriptDescription = "Gurmukhi Script",
        code = "pa"
    )
}

data class ScanItem(
    val id: String,
    val imageUri: String,
    val extractedText: String,
    val timestamp: Long,
    val language: OcrLanguage
)
