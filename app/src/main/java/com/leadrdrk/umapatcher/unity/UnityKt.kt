package com.leadrdrk.umapatcher.unity

/*
    UnityKt - minimal Unity asset file manipulation library for Kotlin based on AssetStudio/UnityPy
    Supports reading/writing UnityFS asset bundles, serialized files and object type trees.

    Part of the UmaPatcher project.
    (c) 2023 LeadRDRK. Licensed under the Apache License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0
*/

/// IMPLEMENTATION NOTE: Can handle files up to 2GB in size before this thing explodes.
/// Not a problem for this project, but be careful when using it for anything else.

import com.leadrdrk.umapatcher.unity.file.UnityFile
import com.leadrdrk.umapatcher.unity.helper.ImportHelper
import com.leadrdrk.umapatcher.unity.stream.FileBinaryReader
import java.io.File

object UnityKt {
    fun load(file: File): UnityFile? {
        return ImportHelper.parseFile(FileBinaryReader(file.inputStream()), file.path)
    }
}