package com.leadrdrk.umapatcher.unity.file

import com.leadrdrk.umapatcher.unity.common.BuildTarget
import com.leadrdrk.umapatcher.unity.common.BuildType
import com.leadrdrk.umapatcher.unity.common.CommonString
import com.leadrdrk.umapatcher.unity.common.FileIdentifier
import com.leadrdrk.umapatcher.unity.common.LocalSerializedObjectIdentifier
import com.leadrdrk.umapatcher.unity.common.SerializedFileFormatVersion
import com.leadrdrk.umapatcher.unity.common.SerializedType
import com.leadrdrk.umapatcher.unity.common.TypeTree
import com.leadrdrk.umapatcher.unity.common.TypeTreeNode
import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import com.leadrdrk.umapatcher.unity.stream.ByteArrayBinaryReader
import com.leadrdrk.umapatcher.unity.stream.ByteArrayInputStream
import com.leadrdrk.umapatcher.unity.stream.SeekableBinaryReader
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import java.util.UUID

class SerializedFile(
    private val reader: SeekableBinaryReader,
    path: String,
    flags: UInt = 0u
): UnityFile(path, flags) {
    private var endianness: UByte = 0u
    var unityVersion = "2.5.0f5"
    var targetPlatform = BuildTarget.UnknownPlatform
    private var enableTypeTree = true
    val types: MutableList<SerializedType>
    private var bigIDEnabled = 0
    var unityVersionInt = IntArray(4) { 0 }
    var buildType = BuildType("f")
    val objects = mutableMapOf<Long, ObjectReader>()
    val scriptTypes: MutableList<LocalSerializedObjectIdentifier>?
    val externals: MutableList<FileIdentifier>
    val refTypes: MutableList<SerializedType>?
    val userInformation: String?
    private var unknown: Long = 0

    /* Read header */
    private var metadataSize = reader.readUInt32()
    private var fileSize = reader.readUInt32().toLong()
    private val versionInt = reader.readUInt32()
    val version = SerializedFileFormatVersion.entries.find {
        it.value == versionInt
    } ?: SerializedFileFormatVersion.Unknown
    private var dataOffset = reader.readUInt32().toLong()

    init {
        if (version >= SerializedFileFormatVersion.Unknown9) {
            endianness = reader.readUInt8()
            /* reserved */ reader.skip(3)
        }
        else {
            reader.position = fileSize - metadataSize.toLong()
            endianness = reader.readUInt8()
        }

        if (version >= SerializedFileFormatVersion.LargeFilesSupport) {
            metadataSize = reader.readUInt32()
            fileSize = reader.readInt64()
            dataOffset = reader.readInt64()
            unknown = reader.readInt64()
        }

        /* Read metadata */
        if (endianness == 0u.toUByte()) reader.order = ByteOrder.LITTLE_ENDIAN

        if (version >= SerializedFileFormatVersion.Unknown7) {
            unityVersion = reader.readNullTerminatedString()
            setVersion(unityVersion)
        }
        if (version >= SerializedFileFormatVersion.Unknown8) {
            val targetPlatformInt = reader.readInt32()
            targetPlatform = BuildTarget.entries.find {
                it.value == targetPlatformInt
            } ?: BuildTarget.UnknownPlatform
        }
        if (version >= SerializedFileFormatVersion.HasTypeTreeHashes) {
            enableTypeTree = reader.readBoolean()
        }

        /* Read types */
        val typeCount = reader.readInt32()
        types = MutableList(typeCount) {
            readSerializedType(false)
        }

        if (version >= SerializedFileFormatVersion.Unknown7 &&
            version < SerializedFileFormatVersion.Unknown14) {
            bigIDEnabled = reader.readInt32()
        }

        /* Read objects */
        val objectCount = reader.readInt32()
        for (i in 1..objectCount) {
            val pathID = if (bigIDEnabled != 0)
                reader.readInt64()
            else if (version < SerializedFileFormatVersion.Unknown14)
                reader.readInt32().toLong()
            else {
                reader.align()
                reader.readInt64()
            }

            val byteStart = (
                if (version >= SerializedFileFormatVersion.LargeFilesSupport)
                    reader.readInt64()
                else
                    reader.readUInt32().toLong()
            ) + dataOffset

            val byteSize = reader.readUInt32()
            val typeID = reader.readInt32()

            var classID: Int
            var serializedType: SerializedType?
            if (version < SerializedFileFormatVersion.RefactoredClassId) {
                classID = reader.readUInt16().toInt()
                serializedType = types.find { it.classID == typeID }
            }
            else {
                val type = types[typeID]
                classID = type.classID
                serializedType = type
            }

            val isDestroyed = if (version < SerializedFileFormatVersion.HasScriptTypeIndex)
                reader.readUInt16() else 0u

            if (version >= SerializedFileFormatVersion.HasScriptTypeIndex &&
                version < SerializedFileFormatVersion.RefactorTypeData) {
                val scriptTypeIndex = reader.readInt16()
                if (serializedType != null)
                    serializedType.scriptTypeIndex = scriptTypeIndex
            }

            val stripped = if (version == SerializedFileFormatVersion.SupportsStrippedObject ||
                version == SerializedFileFormatVersion.RefactoredClassId)
                reader.readUInt8() else 0u

            objects[pathID] = ObjectReader(
                reader = reader,
                assetsFile = this,
                pathID = pathID,
                byteStart = byteStart,
                byteSize = byteSize,
                typeID = typeID,
                classID = classID,
                serializedType = serializedType,
                isDestroyed = isDestroyed,
                stripped = stripped
            )
        }

        /* Read script types */
        scriptTypes = if (version >= SerializedFileFormatVersion.HasScriptTypeIndex) {
            val scriptCount = reader.readInt32()
            MutableList(scriptCount) {
                LocalSerializedObjectIdentifier(
                    localSerializedFileIndex = reader.readInt32(),
                    localIdentifierInFile = if (version < SerializedFileFormatVersion.Unknown14)
                        reader.readInt32().toLong()
                    else {
                        reader.align()
                        reader.readInt64()
                    }
                )
            }
        } else null

        /* Read externals */
        val externalsCount = reader.readInt32()
        externals = MutableList(externalsCount) {
            var uuid: UUID? = null
            var type: Int? = null
            var tempEmpty: String? = null
            if (version >= SerializedFileFormatVersion.Unknown6) {
                tempEmpty = reader.readNullTerminatedString()
            }
            if (version >= SerializedFileFormatVersion.Unknown5) {
                uuid = UUID(reader.readInt64(), reader.readInt64())
                type = reader.readInt32()
            }

            FileIdentifier(
                tempEmpty = tempEmpty,
                uuid = uuid,
                type = type,
                pathName = reader.readNullTerminatedString()
            )
        }

        refTypes = if (version >= SerializedFileFormatVersion.SupportsRefObject) {
            val refTypesCount = reader.readInt32()
            MutableList(refTypesCount) {
                readSerializedType(true)
            }
        } else null

        userInformation = if (version >= SerializedFileFormatVersion.Unknown5)
            reader.readNullTerminatedString() else null
    }

    private fun readSerializedType(isRefType: Boolean): SerializedType {
        val type = SerializedType(
            classID = reader.readInt32()
        )

        if (version >= SerializedFileFormatVersion.RefactoredClassId) {
            type.isStrippedType = reader.readBoolean()
        }

        if (version >= SerializedFileFormatVersion.RefactorTypeData) {
            type.scriptTypeIndex = reader.readInt16()
        }

        if (version >= SerializedFileFormatVersion.HasTypeTreeHashes) {
            if ((isRefType && type.scriptTypeIndex >= 0) ||
                (version < SerializedFileFormatVersion.RefactoredClassId && type.classID < 0) ||
                (version >= SerializedFileFormatVersion.RefactoredClassId && type.classID == 114)) {
                type.scriptID = reader.readBytes(16)
            }
            type.oldTypeHash = reader.readBytes(16)
        }

        if (enableTypeTree) {
            val typeTree = TypeTree()
            if (version >= SerializedFileFormatVersion.Unknown12 ||
                version == SerializedFileFormatVersion.Unknown10) {
                readTypeTreeBlob(typeTree)
            }
            else {
                readTypeTree(typeTree)
            }
            if (version >= SerializedFileFormatVersion.StoresTypeDependencies) {
                if (isRefType) {
                    type.className = reader.readNullTerminatedString()
                    type.namespace = reader.readNullTerminatedString()
                    type.asmName = reader.readNullTerminatedString()
                }
                else {
                    type.typeDependencies = IntArray(reader.readInt32()) {
                        reader.readInt32()
                    }
                }
            }
            type.type = typeTree
        }

        return type
    }

    private fun readTypeTree(typeTree: TypeTree, level: Int = 0) {
        typeTree.nodes.add(
            TypeTreeNode(
                level = level,
                type = reader.readNullTerminatedString(),
                name = reader.readNullTerminatedString(),
                byteSize = reader.readInt32(),
                variableCount = if (version == SerializedFileFormatVersion.Unknown2)
                    reader.readInt32() else 0,
                index = if (version != SerializedFileFormatVersion.Unknown3)
                    reader.readInt32() else 0,
                typeFlags = reader.readInt32(),
                version = reader.readInt32(),
                metaFlag = if (version != SerializedFileFormatVersion.Unknown3)
                    reader.readInt32() else 0
            )
        )

        val childrenCount = reader.readInt32()
        for (i in 1..childrenCount) {
            readTypeTree(typeTree, level + 1)
        }
    }

    private fun readTypeTreeBlob(typeTree: TypeTree) {
        val numberOfNodes = reader.readInt32()
        val stringBufferSize = reader.readInt32()
        for (i in 1..numberOfNodes) {
            typeTree.nodes.add(
                TypeTreeNode(
                    version = reader.readUInt16().toInt(),
                    level = reader.readUInt8().toInt(),
                    typeFlags = reader.readUInt8().toInt(),
                    typeStrOffset = reader.readUInt32(),
                    nameStrOffset = reader.readUInt32(),
                    byteSize = reader.readInt32(),
                    index = reader.readInt32(),
                    metaFlag = reader.readInt32(),
                    refTypeHash = if (version >= SerializedFileFormatVersion.TypeTreeNodeWithTypeFlags)
                        reader.readUInt64() else 0UL
                )
            )
        }
        typeTree.stringBuffer = reader.readBytes(stringBufferSize)

        val stringBufferReader = ByteArrayBinaryReader(ByteArrayInputStream(typeTree.stringBuffer!!))
        typeTree.nodes.forEach { node ->
            node.type = readTypeTreeBlobString(stringBufferReader, node.typeStrOffset)
            node.name = readTypeTreeBlobString(stringBufferReader, node.nameStrOffset)
        }
    }

    private fun readTypeTreeBlobString(reader: SeekableBinaryReader, value: UInt): String {
        val isOffset = (value and 0x80000000u) == 0u
        if (isOffset) {
            reader.position = value.toLong()
            return reader.readNullTerminatedString()
        }

        val offset = value and 0x7FFFFFFFu
        return CommonString.stringBuffer.getOrElse(offset) { offset.toString() }
    }

    fun setVersion(stringVersion: String) {
        if (stringVersion != "0.0.0") {
            unityVersion = stringVersion
            val buildSplit = stringVersion.replace("\\d".toRegex(), "").split(".")
            buildType = BuildType(buildSplit.find { it.isNotEmpty() } ?: "")
            val versionSplit = stringVersion.replace("\\D".toRegex(), ".").split(".")
            unityVersionInt = versionSplit.map { it.toInt() }.toIntArray()
        }
    }

    override fun close() {
        reader.close()
    }

    override fun getAssetsFile(): SerializedFile {
        return this
    }

    override fun save(writer: BinaryWriter) {
        // Header has to be delayed until the very end

        // Write data first
        val metaStream = ByteArrayOutputStream()
        val metaWriter = BinaryWriter(metaStream)
        metaWriter.order = reader.order
        val dataStream = ByteArrayOutputStream()
        val dataWriter = BinaryWriter(dataStream)
        dataWriter.order = reader.order

        if (version >= SerializedFileFormatVersion.Unknown7) {
            metaWriter.writeNullTerminatedString(unityVersion)
        }

        if (version >= SerializedFileFormatVersion.Unknown8) {
            metaWriter.writeInt32(targetPlatform.value)
        }

        if (version >= SerializedFileFormatVersion.HasTypeTreeHashes) {
            metaWriter.writeBoolean(enableTypeTree)
        }

        /* Write types */
        metaWriter.writeInt32(types.size)
        types.forEach { type ->
            writeSerializedType(type, metaWriter, false)
        }

        if (version >= SerializedFileFormatVersion.Unknown7 &&
            version < SerializedFileFormatVersion.Unknown14) {
            metaWriter.writeInt32(bigIDEnabled)
        }

        /* Write objects */
        metaWriter.writeInt32(objects.size)
        objects.forEach { (pathID, obj) ->
            if (bigIDEnabled != 0)
                metaWriter.writeInt64(pathID)
            else if (version < SerializedFileFormatVersion.Unknown14)
                metaWriter.writeInt32(pathID.toInt())
            else {
                metaWriter.align()
                metaWriter.writeInt64(pathID)
            }

            val p = dataWriter.position
            if (version >= SerializedFileFormatVersion.LargeFilesSupport)
                metaWriter.writeInt64(p)
            else
                metaWriter.writeUInt32(p.toUInt())

            obj.save(dataWriter)
            metaWriter.writeUInt32((dataWriter.position - p).toUInt()) // byteSize

            metaWriter.writeInt32(obj.typeID)

            if (version < SerializedFileFormatVersion.RefactoredClassId) {
                metaWriter.writeUInt16(obj.classID.toUShort())
            }

            if (version < SerializedFileFormatVersion.HasScriptTypeIndex) {
                metaWriter.writeUInt16(obj.isDestroyed)
            }

            if (version >= SerializedFileFormatVersion.HasScriptTypeIndex &&
                version < SerializedFileFormatVersion.RefactorTypeData) {
                // idk
                metaWriter.writeInt16(
                    if (obj.serializedType != null) obj.serializedType.scriptTypeIndex
                    else 0
                )
            }

            if (version == SerializedFileFormatVersion.SupportsStrippedObject ||
                version == SerializedFileFormatVersion.RefactoredClassId) {
                metaWriter.writeUInt8(obj.stripped)
            }
        }

        /* Write script types */
        if (version >= SerializedFileFormatVersion.HasScriptTypeIndex) {
            metaWriter.writeInt32(scriptTypes!!.size)
            scriptTypes.forEach { identifier ->
                metaWriter.writeInt32(identifier.localSerializedFileIndex)
                if (version < SerializedFileFormatVersion.Unknown14)
                    metaWriter.writeInt32(identifier.localIdentifierInFile.toInt())
                else {
                    metaWriter.align()
                    metaWriter.writeInt64(identifier.localIdentifierInFile)
                }
            }
        }

        /* Write externals */
        metaWriter.writeInt32(externals.size)
        externals.forEach { identifier ->
            if (version >= SerializedFileFormatVersion.Unknown6) {
                metaWriter.writeNullTerminatedString(identifier.tempEmpty!!)
            }
            if (version >= SerializedFileFormatVersion.Unknown5) {
                metaWriter.writeInt64(identifier.uuid!!.mostSignificantBits)
                metaWriter.writeInt64(identifier.uuid!!.leastSignificantBits)
            }
            metaWriter.writeNullTerminatedString(identifier.pathName)
        }

         if (version >= SerializedFileFormatVersion.SupportsRefObject) {
            metaWriter.writeInt32(refTypes!!.size)
            refTypes.forEach { type ->
                writeSerializedType(type, metaWriter, true)
            }
         }

        if (version >= SerializedFileFormatVersion.Unknown5)
            metaWriter.writeNullTerminatedString(userInformation!!)

        /* Write header + contents */
        writer.order = ByteOrder.BIG_ENDIAN
        val metaBytes = metaStream.toByteArray()
        val dataBytes = dataStream.toByteArray()
        var headerSize = 16
        if (version >= SerializedFileFormatVersion.Unknown9) {
            headerSize += if (version < SerializedFileFormatVersion.LargeFilesSupport) 4 else 4 + 28
            var dataOffset = headerSize.toLong() + metaBytes.size
            dataOffset += (16 - dataOffset % 16) % 16
            val fileSize = dataOffset + dataBytes.size

            if (version < SerializedFileFormatVersion.LargeFilesSupport) {
                writer.writeUInt32(metaBytes.size.toUInt())
                writer.writeUInt32(fileSize.toUInt())
                writer.writeUInt32(version.value)
                writer.writeUInt32(dataOffset.toUInt())
                writer.writeUInt8(endianness)
                writer.write(ByteArray(3)) // reserved
            }
            else {
                // old header
                writer.writeUInt32(0u)
                writer.writeUInt32(0u)
                writer.writeUInt32(version.value)
                writer.writeUInt32(0u)
                writer.writeUInt8(endianness)
                writer.write(ByteArray(3)) // reserved
                writer.writeUInt32(metaBytes.size.toUInt())
                writer.writeInt64(fileSize)
                writer.writeInt64(dataOffset)
                writer.writeInt64(unknown)
            }

            writer.write(metaBytes)
            writer.align(16)
            writer.write(dataBytes)
        }
        else {
            val fileSize = headerSize + metaBytes.size + 1 + dataBytes.size
            writer.writeUInt32(metaBytes.size.toUInt())
            writer.writeUInt32(fileSize.toUInt())
            writer.writeUInt32(version.value)
            writer.writeUInt32(32u)
            writer.write(dataBytes)
            writer.writeUInt8(endianness)
            writer.write(metaBytes)
        }
    }

    private fun writeSerializedType(type: SerializedType, writer: BinaryWriter, isRefType: Boolean) {
        writer.writeInt32(type.classID)

        if (version >= SerializedFileFormatVersion.RefactoredClassId) {
            writer.writeBoolean(type.isStrippedType)
        }

        if (version >= SerializedFileFormatVersion.RefactorTypeData) {
            writer.writeInt16(type.scriptTypeIndex)
        }

        if (version >= SerializedFileFormatVersion.HasTypeTreeHashes) {
            if ((isRefType && type.scriptTypeIndex >= 0) ||
                (version < SerializedFileFormatVersion.RefactoredClassId && type.classID < 0) ||
                (version >= SerializedFileFormatVersion.RefactoredClassId && type.classID == 114)) {
                writer.write(type.scriptID!!)
            }
            writer.write(type.oldTypeHash!!)
        }

        if (enableTypeTree) {
            if (version >= SerializedFileFormatVersion.Unknown12 ||
                version == SerializedFileFormatVersion.Unknown10) {
                writeTypeTreeBlob(type.type!!, writer)
            }
            else {
                writeTypeTree(type.type!!, writer)
            }
            if (version >= SerializedFileFormatVersion.StoresTypeDependencies) {
                if (isRefType) {
                    writer.writeNullTerminatedString(type.className!!)
                    writer.writeNullTerminatedString(type.namespace!!)
                    writer.writeNullTerminatedString(type.asmName!!)
                }
                else {
                    val deps = type.typeDependencies!!
                    writer.writeInt32(deps.size)
                    deps.forEach {
                        writer.writeInt32(it)
                    }
                }
            }
        }
    }

    private fun writeTypeTreeBlob(typeTree: TypeTree, writer: BinaryWriter) {
        val stringBuffer = typeTree.stringBuffer!!
        writer.writeInt32(typeTree.nodes.size)
        writer.writeInt32(stringBuffer.size)
        typeTree.nodes.forEach { node ->
            writer.writeUInt16(node.version.toUShort())
            writer.writeUInt8(node.level.toUByte())
            writer.writeUInt8(node.typeFlags.toUByte())
            writer.writeUInt32(node.typeStrOffset)
            writer.writeUInt32(node.nameStrOffset)
            writer.writeInt32(node.byteSize)
            writer.writeInt32(node.index)
            writer.writeInt32(node.metaFlag)
            if (version >= SerializedFileFormatVersion.TypeTreeNodeWithTypeFlags)
                writer.writeUInt64(node.refTypeHash)
        }
        writer.write(stringBuffer)
    }

    private fun writeTypeTree(typeTree: TypeTree, writer: BinaryWriter) {
        typeTree.nodes.forEachIndexed { i, node ->
            writer.writeNullTerminatedString(node.type)
            writer.writeNullTerminatedString(node.name)
            writer.writeInt32(node.byteSize)

            if (version == SerializedFileFormatVersion.Unknown2)
                writer.writeInt32(node.variableCount)

            if (version != SerializedFileFormatVersion.Unknown3)
                writer.writeInt32(node.index)

            writer.writeInt32(node.typeFlags)
            writer.writeInt32(node.version)

            if (version != SerializedFileFormatVersion.Unknown3)
                writer.writeInt32(node.metaFlag)

            // calc children count
            var childrenCount = 0
            run {
                typeTree.nodes.subList(i + 1, typeTree.nodes.size).forEach { node2 ->
                    if (node2.level == node.level) return@run
                    if (node2.level == node.level - 1) ++childrenCount
                }
            }
            writer.writeInt32(childrenCount)
        }
    }
}