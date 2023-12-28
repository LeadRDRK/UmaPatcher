package com.leadrdrk.umapatcher.unity.common

import java.util.UUID

data class FileIdentifier(
    var tempEmpty: String?,
    var uuid: UUID?,
    var type: Int?, // enum { kNonAssetType = 0, kDeprecatedCachedAssetType = 1, kSerializedAssetType = 2, kMetaAssetType = 3 };
    var pathName: String
)
