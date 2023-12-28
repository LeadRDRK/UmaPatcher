package com.leadrdrk.umapatcher.unity.file

import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import java.io.File

abstract class UnityFile(
    val path: String,
    flags: UInt
): AutoCloseable {
    val name: String = File(path).name
    var flags: UInt = flags
        protected set

    open fun getAssetsFile(): SerializedFile? {
        return null
    }

    abstract fun save(writer: BinaryWriter)
}