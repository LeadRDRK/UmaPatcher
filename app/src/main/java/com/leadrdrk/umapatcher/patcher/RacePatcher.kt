package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.data.TextBlock
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import com.leadrdrk.umapatcher.utils.repoDir

class RacePatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false
): AssetsPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode) {
    override fun getTranslationDir(context: Context) =
        context.repoDir.resolve(RACE_TRANSLATIONS_PATH)

    override fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile) =
        "race/storyrace/text/storyrace_${translation.storyId}"

    override fun getBackupDirName() = BACKUP_DIR_NAME

    @Suppress("UNCHECKED_CAST")
    override fun patch(assets: SerializedFile, rootValues: MutableMap<String, Any>, i: Int, textBlock: TextBlock): PatchResult {
        return if (textBlock.enText != null) {
            val textData = rootValues["textData"] as List<MutableMap<String, Any>>
            textData[i]["text"] = textBlock.enText
            PatchResult.Success
        } else PatchResult.Skipped
    }

    companion object {
        const val RACE_TRANSLATIONS_PATH = "translations/race"
        const val BACKUP_DIR_NAME = "race"
        fun isRestoreAvailable(context: Context) = isRestoreAvailable(context, BACKUP_DIR_NAME)
    }
}