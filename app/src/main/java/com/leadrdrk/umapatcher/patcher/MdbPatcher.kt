package com.leadrdrk.umapatcher.patcher

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.data.MdbTableTranslationDetail
import com.leadrdrk.umapatcher.data.MdbTableTranslationDetailDeserializer
import com.leadrdrk.umapatcher.data.MdbTranslationFile
import com.leadrdrk.umapatcher.utils.repoDir
import com.leadrdrk.umapatcher.utils.workDir
import com.topjohnwu.superuser.Shell
import java.io.File

class MdbPatcher(
    private val directInstall: Boolean = true,
    private val makeBackup: Boolean = true,
    private val restoreMode: Boolean = false,
    private val fileUri: Uri? = null,
    private val overrides: Map<String, String> = mapOf(),
) : Patcher() {
    override fun run(context: Context): Boolean {
        if (restoreMode) return restore(context)

        val mdbFile = context.workDir.resolve("master.mdb")
        val repoDir = context.repoDir
        val indexFile = repoDir.resolve("src/mdb/index.json")
        val translationDir = repoDir.resolve(MDB_TRANSLATIONS_PATH)
        var origFile: File? = null

        if (!translationDir.exists()) {
            log(context.getString(R.string.translation_dir_not_exist))
            return false
        }

        /* Copying file */
        val failedToReadFileStr = context.getString(R.string.failed_to_read_file)
        task = context.getString(R.string.copying_file).format(mdbFile.name)
        if (directInstall) {
            if (!isDirectInstallAllowed(context)) {
                return false
            }
            origFile = GameChecker.filesDir.resolve("master/master.mdb")
            var result = copyAndOwnFile(context, origFile.path, mdbFile.path)
            if (!result.isSuccess) {
                log(context.getString(R.string.failed_to_copy_file).format(origFile.path))
                return false
            }
            if (makeBackup) {
                task = context.getString(R.string.backing_up)
                val backupFile = GameChecker.filesDir.resolve("master/master.mdb.bak")
                result = Shell.cmd("cp -a '${origFile.path}' '${backupFile.path}'").exec()
                if (!result.isSuccess) {
                    log(context.getString(R.string.failed_to_copy_file).format(origFile.path))
                    return false
                }
            }
        }
        else {
            context.contentResolver.openInputStream(fileUri!!).use { input ->
                if (input == null) {
                    log(failedToReadFileStr.format(mdbFile.name))
                    return false
                }

                mdbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(
                MdbTableTranslationDetail::class.java,
                MdbTableTranslationDetailDeserializer()
            )
            .create()

        /* Reading index file */
        task = context.getString(R.string.reading_index_file)
        val translationDetail = JsonReader(indexFile.reader()).use { reader ->
            try {
                gson.fromJson<Array<MdbTableTranslationDetail>>(
                    reader, Array<MdbTableTranslationDetail>::class.java
                )
            }
            catch (ex: Exception) {
                log(failedToReadFileStr.format(indexFile.name))
                logException(ex)
                return@run false
            }
        }

        /* Opening database */
        task = context.getString(R.string.opening_db)
        val db = openDatabase(mdbFile.path) ?: return false
        db.rawQuery("PRAGMA journal_mode = OFF", null).close()
        db.rawQuery("PRAGMA synchronous = OFF", null).close()
        db.beginTransaction()

        /* Translating tables */
        val fileNotFoundStr = context.getString(R.string.file_not_found_skip)
        val failedToReadFileSkipStr = context.getString(R.string.failed_to_read_file_skip)
        val translatingTableStr = context.getString(R.string.translating_table)
        val processingFileStr = context.getString(R.string.processing_file)
        val nRowsTranslatedStr = context.getString(R.string.n_rows_translated)
        val nEntriesNotFoundStr = context.getString(R.string.n_entries_not_found)
        translationDetail.forEach { table ->
            task = translatingTableStr.format(table.name)
            val perFileProgress = 1f/table.files.size
            table.files.forEachIndexed files@ { i, n ->
                val fileProgress = perFileProgress * i
                val name = if (overrides.containsKey(n)) overrides[n]!! else n
                val relPath = if (table.subdir) "${table.name}/$name.json" else "$name.json"
                val f = translationDir.resolve(relPath)
                if (!f.exists() || f.isDirectory) {
                    log(fileNotFoundStr.format(relPath))
                    return@files
                }

                log("* " + processingFileStr.format(relPath))
                val translation = JsonReader(f.reader()).use { reader ->
                    try {
                        gson.fromJson<MdbTranslationFile>(reader, MdbTranslationFile::class.java)
                    }
                    catch (_: Exception) {
                        log(failedToReadFileSkipStr.format(relPath))
                        return@files
                    }
                }

                var totalRows = 0
                var entriesNotFound = 0
                var textIndex = 0
                val textCount = translation.text.size
                translation.text.forEach text@ { (srcText, transText) ->
                    ++textIndex
                    // ignore untranslated values
                    if (transText.isEmpty()) {
                        val textProgress = textIndex.toFloat()/textCount
                        progress = fileProgress + perFileProgress * textProgress
                        return@text
                    }

                    val cv = ContentValues()
                    cv.put(table.textColumn, transText)
                    val rows = db.update(
                        table.name,
                        cv,
                        "${table.textColumn} = ?",
                        arrayOf(srcText)
                    )
                    if (rows == 0) ++entriesNotFound
                    totalRows += rows

                    val textProgress = textIndex.toFloat()/textCount
                    progress = fileProgress + perFileProgress * textProgress
                }
                log(nRowsTranslatedStr.format(totalRows))
                if (entriesNotFound > 0)
                    log(nEntriesNotFoundStr.format(entriesNotFound))
                log("")
            }
        }

        db.version = 1032 // mark patched
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()

        task = context.getString(R.string.installing)
        if (directInstall) {
            val result = moveGameFile(context, mdbFile.path, origFile!!.path)
            log(if (result.isSuccess)
                context.getString(R.string.install_completed) else
                context.getString(R.string.install_failed)
            )
        }
        else {
            saveFile("master.mdb", mdbFile) {
                mdbFile.delete()
                log(context.getString(R.string.install_completed))
            }
        }

        return true
    }

    private fun restore(context: Context): Boolean {
        if (!isDirectInstallAllowed(context)) {
            return false
        }
        task = context.getString(R.string.restoring_files)

        val filesDir = GameChecker.filesDir
        val backupFile = filesDir.resolve("master/master.mdb.bak")
        val destFile = filesDir.resolve("master/master.mdb")

        val result = moveGameFile(context, backupFile.path, destFile.path)
        return result.isSuccess
    }

    companion object {
        const val MDB_TRANSLATIONS_PATH = "translations/mdb"

        fun isRestoreAvailable(context: Context): Boolean {
            if (!isRootOperationAllowed(context)) {
                return false
            }
            val backupFile = GameChecker.filesDir.resolve("master/master.mdb.bak")
            return testFile(backupFile.path)
        }

        fun isPatched(context: Context): Boolean? {
            if (!isRootOperationAllowed(context)) {
                return null
            }

            val mdbFile = GameChecker.filesDir.resolve("master/master.mdb")
            // Get 4 bytes at position 60 (sqlite user version value)
            val result = Shell.cmd(
                "head -c 64 '${mdbFile.path}' | tail -c 4 | od -t x1 -An"
            ).exec()
            if (!result.isSuccess) return null

            val bytes = result.out.joinToString("").replace(" ", "")
            return bytes == "00000408"
        }

        fun getLastModifiedStr(context: Context): String? {
            if (!isRootOperationAllowed(context)) {
                return null
            }

            val mdbFile = GameChecker.filesDir.resolve("master/master.mdb")
            // Get 4 bytes at position 60 (sqlite user version value)
            val result = Shell.cmd(
                "date -r '${mdbFile.path}' '+%Y/%m/%d %H:%M'"
            ).exec()
            if (!result.isSuccess) return null

            return result.out.joinToString("")
        }
    }
}