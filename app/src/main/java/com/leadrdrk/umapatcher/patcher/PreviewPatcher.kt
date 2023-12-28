package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.data.TextBlock
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import com.leadrdrk.umapatcher.utils.repoDir

class PreviewPatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false
): AssetsPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode) {
    override fun getTranslationDir(context: Context) =
        context.repoDir.resolve(PREVIEW_TRANSLATIONS_PATH)

    override fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile) =
        "outgame/announceevent/loguiasset/ast_announce_event_log_ui_asset_0${translation.storyId}"

    override fun getBackupDirName() = BACKUP_DIR_NAME

    @Suppress("UNCHECKED_CAST")
    override fun patch(assets: SerializedFile, rootValues: MutableMap<String, Any>, i: Int, textBlock: TextBlock): PatchResult {
        if (textBlock.enText == null && textBlock.enName == null) return PatchResult.Skipped

        val dataArray = rootValues["DataArray"] as List<MutableMap<String, Any>>
        val data = dataArray[i]

        if (textBlock.enName != null) data["Name"] = textBlock.enName
        if (textBlock.enText != null) data["Text"] = textBlock.enText

        return PatchResult.Success
    }

    companion object {
        const val PREVIEW_TRANSLATIONS_PATH = "translations/preview"
        const val BACKUP_DIR_NAME = "preview"
        fun isRestoreAvailable(context: Context) = isRestoreAvailable(context, BACKUP_DIR_NAME)
    }
}