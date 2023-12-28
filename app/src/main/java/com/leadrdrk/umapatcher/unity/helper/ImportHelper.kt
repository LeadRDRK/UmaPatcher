package com.leadrdrk.umapatcher.unity.helper

import com.leadrdrk.umapatcher.unity.common.SerializedFileFormatVersion
import com.leadrdrk.umapatcher.unity.file.BundleFile
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import com.leadrdrk.umapatcher.unity.file.UnityFile
import com.leadrdrk.umapatcher.unity.stream.SeekableBinaryReader

object ImportHelper {
    fun parseFile(reader: SeekableBinaryReader, path: String, flags: UInt = 0u): UnityFile? {
        // safety measure for dummies
        if (reader.size > Int.MAX_VALUE) {
            throw UnsupportedOperationException("Attempt to read a file larger than 2GB")
        }

        val signature = reader.readNullTerminatedString(20)
        reader.position = 0

        return when (signature) {
            "UnityFS" -> BundleFile(reader, path)
            else -> {
                if (isSerializedFile(reader))
                    SerializedFile(reader, path, flags)
                else null
            }
        }
    }

    private fun isSerializedFile(reader: SeekableBinaryReader): Boolean {
        if (reader.size < 20) return false
        /* metadataSize */ reader.readUInt32()
        var fileSize = reader.readUInt32().toLong()
        val version = reader.readUInt32()
        var dataOffset = reader.readUInt32().toLong()
        /* endianness */ reader.readUInt8()
        /* reserved */ reader.skip(3)

        if (version >= SerializedFileFormatVersion.LargeFilesSupport.value) {
            if (reader.size < 48) {
                reader.position = 0
                return false
            }
            /* metadataSize */ reader.readUInt32()
            fileSize = reader.readInt64()
            dataOffset = reader.readInt64()
        }

        reader.position = 0
        return fileSize == reader.size && dataOffset < reader.size
    }
}