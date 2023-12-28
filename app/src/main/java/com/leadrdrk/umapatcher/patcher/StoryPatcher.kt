package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.utils.repoDir

class StoryPatcher(
    skipMachineTl: Boolean = false,
    nThreads: Int = 8,
    forcePatch: Boolean = false,
    makeBackup: Boolean = false,
    restoreMode: Boolean = false,
    cps: Int = 28,
    fps: Int = 30
): BaseStoryPatcher(skipMachineTl, nThreads, forcePatch, makeBackup, restoreMode, cps, fps) {
    override fun getTranslationDir(context: Context) =
        context.repoDir.resolve(STORY_TRANSLATIONS_PATH)

    override fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile) =
        "story/data/${c[0]}/${c[1]}/storytimeline_${translation.storyId}"

    override fun getBackupDirName() = BACKUP_DIR_NAME

    companion object {
        const val STORY_TRANSLATIONS_PATH = "translations/story"
        const val BACKUP_DIR_NAME = "story"
        fun isRestoreAvailable(context: Context) = isRestoreAvailable(context, BACKUP_DIR_NAME)
    }
}