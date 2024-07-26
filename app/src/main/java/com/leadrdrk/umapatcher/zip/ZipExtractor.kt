package com.leadrdrk.umapatcher.zip

import android.os.Build
import net.lingala.zip4j.model.AbstractFileHeader
import java.io.File

private const val MAX_MAPPED_SIZE = 104857600 // 100MiB

abstract class ZipExtractor(val file: File): AutoCloseable {
    abstract suspend fun extractAll(dir: File, progressCallback: (Float) -> Unit)

    abstract val fileHeaders: List<AbstractFileHeader>

    companion object {
        fun maybeMapped(file: File): ZipExtractor {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && file.length() <= MAX_MAPPED_SIZE) {
                MappedZipFileExtractor(file)
            } else {
                ZipFileExtractor(file)
            }
        }
    }
}