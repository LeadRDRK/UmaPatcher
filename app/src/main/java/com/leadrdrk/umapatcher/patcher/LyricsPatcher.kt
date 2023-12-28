package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.data.TextBlock
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import com.leadrdrk.umapatcher.utils.repoDir

class LyricsPatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false
): AssetsPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode) {
    override fun getTranslationDir(context: Context) =
        context.repoDir.resolve(LYRICS_TRANSLATIONS_PATH)

    override fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile) =
        "live/musicscores/m${translation.storyId}/m${translation.storyId}_lyrics"

    override fun getBackupDirName() = BACKUP_DIR_NAME

    override fun patch(assets: SerializedFile, rootValues: MutableMap<String, Any>, i: Int, textBlock: TextBlock): PatchResult {
        // enforce csv quoting
        val text = "\"${(textBlock.enText ?: textBlock.jpText!!).replace("\"", "\"\"")}\""
        val script = if (i == 0) "time,lyrics\n" else rootValues["m_Script"] as String
        rootValues["m_Script"] = script + "${textBlock.time},$text\n"
        return if (textBlock.enText == null) PatchResult.Skipped else PatchResult.Success
    }

    companion object {
        const val LYRICS_TRANSLATIONS_PATH = "translations/lyrics"
        const val BACKUP_DIR_NAME = "lyrics"
        fun isRestoreAvailable(context: Context) = isRestoreAvailable(context, BACKUP_DIR_NAME)
    }
}