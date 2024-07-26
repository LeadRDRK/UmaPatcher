package com.leadrdrk.umapatcher.zip

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.LocalFileHeader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.O)
class MappedZipFileExtractor(
    file: File
): ZipExtractor(file) {
    // Peek total uncompressed size with normal ZipFile
    override val fileHeaders: List<FileHeader> = ZipFile(file).use { it.fileHeaders }
    private val totalSize = run {
        var value = 0L
        fileHeaders.forEach { value += it.uncompressedSize }
        value.toFloat()
    }
    private val fileChannel = FileChannel.open(file.toPath())
    private val buffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size())
    private val inputStream = ByteBufferInputStream(buffer)
    private val zip = ZipInputStream(inputStream)

    override suspend fun extractAll(dir: File, progressCallback: (Float) -> Unit) {
        var localFileHeader: LocalFileHeader?
        var readLen: Int
        val readBuffer = ByteArray(524288)
        var totalReadLen = 0L
        while ((zip.nextEntry.also { localFileHeader = it }) != null) {
            val extractedFile = dir.resolve(localFileHeader!!.fileName)
            val areRelated =
                extractedFile.canonicalPath.startsWith(dir.canonicalPath + File.separator)
            if (!areRelated) {
                Log.w("UmaPatcher", "File path sanitized: $extractedFile")
                continue
            }
            val extractDir = extractedFile.parentFile!!
            if (!extractDir.exists()) extractDir.mkdirs()

            extractedFile.outputStream().use { output ->
                while (zip.read(readBuffer).also { readLen = it } != -1) {
                    output.write(readBuffer, 0, readLen)
                    totalReadLen += readLen
                    progressCallback(totalReadLen / totalSize)
                }
            }
        }
    }

    override fun close() {
        zip.close()
        inputStream.close()
        fileChannel.close()
    }

    class ByteBufferInputStream(private val buf: ByteBuffer) : InputStream() {
        @Throws(IOException::class)
        override fun read(): Int {
            if (!buf.hasRemaining()) {
                return -1
            }
            return buf.get().toInt() and 0xFF
        }

        @Throws(IOException::class)
        override fun read(bytes: ByteArray, off: Int, length: Int): Int {
            var len = length
            if (!buf.hasRemaining()) {
                return -1
            }

            len = min(len.toDouble(), buf.remaining().toDouble()).toInt()
            buf[bytes, off, len]
            return len
        }
    }
}