package com.leadrdrk.umapatcher.unity.common

@Suppress("unused")
enum class SerializedFileFormatVersion(val value: UInt) {
    Unknown(0u),
    Unsupported(1u),
    Unknown2(2u),
    Unknown3(3u),
    // 1.2.0 to 2.0.0
    Unknown5(5u),
    // 2.1.0 to 2.6.1
    Unknown6(6u),
    // 3.0.0b
    Unknown7(7u),
    // 3.0.0 to 3.4.2
    Unknown8(8u),
    // 3.5.0 to 4.7.2
    Unknown9(9u),
    // 5.0.0aunk1
    Unknown10(10u),
    // 5.0.0aunk2
    HasScriptTypeIndex(11u),
    // 5.0.0aunk3
    Unknown12(12u),
    // 5.0.0aunk4
    HasTypeTreeHashes(13u),
    // 5.0.0unk
    Unknown14(14u),
    // 5.0.1 to 5.4.0
    SupportsStrippedObject(15u),
    // 5.5.0a
    RefactoredClassId(16u),
    // 5.5.0unk to 2018.4
    RefactorTypeData(17u),
    // 2019.1a
    RefactorShareableTypeTreeData(18u),
    // 2019.1unk
    TypeTreeNodeWithTypeFlags(19u),
    // 2019.2
    SupportsRefObject(20u),
    // 2019.3 to 2019.4
    StoresTypeDependencies(21u),
    // 2020.1 to x
    LargeFilesSupport(22u)
}
