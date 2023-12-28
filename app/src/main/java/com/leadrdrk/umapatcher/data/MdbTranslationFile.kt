package com.leadrdrk.umapatcher.data

data class MdbTranslationFile(
    override val version: Int,
    override val type: String,
    val lineLength: Int?,
    val textSize: Int?,
    val text: Map<String, String>
) : TranslationFile()