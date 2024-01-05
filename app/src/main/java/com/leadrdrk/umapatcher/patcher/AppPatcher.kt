package com.leadrdrk.umapatcher.patcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import com.iyxan23.zipalignjava.ZipAlign
import com.leadrdrk.umapatcher.R
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.core.GitHubReleases
import com.leadrdrk.umapatcher.core.PrefKey
import com.leadrdrk.umapatcher.core.dataStore
import com.leadrdrk.umapatcher.core.getPrefValue
import com.leadrdrk.umapatcher.utils.bytesToHex
import com.leadrdrk.umapatcher.utils.copyDirectory
import com.leadrdrk.umapatcher.utils.createDirectoryOverwrite
import com.leadrdrk.umapatcher.utils.downloadFileAndDigestSHA1
import com.leadrdrk.umapatcher.utils.fetchJson
import com.leadrdrk.umapatcher.utils.repoDir
import com.leadrdrk.umapatcher.utils.workDir
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL

private const val LIBS_REPO_PATH = "LeadRDRK/Carrotless"
private const val ARMV8A_LIB_NAME = "libmain-arm64-v8a.so"
private const val ARMV7A_LIB_NAME = "libmain-armeabi-v7a.so"
private const val ARMV8A_LIB_DIR = "lib/arm64-v8a"
private const val ARMV7A_LIB_DIR = "lib/armeabi-v7a"

private const val MOUNT_INSTALL_PATH = "/data/adb/umapatcher"

class AppPatcher(
    private val fileUri: Uri? = null,
    private val mountInstall: Boolean = true,
    private val runInstallData: Boolean = false
): Patcher() {
    override fun run(context: Context): Boolean {
        if (runInstallData)
            return installData(context)

        if (mountInstall && !isDirectInstallAllowed(context))
            return false

        /* Download and keep Carrotless up to date */
        val libVer = runBlocking { syncLibraries(context) } ?: return false
        log(context.getString(R.string.using_app_lib_ver).format(libVer))

        val apkFile = context.workDir.resolve("base.apk")

        val failedToReadFileStr = context.getString(R.string.failed_to_read_file)

        /* Copy APK file */
        task = context.getString(R.string.copying_file).format("base.apk")
        var packageInfo: PackageInfo? = null
        if (fileUri != null) {
            context.contentResolver.openInputStream(fileUri).use { input ->
                if (input == null) {
                    log(failedToReadFileStr.format("base.apk"))
                    return false
                }

                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        else {
            // Unmount APK before copying it
            if (isApkMounted(context)) {
                log(context.getString(R.string.unmounting_apk_file))
                unmountApk(context)
            }
            packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val srcFile = File(packageInfo.applicationInfo.publicSourceDir)
            try {
                srcFile.inputStream().use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            catch (ex: Exception) {
                Log.e("AppPatcher", "Exception", ex)
                log(failedToReadFileStr.format("base.apk"))
                return false
            }
        }

        task = context.getString(R.string.patching_apk_file)

        val libsDir = context.filesDir.resolve("libs")
        val armv8Lib = libsDir.resolve(ARMV8A_LIB_NAME)
        val armv7Lib = libsDir.resolve(ARMV7A_LIB_NAME)
        try {
            ZipFile(apkFile).use { zip ->
                if (!zip.isValidZipFile) throw IOException()

                val armv8LibPath = "$ARMV8A_LIB_DIR/libmain.so"
                val armv7LibPath = "$ARMV7A_LIB_DIR/libmain.so"
                val armv8LibNewPath = "$ARMV8A_LIB_DIR/libmain_orig.so"
                val armv7LibNewPath = "$ARMV7A_LIB_DIR/libmain_orig.so"

                if (zip.getFileHeader(armv8LibNewPath) == null) {
                    log(armv8LibNewPath)
                    zip.renameFile(armv8LibPath, armv8LibNewPath)
                }
                progress = 1 / 4f

                if (zip.getFileHeader(armv7LibNewPath) == null) {
                    log(armv7LibNewPath)
                    zip.renameFile(armv7LibPath, armv7LibNewPath)
                }
                progress = 2 / 4f

                log(armv8LibPath)
                val armv8Param = ZipParameters().also { it.fileNameInZip = armv8LibPath }
                zip.addFile(armv8Lib, armv8Param)
                progress = 3 / 4f

                log(armv7LibPath)
                val armv7Param = ZipParameters().also { it.fileNameInZip = armv7LibPath }
                zip.addFile(armv7Lib, armv7Param)
                progress = 1f
            }
        }
        catch (ex: Exception) {
            Log.e("AppPatcher", "Exception", ex)
            log(context.getString(R.string.failed_to_patch_apk_file).format(ex.javaClass.name))
            apkFile.delete()
            return false
        }

        /* Align APK */
        progress = 0f
        task = context.getString(R.string.aligning_apk_file)

        val alignedApkFile = context.workDir.resolve("base-aligned.apk")
        alignedApkFile.outputStream().use { output ->
            ZipAlign.alignZip(RandomAccessFile(apkFile, "r"), output)
        }
        apkFile.delete()

        /* Install */
        if (mountInstall) {
            task = context.getString(R.string.installing)

            if (packageInfo == null) packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            val installPath = "$MOUNT_INSTALL_PATH/$packageName"
            val installApkPath = "$installPath/base.apk"

            val res =
                createDir(installPath).isSuccess &&
                moveFile(alignedApkFile.path, installApkPath).isSuccess &&
                chown(installApkPath, "system:system").isSuccess &&
                chmod(installApkPath, "644").isSuccess &&
                writeScript("$installPath/mount.sh", getMountScript(packageName)).isSuccess &&
                runMountScript(context, packageName)

            // Replace system's cached/extracted library files if needed
            val appApkDir = File(packageInfo.applicationInfo.publicSourceDir).parentFile!!
            val armv8LibDir = appApkDir.resolve("lib/arm64")
            val armv7LibDir = appApkDir.resolve("lib/arm")
            if (testDirectory(armv8LibDir.path)) {
                val srcArmv8Lib = armv8LibDir.resolve("libmain.so")
                val origArmv8Lib = armv8LibDir.resolve("libmain_orig.so")
                if (!testFile(origArmv8Lib.path)) {
                    moveGameLibrary(srcArmv8Lib.path, origArmv8Lib.path)
                }
                copyGameLibrary(armv8Lib.path, srcArmv8Lib.path)
            }
            else if (testDirectory(armv7LibDir.path)) {
                val srcArmv7Lib = armv7LibDir.resolve("libmain.so")
                val origArmv7Lib = armv7LibDir.resolve("libmain_orig.so")
                if (!testFile(origArmv7Lib.path)) {
                    moveGameLibrary(srcArmv7Lib.path, origArmv7Lib.path)
                }
                copyGameLibrary(armv7Lib.path, srcArmv7Lib.path)
            }

            if (res) {
                log(context.getString(R.string.install_completed))
                log(context.getString(R.string.mount_install_note))
            }
            else log(context.getString(R.string.install_failed))
        }
        else {
            /* Sign APK */
            task = context.getString(R.string.signing_apk_file)
            val signedApkFile = context.workDir.resolve("base-signed.apk")
            val apkSigner = ApkSigner("UmaPatcher", "securep@ssw0rd816-n")
            val ksFile = context.filesDir.resolve("keystore.bks")
            apkSigner.signApk(alignedApkFile, signedApkFile, ksFile)
            alignedApkFile.delete()

            task = context.getString(R.string.installing)
            saveFile("patched.apk", signedApkFile) {
                signedApkFile.delete()
                log(context.getString(R.string.install_completed))
            }
        }

        return true
    }

    private fun getAssetDownloadUrl(assets: List<Map<String, Any>>, name: String) =
        assets.find { it["name"] as String == name }?.get("browser_download_url") as String?

    private suspend fun syncLibraries(context: Context): String? {
        task = context.getString(R.string.syncing_app_libs)

        val libsDir = context.filesDir.resolve("libs")
        if (!libsDir.exists()) libsDir.mkdir() || return null

        val currentVer = context.getPrefValue(PrefKey.APP_LIBS_VERSION) as String?

        // Try syncing the libraries
        try {
            val releases = GitHubReleases(LIBS_REPO_PATH)
            val latest = releases.fetchLatest()
            val tagName = latest["tag_name"] as String

            if (tagName == currentVer) {
                // Already up to date
                return tagName
            }

            val assets = (latest["assets"] as ArrayList<*>).filterIsInstance<Map<String, Any>>()
            if (assets.size < 3) throw RuntimeException()

            val armv8LibUrl = URL(getAssetDownloadUrl(assets, ARMV8A_LIB_NAME)!!)
            val armv7LibUrl = URL(getAssetDownloadUrl(assets, ARMV7A_LIB_NAME)!!)
            val sha1Url = URL(getAssetDownloadUrl(assets, "sha1.json")!!)

            // First fetch the sha1 json
            val hashes = fetchJson(sha1Url)

            // Download the libraries to temporary files
            val workDir = context.workDir
            val armv8LibTmp = workDir.resolve(ARMV8A_LIB_NAME)
            val armv7LibTmp = workDir.resolve(ARMV7A_LIB_NAME)

            val armv8Sha1 = bytesToHex(downloadFileAndDigestSHA1(armv8LibUrl, armv8LibTmp))
            val armv7Sha1 = bytesToHex(downloadFileAndDigestSHA1(armv7LibUrl, armv7LibTmp))

            // Check their hashes
            if (armv8Sha1 != hashes[ARMV8A_LIB_NAME] || armv7Sha1 != hashes[ARMV7A_LIB_NAME]) {
                log(context.getString(R.string.corrupted_file_abort_download))
                armv8LibTmp.delete()
                armv7LibTmp.delete()
                throw IOException()
            }

            // Move the files to their destination
            val armv8Lib = libsDir.resolve(ARMV8A_LIB_NAME)
            val armv7Lib = libsDir.resolve(ARMV7A_LIB_NAME)

            armv8LibTmp.renameTo(armv8Lib)
            armv7LibTmp.renameTo(armv7Lib)

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

    @SuppressLint("SdCardPath")
    private fun getMountScript(packageName: String) = """
        until [ "${'$'}(getprop sys.boot_completed)" = 1 ]; do sleep 3; done
        until [ -d "/sdcard/Android" ]; do sleep 1; done
        
        # Unmount any existing installation to prevent multiple unnecessary mounts.
        grep $packageName /proc/mounts | while read -r line; do echo ${'$'}line | cut -d " " -f 2 | sed "s/apk.*/apk/" | xargs -r umount -l; done

        base_path=$MOUNT_INSTALL_PATH/$packageName/base.apk
        stock_path=${'$'}(pm path $packageName | grep base | sed "s/package://g" )

        chcon u:object_r:apk_data_file:s0 ${'$'}base_path
        mount -o bind ${'$'}base_path ${'$'}stock_path

        # Kill the app to force it to restart the mounted APK in case it is already running
        am force-stop $packageName
    """.trimIndent()

    @SuppressLint("SdCardPath")
    fun installData(context: Context): Boolean {
        val mediaDir = DocumentFile.fromTreeUri(context, fileUri!!)
        if (mediaDir == null) {
            log(context.getString(R.string.failed_to_open_dir).format("Android/media"))
            return false
        }

        val src = context.repoDir.resolve(APP_TRANSLATIONS_PATH)
        if (!src.exists()) {
            log(context.getString(R.string.translation_dir_not_exist))
            return false
        }

        val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
        val packageName = packageInfo.packageName
        val dest = mediaDir.createDirectoryOverwrite(packageName)
        if (dest == null) {
            log(context.getString(R.string.failed_to_create_dir).format(packageName))
            return false
        }

        val success = copyDirectory(context, src, dest) {
            log(it)
        }
        if (!success) log(
            context.getString(R.string.failed_to_copy_dir).format(APP_TRANSLATIONS_PATH)
        )

        return success
    }

    companion object {
        const val APP_TRANSLATIONS_PATH = "localify"

        fun runMountScript(context: Context): Boolean {
            if (!isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            return runMountScript(context, packageName)
        }

        fun runMountScript(context: Context, packageName: String): Boolean {
            if (!isRootOperationAllowed(context)) return false
            return Shell.cmd("$MOUNT_INSTALL_PATH/$packageName/mount.sh").exec().isSuccess
        }

        fun isApkMounted(context: Context): Boolean {
            if (!isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            val res = Shell.cmd("grep $packageName /proc/mounts").exec()
            return res.out.joinToString("").isNotEmpty()
        }

        fun unmountApk(context: Context): Boolean {
            if (!isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            return Shell.cmd(
                "grep $packageName /proc/mounts | while read -r line; do echo ${'$'}line | cut -d \" \" -f 2 | sed \"s/apk.*/apk/\" | xargs -r umount -l; done"
            ).exec().isSuccess
        }

        fun isMountInstalled(context: Context): Boolean {
            if (!isRootOperationAllowed(context)) return false
            val packageInfo = GameChecker.getPackageInfo(context.packageManager)!!
            val packageName = packageInfo.packageName
            return testDirectory("$MOUNT_INSTALL_PATH/$packageName")
        }

        fun requestDataPermission(launcher: ActivityResultLauncher<Uri?>) {
            launcher.launch(
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Android/media"
                )
            )
        }
    }
}