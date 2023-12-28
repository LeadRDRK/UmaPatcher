package com.leadrdrk.umapatcher.data

@Suppress("ArrayInDataClass")
data class AssetsTranslationFile(
    override val version: Int,
    override val type: String,
    val bundle: String,
    val storyId: String,
    val title: String,
    val text: Array<TextBlock>,
    val modified: Long,
    val humanTl: Boolean?
) : TranslationFile()