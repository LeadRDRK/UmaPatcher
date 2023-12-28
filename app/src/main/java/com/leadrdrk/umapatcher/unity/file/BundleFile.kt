package com.leadrdrk.umapatcher.unity.file

import com.leadrdrk.umapatcher.unity.helper.CompressionHelper
import com.leadrdrk.umapatcher.unity.helper.ImportHelper
import com.leadrdrk.umapatcher.unity.stream.BinaryReader
import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import com.leadrdrk.umapatcher.unity.stream.ByteArrayBinaryReader
import com.leadrdrk.umapatcher.unity.stream.ByteArrayInputStream
import com.leadrdrk.umapatcher.unity.stream.SeekableBinaryReader
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

class BundleFile(
    private val reader: SeekableBinaryReader,
    path: String
): UnityFile(path, 0u) {
    @Suppress("unused")
    private enum class ArchiveFlags(val value: UInt)
    {
        CompressionTypeMask(0x3fu),
        BlocksAndDirectoryInfoCombined(0x40u),
        BlocksInfoAtTheEnd(0x80u),
        OldWebPluginCompatibility(0x100u),
        BlockInfoNeedPaddingAtStart(0x200u)
    }

    @Suppress("unused")
    private enum class StorageBlockFlags(val value: UShort)
    {
        CompressionTypeMask(0x3fu),
        Streamed(0x40u)
    }

    @Suppress("unused")
    enum class CompressionType(val value: UInt) {
        None(0u),
        Lzma(1u),
        Lz4(2u),
        Lz4HC(3u),
        Lzham(4u)
    }

    private data class StorageBlock(
        val compressedSize: UInt,
        val uncompressedSize: UInt,
        val flags: UShort
    )

    private data class Node(
        val offset: Long,
        val size: Long,
        val flags: UInt,
        val path: String
    )

    private var size: Long = 0
    private var compressedBlocksInfoSize: UInt = 0u
    private var uncompressedBlocksInfoSize: UInt = 0u

    private lateinit var blocksInfo: Array<StorageBlock>
    private lateinit var directoryInfo: Array<Node>

    lateinit var files: List<UnityFile>

    val signature = reader.readNullTerminatedString(20)
    val version = reader.readUInt32()
    val playerVersion = reader.readNullTerminatedString(20)
    val engineVersion = reader.readNullTerminatedString(20)

    init {
        when (signature) {
            "UnityFS" -> readFS()
            else -> throw UnsupportedOperationException("Unknown signature: $signature")
        }
    }

    private fun readFS() {
        readHeader()
        readBlocksInfoAndDirectory()

        val buffer = createBlocksBuffer()
        readBlocks(buffer)
        readFiles(buffer)
    }

    private fun createBlocksBuffer(): ByteArray {
        return ByteArray(blocksInfo.sumOf { it.uncompressedSize }.toInt())
    }

    private fun readHeader() {
        size = reader.readInt64()
        compressedBlocksInfoSize = reader.readUInt32()
        uncompressedBlocksInfoSize = reader.readUInt32()
        flags = reader.readUInt32()
    }

    private fun isFlagSet(flag: ArchiveFlags): Boolean {
        return (flags and flag.value) != 0u
    }

    private fun readBlocksInfoAndDirectory() {
        val blockInfoBytes = ByteArray(compressedBlocksInfoSize.toInt())
        if (version >= 7u) {
            reader.align(16)
        }

        if (isFlagSet(ArchiveFlags.BlocksInfoAtTheEnd)) {
            val pos = reader.position
            reader.position = size - compressedBlocksInfoSize.toLong()
            reader.readBytes(blockInfoBytes, 0, compressedBlocksInfoSize.toInt())
            reader.position = pos
        }
        else { // 0x40 BlocksAndDirectoryInfoCombined
            reader.readBytes(blockInfoBytes, 0, compressedBlocksInfoSize.toInt())
        }

        val uncompressedSize = uncompressedBlocksInfoSize.toInt()
        val blocksInfoReader = when (val compressionType = flags and ArchiveFlags.CompressionTypeMask.value) {
            CompressionType.None.value -> BinaryReader(ByteArrayInputStream(blockInfoBytes))
            CompressionType.Lz4.value, CompressionType.Lz4HC.value -> {
                val uncompressedBytes = CompressionHelper.decompressLz4(blockInfoBytes, uncompressedSize)
                BinaryReader(ByteArrayInputStream(uncompressedBytes))
            }
            else -> throw UnsupportedOperationException("Unsupported compression type: $compressionType")
        }

        blocksInfoReader.skip(16) // uncompressedDataHash
        val blocksInfoCount = blocksInfoReader.readInt32()
        blocksInfo = Array(blocksInfoCount) {
            StorageBlock(
                uncompressedSize = blocksInfoReader.readUInt32(),
                compressedSize = blocksInfoReader.readUInt32(),
                flags = blocksInfoReader.readUInt16()
            )
        }

        val nodesCount = blocksInfoReader.readInt32()
        directoryInfo = Array(nodesCount) {
            Node(
                offset = blocksInfoReader.readInt64(),
                size = blocksInfoReader.readInt64(),
                flags = blocksInfoReader.readUInt32(),
                path = blocksInfoReader.readNullTerminatedString()
            )
        }

        if (isFlagSet(ArchiveFlags.BlockInfoNeedPaddingAtStart)) {
            reader.align(16)
        }
    }

    private fun readBlocks(buffer: ByteArray) {
        var off = 0
        blocksInfo.forEach { block ->
            @Suppress("LiftReturnOrAssignment") // god knows why you're so persistent
            when (val compressionType = (block.flags and StorageBlockFlags.CompressionTypeMask.value).toUInt()) {
                CompressionType.None.value -> off += reader.readBytes(buffer, off, block.compressedSize.toInt())
                CompressionType.Lz4.value, CompressionType.Lz4HC.value -> {
                    val compressedSize = block.compressedSize.toInt()
                    val compressedBytes = ByteArray(compressedSize)
                    reader.readBytes(compressedBytes, 0, compressedSize)

                    val uncompressedSize = block.uncompressedSize.toInt()
                    CompressionHelper.decompressLz4(compressedBytes, uncompressedSize, buffer, off)

                    off += uncompressedSize
                }
                else -> throw UnsupportedOperationException("Unsupported compression type: $compressionType")
            }
        }
    }

    private fun readFiles(buffer: ByteArray) {
        var off = 0
        files = List(directoryInfo.size) { i ->
            val node = directoryInfo[i]
            val size = node.size.toInt()
            val fileBuffer = buffer.copyOfRange(off, off + size)
            off += size

            val reader = ByteArrayBinaryReader(ByteArrayInputStream(fileBuffer))
            ImportHelper.parseFile(reader, node.path, node.flags)
                ?: ResourceFile(
                    buffer = fileBuffer,
                    path = node.path,
                    flags = node.flags
                )
        }
    }

    override fun close() {
        reader.close()
    }

    override fun getAssetsFile(): SerializedFile? {
        return files.find { it is SerializedFile } as SerializedFile?
    }

    override fun save(writer: BinaryWriter) {
        save(writer, null, null)
    }

    fun save(writer: BinaryWriter, compType: CompressionType? = null, compLevel: Int? = null) {
        val compressionType = if (compType == null) {
            val value = flags and ArchiveFlags.CompressionTypeMask.value
            CompressionType.entries.find { it.value == value }!!
        }
        else compType

        val compressionLevel = compLevel
            ?: when (compressionType) {
                CompressionType.Lz4 -> 1
                CompressionType.Lz4HC -> 9 // LZ4HC_CLEVEL_DEFAULT
                CompressionType.None -> 0
                else -> throw UnsupportedOperationException("Unsupported compression type: $compressionType")
            }

        writer.order = ByteOrder.BIG_ENDIAN
        val p = writer.position
        writer.writeNullTerminatedString(signature)
        writer.writeUInt32(version)
        writer.writeNullTerminatedString(playerVersion)
        writer.writeNullTerminatedString(engineVersion)
        val initHeaderSize = writer.position - p

        when (signature) {
            "UnityFS" -> writeFS(writer, compressionType, compressionLevel, initHeaderSize)
            else -> throw UnsupportedOperationException("Unknown signature: $signature")
        }
    }

    private data class WrittenNode(
        val size: Long,
        val flags: UInt,
        val path: String
    )

    private fun writeFS(writer: BinaryWriter, compType: CompressionType, compLevel: Int, initHeaderSize: Long) {
        val dataStream = ByteArrayOutputStream()
        val dataWriter = BinaryWriter(dataStream)

        val writtenFiles = files.map { file ->
            val p = dataWriter.position
            file.save(dataWriter)
            WrittenNode(
                size = dataWriter.position - p,
                flags = file.flags,
                path = file.path
            )
        }

        val uncompressedData = dataStream.toByteArray()
        val uncompressedDataSize = uncompressedData.size.toUInt()

        val compressedDataSize: UInt
        val compressedData = when (compType) {
            CompressionType.None -> {
                compressedDataSize = uncompressedDataSize
                uncompressedData
            }
            CompressionType.Lz4, CompressionType.Lz4HC -> {
                val pair = CompressionHelper.compressLz4(uncompressedData, compLevel)
                compressedDataSize = pair.second.toUInt()
                pair.first
            }
            else -> throw UnsupportedOperationException("Unsupported compression type: $compType")
        }

        val blockStream = ByteArrayOutputStream()
        val blockWriter = BinaryWriter(blockStream)

        // preserve block info flags and set new compression flag
        val blockFlags = (blocksInfo.getOrNull(0)?.flags ?: 0u) and
            StorageBlockFlags.CompressionTypeMask.value.inv() or
            compType.value.toUShort()

        // uncompressed data hash
        blockWriter.write(ByteArray(16))
        // data block info
        blockWriter.writeInt32(1) // block count
        blockWriter.writeUInt32(uncompressedDataSize)
        blockWriter.writeUInt32(compressedDataSize)
        blockWriter.writeUInt16(blockFlags)

        // file block info
        if ((flags and ArchiveFlags.BlocksAndDirectoryInfoCombined.value) == 0u) {
            throw UnsupportedOperationException("Cannot write bundle file without DirectoryInfo (flag not set)")
        }
        blockWriter.writeInt32(files.size)
        var offset = 0L
        writtenFiles.forEach { node ->
            blockWriter.writeInt64(offset)
            blockWriter.writeInt64(node.size)
            offset += node.size
            blockWriter.writeUInt32(node.flags)
            blockWriter.writeNullTerminatedString(node.path)
        }

        // compress block data
        val blockData = blockStream.toByteArray()
        val uncompressedBlockDataSize = blockData.size.toUInt()

        val compressedBlockDataSize: UInt
        val compressedBlockData = when (compType) {
            CompressionType.None -> {
                compressedBlockDataSize = uncompressedBlockDataSize
                blockData
            }
            CompressionType.Lz4, CompressionType.Lz4HC -> {
                val pair = CompressionHelper.compressLz4(blockData, compLevel)
                compressedBlockDataSize = pair.second.toUInt()
                pair.first
            }
            else -> throw UnsupportedOperationException("Unsupported compression type: $compType")
        }

        // preserve flags and set new compression flag
        val dataFlags = flags and ArchiveFlags.CompressionTypeMask.value.inv() or compType.value

        // calculate file size
        var fileSize = initHeaderSize + 20 // + header info
        if (version >= 7u) {
            // align 16
            fileSize += (16 - fileSize % 16) % 16
        }
        if (isFlagSet(ArchiveFlags.BlocksInfoAtTheEnd)) {
            if (isFlagSet(ArchiveFlags.BlockInfoNeedPaddingAtStart) && version < 7u) {
                fileSize += (16 - fileSize % 16) % 16
            }
            fileSize += compressedDataSize.toLong() + compressedBlockDataSize.toLong()
        }
        else {
            fileSize += compressedBlockDataSize.toLong()
            if (isFlagSet(ArchiveFlags.BlockInfoNeedPaddingAtStart)) {
                fileSize += (16 - fileSize % 16) % 16
            }
            fileSize += compressedDataSize.toLong()
        }

        // write header info
        writer.writeInt64(fileSize)
        writer.writeUInt32(compressedBlockDataSize)
        writer.writeUInt32(uncompressedBlockDataSize)
        writer.writeUInt32(dataFlags)

        if (version >= 7u) {
            writer.align(16)
        }

        // write data
        if (isFlagSet(ArchiveFlags.BlocksInfoAtTheEnd)) {
            if (isFlagSet(ArchiveFlags.BlockInfoNeedPaddingAtStart) && version < 7u) {
                writer.align(16)
            }
            writer.write(compressedData, 0, compressedDataSize.toInt())
            writer.write(compressedBlockData, 0, compressedBlockDataSize.toInt())
        }
        else {
            writer.write(compressedBlockData, 0, compressedBlockDataSize.toInt())
            if (isFlagSet(ArchiveFlags.BlockInfoNeedPaddingAtStart)) {
                writer.align(16)
            }
            writer.write(compressedData, 0, compressedDataSize.toInt())
        }
    }
}