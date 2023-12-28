package com.leadrdrk.umapatcher.unity.stream

import java.io.InputStream

abstract class SeekableBinaryReader(
    stream: InputStream
) : BinaryReader(stream) {
    abstract var position: Long
    abstract val size: Long

    fun align(alignment: Int = 4) {
        val mod = position % alignment
        if (mod != 0L) position += alignment - mod
    }

    fun readAlignedString(): String {
        val length = readInt32()
        if (length > 0 && length <= size - position) {
            val stringData = readBytes(length)
            align()
            return String(stringData, Charsets.UTF_8)
        }
        return ""
    }
}