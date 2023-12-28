package com.leadrdrk.umapatcher.data

@Suppress("ArrayInDataClass")
data class TextBlock(
    val jpText: String?,
    val enText: String?,
    val jpName: String?,
    val enName: String?,
    val blockIdx: Int?,
    val time: String?,
    val nextBlock: Int?,
    val voiceIdx: Int?,
    val pathId: Long?,
    val origClipLength: Int?,
    val newClipLength: Int?,
    val animData: Array<AnimGroup>?,
    val choices: Array<TextBlock>?,
    val coloredText: Array<TextBlock>?
)