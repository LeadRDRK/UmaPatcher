package com.leadrdrk.umapatcher.unity.file

import com.leadrdrk.umapatcher.unity.TypeTreeException
import com.leadrdrk.umapatcher.unity.common.BuildTarget
import com.leadrdrk.umapatcher.unity.common.BuildType
import com.leadrdrk.umapatcher.unity.common.ClassIDType
import com.leadrdrk.umapatcher.unity.common.SerializedFileFormatVersion
import com.leadrdrk.umapatcher.unity.common.SerializedType
import com.leadrdrk.umapatcher.unity.common.TypeTree
import com.leadrdrk.umapatcher.unity.helper.TypeTreeHelper
import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import com.leadrdrk.umapatcher.unity.stream.SeekableBinaryReader
import java.io.ByteArrayOutputStream

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ObjectReader(
    private val reader: SeekableBinaryReader,
    val assetsFile: SerializedFile,
    val type: ClassIDType,
    val platform: BuildTarget,
    val version: SerializedFileFormatVersion,
    val byteStart: Long,
    val byteSize: UInt,
    val typeID: Int,
    val classID: Int,
    val isDestroyed: UShort,
    val stripped: UByte,
    val pathID: Long,
    val serializedType: SerializedType?
) : SeekableBinaryReader(reader) {
    init {
        order = reader.order
    }

    val unityVersionInt: IntArray get() = assetsFile.unityVersionInt
    val buildType: BuildType get() = assetsFile.buildType
    private var savedBytes: ByteArray? = null

    override var position: Long
        get() = reader.position
        set(v) { reader.position = v }

    override val size: Long
        get() = reader.size

    constructor(
        reader: SeekableBinaryReader,
        assetsFile: SerializedFile,
        byteStart: Long,
        byteSize: UInt,
        typeID: Int,
        classID: Int,
        isDestroyed: UShort,
        stripped: UByte,
        pathID: Long,
        serializedType: SerializedType?
    ) : this(
        reader = reader,
        assetsFile = assetsFile,
        type = ClassIDType.entries.find { it.value == classID } ?: ClassIDType.UnknownType,
        platform = assetsFile.targetPlatform,
        version = assetsFile.version,
        byteStart = byteStart,
        byteSize = byteSize,
        typeID = typeID,
        classID = classID,
        isDestroyed = isDestroyed,
        stripped = stripped,
        pathID = pathID,
        serializedType = serializedType
    )

    fun readTypeTree(types: TypeTree? = serializedType?.type): MutableMap<String, Any>? {
        return if (types == null)
            null
        else
            TypeTreeHelper.readTypeTree(types, this)
    }

    fun saveTypeTree(obj: Map<String, Any>, types: TypeTree? = serializedType?.type) {
        if (types == null) throw TypeTreeException("Type tree is null")
        val stream = ByteArrayOutputStream()
        val writer = BinaryWriter(stream)
        writer.order = order
        TypeTreeHelper.writeTypeTree(obj, types, writer)
        savedBytes = stream.toByteArray()
    }

    fun save(writer: BinaryWriter) {
        val bytes = if (savedBytes != null)
            savedBytes!!
        else
            getRawData()

        writer.write(bytes)
    }

    fun seekToStart() {
        position = byteStart
    }

    fun getRawData(): ByteArray {
        seekToStart()
        return reader.readBytes(byteSize.toInt())
    }
}