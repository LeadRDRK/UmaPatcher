package com.leadrdrk.umapatcher.patcher

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.core.GitHubReleases
import com.leadrdrk.umapatcher.core.PrefKey
import com.leadrdrk.umapatcher.core.dataStore
import com.leadrdrk.umapatcher.core.getPrefValue
import com.leadrdrk.umapatcher.utils.bytesToHex
import com.leadrdrk.umapatcher.utils.downloadFileAndDigestSHA1
import com.leadrdrk.umapatcher.utils.fetchJson
import com.leadrdrk.umapatcher.utils.ksFile
import com.leadrdrk.umapatcher.utils.workDir
import com.leadrdrk.umapatcher.zip.ZipExtractor
import com.reandroid.apk.ApkModule
import com.reandroid.archive.Archive
import com.reandroid.archive.FileInputSource
import com.reandroid.archive.ZipEntryMap
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.io.IOException
import java.net.URL


private const val LIBS_REPO_PATH = "Hachimi-Hachimi/Hachimi"

private const val MOD_ARM64_LIB_NAME = "libmain-arm64-v8a.so"
private const val APK_ARM64_LIB_DIR = "lib/arm64-v8a"
private const val APK_ARM64_LIB_PATH = "$APK_ARM64_LIB_DIR/libmain.so"
private const val APK_ORIG_ARM64_LIB_PATH = "$APK_ARM64_LIB_DIR/libmain_orig.so"

private const val MOD_ARM_LIB_NAME = "libmain-armeabi-v7a.so"
private const val APK_ARM_LIB_DIR = "lib/armeabi-v7a"
private const val APK_ARM_LIB_PATH  = "$APK_ARM_LIB_DIR/libmain.so"
private const val APK_ORIG_ARM_LIB_PATH = "$APK_ARM_LIB_DIR/libmain_orig.so"

private const val LEGACY_MOUNT_SCRIPT_DIR = "/data/adb/umapatcher"

private val Context.libsDir: File
    get() = filesDir.resolve("libs")

private val Context.modArm64Lib: File
    get() = libsDir.resolve(MOD_ARM64_LIB_NAME)

private val Context.modArmLib: File
    get() = libsDir.resolve(MOD_ARM_LIB_NAME)

private val Context.apkExtractDir: File
    get() = workDir.resolve("apk_extract")

private val Context.xapkExtractDir: File
    get() = workDir.resolve("xapk_extract")

class AppPatcher(
    private val fileUris: Array<Uri>,
    private val install: Boolean,
    private val directInstall: Boolean
): Patcher() {
    override fun run(context: Context): Boolean {
        if (directInstall && !isDirectInstallAllowed(context))
            return false

        val libVer = runBlocking { syncModLibs(context) } ?: return false
        log(context.getString(R.string.using_app_lib_ver).format(libVer))

        if (directInstall)
            return runDirectInstall(context)

        if (fileUris.size == 1) {
            return runBlocking {
                runXapkOrFullApk(context, (copyInputFiles(context) ?: return@runBlocking false)[0])
            }
        }
        else if (fileUris.size > 1) {
            return runBlocking { runSplitApks(context) }
        }

        return false
    }

    private fun runDirectInstall(context: Context): Boolean {
        // Check and remove legacy mount script
        if (RootUtils.testDirectory(LEGACY_MOUNT_SCRIPT_DIR)) {
            task = context.getString(R.string.removing_legacy_files)
            progress = -1f

            if (isApkMounted(context)) unmountApk(context)
            RootUtils.removeDirectory(LEGACY_MOUNT_SCRIPT_DIR)
        }

        task = context.getString(R.string.installing)
        progress = -1f

        val modArm64Lib = context.modArm64Lib
        val modArmLib = context.modArmLib

        val packageInfo = GameChecker.getPackageInfo(context.packageManager) ?: return false
        val appApkDir = File(packageInfo.applicationInfo.publicSourceDir).parentFile ?: return false

        val arm64LibDir = appApkDir.resolve("lib/arm64")
        val armLibDir = appApkDir.resolve("lib/arm")

        if (RootUtils.testDirectory(arm64LibDir.path)) {
            installModLib(modArm64Lib, arm64LibDir)
        }
        else if (RootUtils.testDirectory(armLibDir.path)) {
            installModLib(modArmLib, armLibDir)
        }
        else {
            log(context.getString(R.string.app_lib_dir_not_found))
            return false
        }

        return true
    }

    private fun installModLib(modLib: File, libDir: File): Boolean {
        val lib = libDir.resolve("libmain.so")
        val origLib = libDir.resolve("libmain_orig.so")
        if (!RootUtils.testFile(origLib.path)) {
            RootUtils.moveGameLibrary(lib.path, origLib.path)
                .isSuccess || return false
        }
        RootUtils.copyGameLibrary(modLib.path, lib.path)
            .isSuccess || return false

        return true
    }

    private suspend fun runXapkOrFullApk(context: Context, file: File): Boolean {
        val fileHeaders = ZipFile(file).use { it.fileHeaders }
        val zip = ZipExtractor.maybeMapped(file)

        // Detect file type without extracting first
        for (header in fileHeaders) {
            if (header.fileName.endsWith(".apk")) {
                log(context.getString(R.string.detected_xapk_file))
                return runXapk(context, zip)
            }
            else if (header.fileName == "classes.dex") {
                log(context.getString(R.string.detected_apk_file))
                return runFullApk(context, zip)
            }
        }

        zip.close()
        log(context.getString(R.string.invalid_apk_file))
        return false
    }

    private suspend fun runXapk(context: Context, zip: ZipExtractor): Boolean {
        // Extract the XAPK file
        val extractDir = context.xapkExtractDir
        extractZipWithProgress(context, zip, extractDir)
        zip.close()
        zip.file.delete()

        // Patch the split APKs
        try {
            val apkFiles = extractDir.listFiles { _, name -> name.endsWith(".apk") } ?: return false
            if (!patchSplitApks(context, apkFiles))
                return false

            // Install or save
            return if (install)
                installApks(context, apkFiles)
            else
                createAndSaveXapk(context, apkFiles)
        }
        catch (ex: Exception) {
            logException(ex)
            return false
        }
        finally {
            extractDir.deleteRecursively()
        }
    }

    private suspend fun runFullApk(context: Context, zip: ZipExtractor): Boolean {
        try {
            if (!patchApk(context, zip))
                return false

            return if (install) {
                installApks(context, arrayOf(zip.file))
            }
            else {
                val success = saveFile("patched-${System.currentTimeMillis()}.apk", zip.file)
                log(
                    if (success) context.getString(R.string.file_saved)
                    else context.getString(R.string.failed_to_save_file)
                )
                success
            }
        }
        catch (ex: Exception) {
            logException(ex)
            return false
        }
        finally {
            zip.close()
            zip.file.delete()
        }
    }

    private suspend fun runSplitApks(context: Context): Boolean {
        log(context.getString(R.string.patching_as_split_apks))
        val files = copyInputFiles(context, ".apk") ?: return false
        try {
            if (!patchSplitApks(context, files))
                return false

            return if (install)
                installApks(context, files)
            else
                createAndSaveXapk(context, files)
        }
        catch (ex: Exception) {
            logException(ex)
            return false
        }
        finally {
            files.forEach { it.delete() }
        }
    }

    private fun copyInputFiles(context: Context, ext: String = ""): Array<File>? {
        return Array(fileUris.size) {
            val filename = "file$it$ext"
            val file = context.workDir.resolve(filename)

            task = if (fileUris.size > 1) context.getString(R.string.copying_file_name).format(filename)
                else context.getString(R.string.copying_file)
            progress = -1f

            context.contentResolver.openInputStream(fileUris[it]).use { input ->
                if (input == null) {
                    log(context.getString(R.string.failed_to_read_file).format(filename))
                    return null
                }

                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file
        }
    }

    private enum class SplitApkType {
        BASE,
        CONFIG_ARM,
        CONFIG_ARM64
    }

    private suspend fun patchSplitApks(context: Context, files: Array<File>): Boolean {
        val extractDir = context.apkExtractDir
        val res = run {
            var success = true
            useZipExtractors(files) { zipFiles ->
                val processedSplits = mutableMapOf<SplitApkType, Boolean>()
                for (zip in zipFiles) {
                    try {
                        if (!runBlocking { patchApk(context, zip, processedSplits) }) {
                            success = false
                            return@useZipExtractors
                        }
                    }
                    catch (ex: Exception) {
                        logException(ex)
                        success = false
                        return@useZipExtractors
                    }
                }

                if (processedSplits[SplitApkType.BASE] != true) {
                    log(context.getString(R.string.apk_splits_missing_base))
                    success = false
                }

                if (processedSplits[SplitApkType.CONFIG_ARM64] != true &&
                    processedSplits[SplitApkType.CONFIG_ARM] != true)
                {
                    log(context.getString(R.string.apk_files_missing_lib))
                    success = false
                }
            }

            success
        }

        extractDir.deleteRecursively()
        return res
    }

    private fun useZipExtractors(files: Array<File>, callback: (Array<ZipExtractor>) -> Unit) {
        val zipFiles = Array(files.size) {
            ZipExtractor.maybeMapped(files[it])
        }
        callback(zipFiles)
        zipFiles.forEach { it.close() }
    }

    private suspend fun extractZipWithProgress(context: Context, zip: ZipExtractor, extractDir: File) {
        task = context.getString(R.string.extracting_file).format(zip.file.name)
        progress = -1f

        extractDir.deleteRecursively()
        zip.extractAll(extractDir) { progress = it }
    }

    private suspend fun monitorZipProgress(zip: ZipFile) {
        val progressMonitor = zip.progressMonitor
        while (!progressMonitor.state.equals(ProgressMonitor.State.READY)) {
            progress = progressMonitor.percentDone / 100f
            delay(100)
        }
    }

    private suspend fun patchApk(
        context: Context,
        zip: ZipExtractor,
        processedSplits: MutableMap<SplitApkType, Boolean>? = null
    ): Boolean {
        // Build do not compress list
        val doNotCompress = zip.fileHeaders
            .filter { it.compressionMethod == CompressionMethod.STORE }
            .map { it.fileName }
            .toSet()

        // Extract file
        val file = zip.file
        val filename = file.name

        val extractDir = context.apkExtractDir
        extractZipWithProgress(context, zip, extractDir)
        zip.close()
        file.delete()

        // Check manifest
        if (!extractDir.resolve("AndroidManifest.xml").isFile) {
            log(context.getString(R.string.invalid_apk_file_name).format(filename))
            return false
        }

        // Detect apk type / Patch the lib files
        task = context.getString(R.string.patching)
        progress = -1f

        val arm64Lib = extractDir.resolve(APK_ARM64_LIB_PATH)
        val armLib = extractDir.resolve(APK_ARM_LIB_PATH)
        val classesDex = extractDir.resolve("classes.dex")
        var processed = false
        var libPatched = false

        fun isInvalidSplit(splitType: SplitApkType): Boolean {
            if (processedSplits == null) return false
            log(context.getString(R.string.detected_apk_split).format(splitType.name))

            if (processed) {
                log(context.getString(R.string.invalid_split_apk_multiple_type))
                return true
            }

            if (processedSplits[splitType] == true) {
                log(
                    context.getString(R.string.invalid_apk_splits_duplicate)
                        .format(splitType.name)
                )
                return true
            }

            return false
        }

        fun patchArchLibs(
            splitType: SplitApkType,
            lib: File,
            modLib: File,
            origLibPath: String
        ): Boolean {
            if (isInvalidSplit(splitType)) return false

            // Only create the orig lib if it hasn't existed yet to allow updating an already patched apk
            val origLib = extractDir.resolve(origLibPath)
            if (!origLib.exists()) {
                lib.renameTo(origLib)
            }
            modLib.copyTo(lib)

            log(context.getString(R.string.libraries_patched).format(lib.parentFile!!.name))
            processed = true
            libPatched = true
            processedSplits?.put(splitType, true)

            return true
        }

        if (arm64Lib.exists()) {
            patchArchLibs(
                splitType = SplitApkType.CONFIG_ARM64,
                lib = arm64Lib,
                modLib = context.modArm64Lib,
                origLibPath = APK_ORIG_ARM64_LIB_PATH
            ) || return false
        }

        if (armLib.exists()) {
            patchArchLibs(
                splitType = SplitApkType.CONFIG_ARM,
                lib = armLib,
                modLib = context.modArmLib,
                origLibPath = APK_ORIG_ARM_LIB_PATH
            ) || return false
        }

        if (classesDex.exists()) {
            val splitType = SplitApkType.BASE
            if (isInvalidSplit(splitType)) return false
            processed = true
            processedSplits?.put(splitType, true)
        }

        if (!processed) {
            log(context.getString(R.string.invalid_apk_file_name).format(filename))
            return false
        }

        // Check if libs are patched if this is a full apk
        if (processedSplits == null && !libPatched) {
            log(context.getString(R.string.apk_files_missing_lib))
            return false
        }

        // Create new apk file
        task = context.getString(R.string.creating_file).format(filename)
        progress = -1f

        val zipEntryMap = ZipEntryMap()
        val prefixLen = extractDir.canonicalPath.length + 1
        var fileCount = 0
        extractDir.walkTopDown().forEach { child ->
            if (child.isFile) {
                val name = child.canonicalPath.substring(prefixLen)
                val inputSource = FileInputSource(child, name).apply {
                    if (doNotCompress.contains(name)) method = Archive.STORED
                }
                zipEntryMap.add(inputSource)
                ++fileCount
            }
        }

        val apk = ApkModule(zipEntryMap)
        var writtenFiles = 0
        apk.writeApk(file) { path, _, _ ->
            log(path)
            progress = ++writtenFiles / fileCount.toFloat()
        }

        // Sign APK file
        task = context.getString(R.string.signing_apk_file).format(filename)
        progress = -1f

        val signedApkFile = context.workDir.resolve("tmp_signed.apk")
        val apkSigner = ApkSigner("UmaPatcher", "securep@ssw0rd816-n")
        apkSigner.signApk(file, signedApkFile, context.ksFile)
        if (!signedApkFile.renameTo(file)) {
            log(context.getString(R.string.failed_to_move_file).format(signedApkFile.name))
            return false
        }

        // we're finally done :')
        return true
    }

    private suspend fun installApks(context: Context, files: Array<File>): Boolean {
        task = context.getString(R.string.staging_app)
        progress = -1f

        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        sessionParams.setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
        }

        val packageInstaller = context.packageManager.packageInstaller
        val sessionId: Int = packageInstaller.createSession(sessionParams)

        packageInstaller.openSession(sessionId).use { session ->
            try {
                var i = 0
                for (file in files) {
                    val length = file.length()
                    file.inputStream().use { input ->
                        session.openWrite("${i++}.apk", 0, length).use { output ->
                            log(context.getString(R.string.copying_file_name).format(file.name))
                            copyStreamProgress(input, output, length)
                            progress = -1f
                            session.fsync(output)
                        }
                    }
                }

                task = context.getString(R.string.installing)

                val callbackIntent = Intent(context, PackageInstallerStatusReceiver::class.java)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        callbackIntent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                session.commit(pendingIntent.intentSender)
            }
            catch (ex: Exception) {
                log(context.getString(R.string.install_failed))
                logException(ex)
                session.abandon()
                return false
            }
        }

        val success = PackageInstallerStatusReceiver.waitForInstallFinish()
        log(context.getString(
            if (success) R.string.install_completed
            else R.string.install_failed
        ))
        return success
    }

    private suspend fun createAndSaveXapk(context: Context, files: Array<File>): Boolean {
        task = context.getString(R.string.creating_file).format("patched.xapk")
        progress = -1f

        val xapkFile = context.workDir.resolve("patched.xapk")
        xapkFile.delete()

        val zip = ZipFile(xapkFile)
        zip.isRunInThread = true
        zip.addFiles(files.toMutableList(), ZipParameters().apply {
            compressionMethod = CompressionMethod.STORE
        })
        monitorZipProgress(zip)
        zip.close()

        val success = saveFile("patched-${System.currentTimeMillis()}.xapk", xapkFile)
        log(
            if (success) context.getString(R.string.file_saved)
            else context.getString(R.string.failed_to_save_file)
        )

        task = context.getString(R.string.cleaning_up)
        progress = -1f
        xapkFile.delete()

        return success
    }

    private fun getAssetDownloadUrl(assets: List<Map<String, Any>>, name: String) =
        assets.find { it["name"] as String == name }?.get("browser_download_url") as String?

    private suspend fun syncModLibs(context: Context): String? {
        task = context.getString(R.string.syncing_app_libs)

        val libsDir = context.libsDir
        if (!libsDir.exists()) libsDir.mkdir() || return null

        val currentVer = context.getPrefValue(PrefKey.APP_LIBS_VERSION) as String?

        // Try syncing the libraries
        try {
            val releases = GitHubReleases(LIBS_REPO_PATH)
            val latest = releases.fetchLatest()
            val tagName = latest["tag_name"] as String

            val arm64Lib = context.modArm64Lib
            val armLib = context.modArmLib

            if (tagName == currentVer && arm64Lib.exists() && armLib.exists()) {
                // Already up to date
                return tagName
            }

            val assets = (latest["assets"] as ArrayList<*>).filterIsInstance<Map<String, Any>>()
            val arm64LibUrl = URL(
                getAssetDownloadUrl(assets, MOD_ARM64_LIB_NAME) ?:
                throw RuntimeException("ARM64 lib asset not found")
            )
            val armLibUrl = URL(
                getAssetDownloadUrl(assets, MOD_ARM_LIB_NAME) ?:
                throw RuntimeException("ARM lib asset not found")
            )
            val sha1Url = URL(
                getAssetDownloadUrl(assets, "sha1.json") ?:
                throw RuntimeException("SHA1 hash asset not found")
            )

            // First fetch the sha1 json
            val hashes = fetchJson(sha1Url)

            // Download the libraries to temporary files
            val workDir = context.workDir
            val arm64LibTmp = workDir.resolve(MOD_ARM64_LIB_NAME)
            val armLibTmp = workDir.resolve(MOD_ARM_LIB_NAME)

            log(context.getString(R.string.downloading_file).format(MOD_ARM64_LIB_NAME))
            progress = -1f
            val arm64Sha1 = bytesToHex(downloadFileAndDigestSHA1(arm64LibUrl, arm64LibTmp) {
                progress = it
            })

            log(context.getString(R.string.downloading_file).format(MOD_ARM_LIB_NAME))
            progress = -1f
            val armSha1 = bytesToHex(downloadFileAndDigestSHA1(armLibUrl, armLibTmp) {
                progress = it
            })

            // Check their hashes
            if (arm64Sha1 != hashes[MOD_ARM64_LIB_NAME] || armSha1 != hashes[MOD_ARM_LIB_NAME]) {
                log(context.getString(R.string.corrupted_file_abort_download))
                arm64LibTmp.delete()
                armLibTmp.delete()
                throw IOException()
            }

            // Move the files to their destination
            arm64LibTmp.renameTo(arm64Lib) || throw RuntimeException("Failed to move ARM64 lib")
            armLibTmp.renameTo(armLib) || throw RuntimeException("Failed to move ARM lib")

            // Update version string
            context.dataStore.edit { preferences ->
                preferences[PrefKey.APP_LIBS_VERSION] = tagName
            }

            log(context.getString(R.string.app_lib_updated))
            return tagName
        }
        catch (ex: Exception) {
            Log.e("AppPatcher", "Exception", ex)

            // If libraries are already downloaded then let it continue
            if (currentVer != null) {
                log(context.getString(R.string.failed_to_sync_app_libs))
                return currentVer
            }

            log(context.getString(R.string.failed_to_download_app_libs))
            return null
        }
    }

    companion object {
        fun isApkMounted(context: Context): Boolean {
            if (!RootUtils.isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            val res = Shell.cmd("grep $packageName /proc/mounts").exec()
            return res.out.joinToString("").isNotEmpty()
        }

        fun unmountApk(context: Context): Boolean {
            if (!RootUtils.isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            return Shell.cmd(
                "grep $packageName /proc/mounts | while read -r line; do echo ${'$'}line | cut -d \" \" -f 2 | sed \"s/apk.*/apk/\" | xargs -r umount -l; done"
            ).exec().isSuccess
        }
    }
}