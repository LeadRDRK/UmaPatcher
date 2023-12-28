package com.leadrdrk.umapatcher.unity.common

@Suppress("ArrayInDataClass")
data class TypeTree(
    val nodes: MutableList<TypeTreeNode> = mutableListOf(),
    var stringBuffer: ByteArray? = null
)