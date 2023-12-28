package com.leadrdrk.umapatcher.unity.file

import com.leadrdrk.umapatcher.unity.stream.BinaryWriter

class ResourceFile(
    val buffer: ByteArray,
    path: String,
    flags: UInt
): UnityFile(path, flags) {
    override fun save(writer: BinaryWriter) {
        writer.write(buffer)
    }

    override fun close() {}
}