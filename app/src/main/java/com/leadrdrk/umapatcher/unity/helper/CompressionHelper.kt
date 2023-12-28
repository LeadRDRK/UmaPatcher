package com.leadrdrk.umapatcher.unity.helper

import net.jpountz.lz4.LZ4Factory
import java.io.IOException

object CompressionHelper {
    fun decompressLz4(compressedBytes: ByteArray, uncompressedSize: Int, dest: ByteArray, destOff: Int): ByteArray {
        val factory = LZ4Factory.fastestInstance()
        val decompressor = factory.safeDecompressor()
        val decompressedSize =
            decompressor.decompress(compressedBytes, 0, compressedBytes.size, dest, destOff, uncompressedSize)

        if (decompressedSize != uncompressedSize) {
            throw IOException(
                "LZ4 decompression failed (expected $uncompressedSize bytes, got $decompressedSize bytes)"
            )
        }

        return dest
    }

    fun decompressLz4(compressedBytes: ByteArray, uncompressedSize: Int): ByteArray {
        val uncompressedBytes = ByteArray(uncompressedSize)
        return decompressLz4(compressedBytes, uncompressedSize, uncompressedBytes, 0)
    }

    fun compressLz4(uncompressedBytes: ByteArray, compLevel: Int): Pair<ByteArray, Int> {
        val factory = LZ4Factory.fastestInstance()
        val compressor = factory.highCompressor(compLevel)

        val maxSize = compressor.maxCompressedLength(uncompressedBytes.size)
        val compressedBytes = ByteArray(maxSize)
        val compressedSize = compressor.compress(
            uncompressedBytes, 0, uncompressedBytes.size,
            compressedBytes, 0, maxSize
        )
        return compressedBytes to compressedSize
    }
}