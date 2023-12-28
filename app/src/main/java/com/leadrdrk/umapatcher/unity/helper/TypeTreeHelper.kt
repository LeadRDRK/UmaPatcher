package com.leadrdrk.umapatcher.unity.helper

import com.leadrdrk.umapatcher.unity.TypeTreeException
import com.leadrdrk.umapatcher.unity.common.TypeTree
import com.leadrdrk.umapatcher.unity.common.TypeTreeNode
import com.leadrdrk.umapatcher.unity.file.ObjectReader
import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import com.leadrdrk.umapatcher.unity.stream.SeekableBinaryReader

private const val kAlignBytes = 0x4000

object TypeTreeHelper {
    fun readTypeTree(types: TypeTree, reader: ObjectReader): MutableMap<String, Any> {
        reader.seekToStart()
        val obj = mutableMapOf<String, Any>()
        val nodes = types.nodes
        var i = 0
        while (++i < nodes.size) {
            val (value, newI) = readValue(nodes, reader, i)
            obj[nodes[i].name] = value
            i = newI
        }
        val readSize = reader.position - reader.byteStart
        if (readSize != reader.byteSize.toLong()) {
            throw TypeTreeException("Error while reading type (expected ${reader.byteSize} bytes, got $readSize)")
        }
        return obj
    }

    private fun readValue(nodes: List<TypeTreeNode>, reader: SeekableBinaryReader, ii: Int): Pair<Any, Int> {
        var i = ii
        val node = nodes[i]
        val varTypeStr = node.type
        var align = (node.metaFlag and kAlignBytes) != 0
        val value: Any = when (varTypeStr) {
            "SInt8" -> reader.readInt8()
            "UInt8" -> reader.readUInt8()
            "char" -> Char(reader.readUInt8().toInt())
            "short", "SInt16" -> reader.readInt16()
            "UInt16", "unsigned short" -> reader.readUInt16()
            "int", "SInt32" -> reader.readInt32()
            "UInt32", "unsigned int", "Type*" -> reader.readUInt32()
            "long long", "SInt64" -> reader.readInt64()
            "UInt64", "unsigned long long", "FileSize" -> reader.readUInt64()
            "float" -> reader.readFloat()
            "double" -> reader.readDouble()
            "bool" -> reader.readBoolean()
            "string" -> {
                i += 3
                reader.readAlignedString()
            }
            "map" -> {
                if ((nodes[i + 1].metaFlag and kAlignBytes) != 0)
                    align = true

                val mapNodes = getNodes(nodes, i)
                i += mapNodes.size - 1
                val first = getNodes(mapNodes, 4)
                val next = 4 + first.size
                val second = getNodes(mapNodes, next)
                val size = reader.readInt32()
                val pairs = mutableListOf<Pair<Any, Any>>()
                for (j in 0..<size) {
                    pairs.add(readValue(first, reader, 0).first to readValue(second, reader, 0).first)
                }
                pairs
            }
            "TypelessData" -> {
                val size = reader.readInt32()
                i += 2
                reader.readBytes(size)
            }
            else -> {
                if (i < nodes.size - 1 && nodes[i + 1].type == "Array") { // Vector
                    if ((nodes[i + 1].metaFlag and kAlignBytes) != 0)
                        align = true

                    val vector = getNodes(nodes, i)
                    i += vector.size - 1
                    val size = reader.readInt32()
                    val list = mutableListOf<Any>()
                    for (j in 0..<size) {
                        list.add(readValue(vector, reader, 3).first)
                    }
                    list
                }
                else { // Class
                    val clazz = getNodes(nodes, i)
                    i += clazz.size - 1
                    val obj = mutableMapOf<String, Any>()
                    var j = 0
                    while (++j < clazz.size) {
                        val (value, newJ) = readValue(clazz, reader, j)
                        obj[clazz[j].name] = value
                        j = newJ
                    }
                    obj
                }
            }
        }
        if (align) reader.align()
        return value to i
    }

    fun writeTypeTree(obj: Map<String, Any>, types: TypeTree, writer: BinaryWriter) {
        val nodes = types.nodes
        var i = 0
        while (++i < nodes.size) {
            val name = nodes[i].name
            val value = obj[name] ?: throw TypeTreeException("Value not found for $name")
            i = writeValue(value, nodes, writer, i)
        }
    }

    private fun writeValue(value: Any, nodes: List<TypeTreeNode>, writer: BinaryWriter, ii: Int): Int {
        var i = ii
        val node = nodes[i]
        val varTypeStr = node.type
        var align = (node.metaFlag and kAlignBytes) != 0
        when (varTypeStr) {
            "SInt8" -> writer.writeInt8(value as Byte)
            "UInt8" -> writer.writeUInt8(value as UByte)
            "char" -> writer.writeInt8((value as Char).code.toByte())
            "short", "SInt16" -> writer.writeInt16(value as Short)
            "UInt16", "unsigned short" -> writer.writeUInt16(value as UShort)
            "int", "SInt32" -> writer.writeInt32(value as Int)
            "UInt32", "unsigned int", "Type*" -> writer.writeUInt32(value as UInt)
            "long long", "SInt64" -> writer.writeInt64(value as Long)
            "UInt64", "unsigned long long", "FileSize" -> writer.writeUInt64(value as ULong)
            "float" -> writer.writeFloat(value as Float)
            "double" -> writer.writeDouble(value as Double)
            "bool" -> writer.writeBoolean(value as Boolean)
            "string" -> {
                writer.writeAlignedString(value as String)
                i += 3
            }
            "map" -> {
                @Suppress("UNCHECKED_CAST")
                value as List<Pair<Any, Any>>

                if ((nodes[i + 1].metaFlag and kAlignBytes) != 0)
                    align = true

                val mapNodes = getNodes(nodes, i)
                i += mapNodes.size - 1
                val first = getNodes(mapNodes, 4)
                val next = 4 + first.size
                val second = getNodes(mapNodes, next)

                writer.writeInt32(value.size)
                value.forEach { pair ->
                    writeValue(pair.first, first, writer, 0)
                    writeValue(pair.second, second, writer, 0)
                }
            }
            "TypelessData" -> {
                value as ByteArray
                writer.writeInt32(value.size)
                writer.write(value)
                i += 2
            }
            else -> {
                if (i < nodes.size - 1 && nodes[i + 1].type == "Array") { // Vector
                    @Suppress("UNCHECKED_CAST")
                    value as List<Any>

                    if ((nodes[i + 1].metaFlag and kAlignBytes) != 0)
                        align = true

                    val vector = getNodes(nodes, i)
                    i += vector.size - 1

                    writer.writeInt32(value.size)
                    value.forEach { v ->
                        writeValue(v, vector, writer, 3)
                    }
                }
                else { // Class
                    @Suppress("UNCHECKED_CAST")
                    value as Map<String, Any>

                    val clazz = getNodes(nodes, i)
                    i += clazz.size - 1

                    var j = 0
                    while (++j < clazz.size) {
                        val name = clazz[j].name
                        val v = value[name] ?: throw TypeTreeException("Value not found for $name")
                        j = writeValue(v, clazz, writer, j)
                    }
                }
            }
        }
        if (align) writer.align()
        return i
    }

    private fun getNodes(mNodes: List<TypeTreeNode>, index: Int): List<TypeTreeNode> {
        val nodes = mutableListOf<TypeTreeNode>()
        nodes.add(mNodes[index])
        val level = mNodes[index].level
        for (i in index + 1..<mNodes.size) {
            val member = mNodes[i]
            val level2 = member.level
            if (level2 <= level) {
                return nodes
            }
            nodes.add(member)
        }
        return nodes
    }
}