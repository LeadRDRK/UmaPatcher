package com.leadrdrk.umapatcher.unity.common

data class TypeTreeNode(
    val byteSize: Int,
    val index: Int,
    val typeFlags: Int, // m_IsArray
    val version: Int,
    val metaFlag: Int,
    val level: Int,
    val typeStrOffset: UInt,
    val nameStrOffset: UInt,
    val refTypeHash: ULong,
    val variableCount: Int = 0
) {
    lateinit var type: String
    lateinit var name: String

    constructor(
        type: String,
        name: String,
        byteSize: Int,
        index: Int = 0,
        typeFlags: Int,
        version: Int,
        metaFlag: Int = 0,
        level: Int,
        typeStrOffset: UInt = 0U,
        nameStrOffset: UInt = 0U,
        refTypeHash: ULong = 0UL,
        variableCount: Int = 0
    ) : this(
        byteSize,
        index,
        typeFlags,
        version,
        metaFlag,
        level,
        typeStrOffset,
        nameStrOffset,
        refTypeHash,
        variableCount
    ) {
        this.type = type
        this.name = name
    }
}