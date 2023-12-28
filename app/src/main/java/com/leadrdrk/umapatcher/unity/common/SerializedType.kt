package com.leadrdrk.umapatcher.unity.common

@Suppress("ArrayInDataClass")
data class SerializedType(
    var classID: Int,
    var isStrippedType: Boolean = false,
    var scriptTypeIndex: Short = -1,
    var type: TypeTree? = null,
    var scriptID: ByteArray? = null, // Hash128
    var oldTypeHash: ByteArray? = null, // Hash128
    var typeDependencies: IntArray? = null,
    var className: String? = null,
    var namespace: String? = null,
    var asmName: String? = null
)
