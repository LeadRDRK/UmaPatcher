package com.leadrdrk.umapatcher.patcher

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.data.AssetsTranslationFile
import com.leadrdrk.umapatcher.data.TextBlock
import com.leadrdrk.umapatcher.unity.UnityKt
import com.leadrdrk.umapatcher.unity.file.BundleFile
import com.leadrdrk.umapatcher.unity.file.ObjectReader
import com.leadrdrk.umapatcher.unity.file.SerializedFile
import com.leadrdrk.umapatcher.unity.stream.BinaryWriter
import com.leadrdrk.umapatcher.unity.stream.FileBinaryReader
import com.leadrdrk.umapatcher.utils.getUid
import com.leadrdrk.umapatcher.utils.workDir
import com.topjohnwu.superuser.Shell
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

// :v
private const val PATCH_SIGNATURE = 6146640879782152448L

abstract class AssetsPatcher(
    private val skipMachineTl: Boolean,
    private val nThreads: Int,
    private val forcePatch: Boolean,
    private val makeBackup: Boolean,
    private val restoreMode: Boolean
) : Patcher() {
    protected abstract fun getTranslationDir(context: Context): File
    protected abstract fun getMetaAssetPath(c: List<String>, translation: AssetsTranslationFile): String
    protected abstract fun patch(assets: SerializedFile, rootValues: MutableMap<String, Any>, i: Int, textBlock: TextBlock): PatchResult
    protected abstract fun getBackupDirName(): String

    private fun getBackupDir() =
        GameChecker.filesDir.resolve("dat/.backup/${getBackupDirName()}")

    protected enum class PatchResult {
        Success,
        Skipped,
        ObjectNotFound,
        ObjectReadFailed
    }

    override fun run(context: Context): Boolean {
        if (restoreMode) return restore(context)

        if (!isDirectInstallAllowed(context)) {
            return false
        }

        val translationDir = getTranslationDir(context)
        if (!translationDir.exists()) {
            log(context.getString(R.string.translation_dir_not_exist))
            return false
        }

        val backupDir = getBackupDir()
        if (makeBackup && !testDirectory(backupDir.path)) {
            val result = createGameDir(context, backupDir.path)
            if (!result.isSuccess) {
                log(context.getString(R.string.failed_to_create_dir).format(backupDir.path))
                return false
            }
        }

        val workDir = context.workDir
        val copyingFileStr = context.getString(R.string.copying_file)
        val failedToCopyFileStr = context.getString(R.string.failed_to_copy_file)

        /* Copy meta database */
        val srcMetaFile = GameChecker.filesDir.resolve("meta")
        val metaFile = workDir.resolve("meta")
        task = copyingFileStr.format(srcMetaFile.name)
        val result = copyAndOwnFile(context, srcMetaFile.path, metaFile.path)
        if (!result.isSuccess) {
            log(failedToCopyFileStr.format(srcMetaFile.path))
            return false
        }

        /* Open database */
        task = context.getString(R.string.opening_db)
        val db = openDatabase(metaFile.path, SQLiteDatabase.OPEN_READONLY) ?: return false

        /* Init strings */
        val failedToReadFileSkipStr = context.getString(R.string.failed_to_read_file_skip)
        val fileNotFoundSkipStr = context.getString(R.string.file_not_found_skip)
        val patchingBundleStr = context.getString(R.string.patching_bundle)
        val invalidFileSkipStr = context.getString(R.string.invalid_file_skip)
        val bundlePatchedSkipStr = context.getString(R.string.bundle_patched_skip)
        val objectNotFoundStr = context.getString(R.string.object_not_found_skip)
        val bundlePatchResultStr = context.getString(R.string.bundle_patch_result)
        val patchingFilesStr = context.getString(R.string.patching_files_n)
        val installCompletedStr = context.getString(R.string.install_completed)
        val installFailedStr = context.getString(R.string.install_failed)
        val backingUpStr = context.getString(R.string.backing_up_name)
        val processingFileStr = context.getString(R.string.processing_file)

        /* Iterate the translation dir */
        val gson = Gson()
        val executor = Executors.newFixedThreadPool(nThreads)
        val tasks = mutableListOf<Callable<Unit>>()
        val completedCount = AtomicInteger(0)
        translationDir.walk().forEach { file ->
            if (file.isDirectory || !file.name.endsWith(".json")) return@forEach

            tasks.add(Callable {
                run thread@ {
                    val components = getParentPathComponents(file, translationDir.path)
                    val relPath = "${components.joinToString(separator = "/")}/${file.name}"

                    val translation = JsonReader(file.reader()).use { reader ->
                        try {
                            gson.fromJson<AssetsTranslationFile>(
                                reader,
                                AssetsTranslationFile::class.java
                            )
                        } catch (_: Exception) {
                            log(failedToReadFileSkipStr.format(relPath))
                            return@thread
                        }
                    }
                    if (skipMachineTl && translation.humanTl != true)
                        return@thread

                    log(processingFileStr.format(relPath))

                    /* Get bundle id from meta database */
                    val metaAssetPath = getMetaAssetPath(components, translation)
                    val bundleId: String
                    db.rawQuery("SELECT h FROM a WHERE n=?", arrayOf(metaAssetPath)).use { cursor ->
                        if (!cursor.moveToNext()) {
                            log(fileNotFoundSkipStr.format(metaAssetPath))
                            return@thread
                        }

                        bundleId = cursor.getString(0)
                    }

                    val srcBundleFile = getBundleFile(bundleId)
                    val bundleFile = workDir.resolve(bundleId)

                    if (!testFile(srcBundleFile.path)) {
                        log(fileNotFoundSkipStr.format(bundleId))
                        return@thread
                    }

                    /* Check if bundle is already patched and up to date */
                    val timestamp = getBundlePatchedTimestampFast(context, srcBundleFile)
                    if (!forcePatch && timestamp == translation.modified) {
                        log(bundlePatchedSkipStr.format(bundleId))
                        return@thread
                    }

                    /* Create backup if file isn't patched */
                    if (makeBackup && timestamp == null) {
                        log(backingUpStr.format(bundleId))
                        val backupFile = backupDir.resolve(bundleId)
                        copyGameFile(context, srcBundleFile.path, backupFile.path)
                    }

                    log(patchingBundleStr.format(bundleId))

                    /* Copy bundle to workDir */
                    val copyRes = copyAndOwnFile(context, srcBundleFile.path, bundleFile.path)
                    if (!copyRes.isSuccess) {
                        log(failedToCopyFileStr.format(bundleId))
                        return@thread
                    }

                    /* Open and patch bundle */
                    val bundle = UnityKt.load(bundleFile)

                    run bundle@{
                        bundle?.close() // bundle does not need to be kept open

                        if (bundle !is BundleFile) {
                            log(invalidFileSkipStr.format(bundleId))
                            return@bundle
                        }

                        val assets = bundle.getAssetsFile()
                        if (assets == null) {
                            log(invalidFileSkipStr.format(bundleId))
                            return@bundle
                        }

                        val rootObject = getRootObject(assets)
                        val rootValues = rootObject?.readTypeTree()
                        if (rootValues == null) {
                            log(invalidFileSkipStr.format(bundleId))
                            return@bundle
                        }

                        var skippedCount = 0
                        var patchedCount = 0
                        translation.text.forEachIndexed text@{ i, textBlock ->
                            val patchRes = patch(assets, rootValues, i, textBlock)
                            when (patchRes) {
                                PatchResult.ObjectNotFound ->
                                    log(objectNotFoundStr.format(textBlock.pathId.toString()))

                                PatchResult.ObjectReadFailed ->
                                    log(failedToReadFileSkipStr.format(textBlock.pathId.toString()))

                                else -> {}
                            }
                            if (patchRes > PatchResult.Success) ++skippedCount
                            else ++patchedCount
                        }

                        log(bundlePatchResultStr.format(bundleId, patchedCount, skippedCount))

                        // Write file
                        rootObject.saveTypeTree(rootValues)
                        BinaryWriter(bundleFile.outputStream()).use { writer ->
                            bundle.save(writer)
                            markPatched(writer, translation.modified)
                        }

                        // Install
                        val installRes = moveGameFile(context, bundleFile.path, srcBundleFile.path)
                        log(
                            "$bundleId - " + if (installRes.isSuccess)
                                installCompletedStr else
                                installFailedStr
                        )
                    }

                    // Delete if patching failed
                    if (bundleFile.exists()) bundleFile.delete()
                }

                val currentCount = completedCount.addAndGet(1)
                progress = currentCount.toFloat() / tasks.size
                task = patchingFilesStr.format(currentCount, tasks.size)
            })
        }

        // Run all tasks and wait for them to complete
        executor.invokeAll(tasks)

        // Cleanup
        db.close()
        metaFile.delete()
        return true
    }

    private fun markPatched(writer: BinaryWriter, modified: Long) {
        writer.order = ByteOrder.BIG_ENDIAN
        writer.writeInt64(PATCH_SIGNATURE)
        writer.writeInt64(modified)
    }

    private fun getBundlePatchedTimestampFast(context: Context, gameFile: File): Long? {
        val tmpFile = context.workDir.resolve(gameFile.name + ".tmp")

        // Copy only the last 16 bytes of the file
        val uid = context.getUid()
        val result = Shell.cmd(
            "tail -c 16 '${gameFile.path}' > '${tmpFile.path}'",
            "chown $uid:$uid '${tmpFile.path}'",
            "chmod 700 '${tmpFile.path}'"
        ).exec()
        if (!result.isSuccess) return null

        // Read the timestamp
        val timestamp = getBundlePatchedTimestamp(tmpFile)
        tmpFile.delete()
        return timestamp
    }

    private fun getBundlePatchedTimestamp(file: File): Long? {
        FileBinaryReader(file.inputStream()).use { reader ->
            if (reader.size < 16) return null
            reader.position = reader.size - 16
            val signature = reader.readInt64()
            if (signature != PATCH_SIGNATURE) return null
            return reader.readInt64()
        }
    }

    private fun isBundlePatched(file: File): Boolean {
        return getBundlePatchedTimestamp(file) != null
    }

    private fun getParentPathComponents(file: File, stopAtPath: String): List<String> {
        var currentFile = file.parentFile ?: throw IllegalArgumentException()
        val list = mutableListOf<String>()

        while (currentFile.path != stopAtPath) {
            list.add(currentFile.name)
            currentFile = currentFile.parentFile ?: throw IllegalArgumentException()
        }

        list.reverse()
        return list
    }

    private fun getBundleFile(bundleId: String) =
        GameChecker.filesDir.resolve("dat/${bundleId.take(2)}/$bundleId")

    @Suppress("UNCHECKED_CAST")
    private fun getRootObject(assets: SerializedFile): ObjectReader? {
        // Manually interpret m_Container in the AssetBundle object
        // (what's type safety anyways?)
        val values = assets.objects[1]?.readTypeTree() ?: return null

        val container = (values["m_Container"] as List<Pair<Any, Any>>)[0]
        val assetInfo = (container.second as Map<String, Any>)["asset"] as Map<String, Any>
        val pathID = assetInfo["m_PathID"] as Long

        return assets.objects[pathID]
    }

    private fun restore(context: Context): Boolean {
        if (!isDirectInstallAllowed(context)) {
            return false
        }

        task = context.getString(R.string.restoring_files)

        val backupDir = getBackupDir()
        val files = listDirectory(backupDir.path)
        if (files == null) {
            log(context.getString(R.string.failed_to_list_dir).format(backupDir.path))
            return false
        }

        val failedToRestoreFileStr = context.getString(R.string.failed_to_restore_file)
        files.forEachIndexed { i, bundleId ->
            val backupFile = backupDir.resolve(bundleId)
            val origFile = getBundleFile(bundleId)
            val result = moveGameFile(context, backupFile.path, origFile.path)
            log(
                if (result.isSuccess) bundleId
                else failedToRestoreFileStr.format(bundleId)
            )
            progress = (i + 1f) / files.size
        }

        removeDirectoryIfEmpty(backupDir.path)
        return true
    }

    companion object {
        fun isRestoreAvailable(context: Context, backupDirName: String): Boolean {
            if (!isRootOperationAllowed(context)) {
                return false
            }
            val backupDir = GameChecker.filesDir.resolve("dat/.backup/$backupDirName")
            return listDirectory(backupDir.path)?.isNotEmpty() == true
        }
    }
}