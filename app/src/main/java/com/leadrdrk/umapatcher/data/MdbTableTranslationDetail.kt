package com.leadrdrk.umapatcher.data

data class MdbTableTranslationDetail (
    val name: String,
    val textColumn: String,
    val files: List<String>,
    val subdir: Boolean
)

