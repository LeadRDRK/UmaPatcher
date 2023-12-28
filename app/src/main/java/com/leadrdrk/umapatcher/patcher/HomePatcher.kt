package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.utils.repoDir

class HomePatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false,
    cps: Int = 28,
    fps: Int = 30
): BaseStoryPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode, cps, fps) {
    override fun getTranslationDir(context: Context) =
        context.repoDir.resolve(HOME_TRANSLATIONS_PATH)

    override fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile) =
        "home/data/${c[0]}/${c[1]}/hometimeline_${c[0]}_${c[1]}_${translation.storyId.takeLast(7)}"

    override fun getBackupDirName() = BACKUP_DIR_NAME

    companion object {
        const val HOME_TRANSLATIONS_PATH = "translations/home"
        const val BACKUP_DIR_NAME = "home"
        fun isRestoreAvailable(context: Context) = isRestoreAvailable(context, BACKUP_DIR_NAME)
    }
}