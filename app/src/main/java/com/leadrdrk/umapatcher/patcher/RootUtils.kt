package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.core.GameChecker
import com.topjohnwu.superuser.Shell

object RootUtils {
    fun isRootOperationAllowed(context: Context): Boolean {
        return Shell.isAppGrantedRoot() == true && GameChecker.isPackageInstalled(context.packageManager)
    }

    fun testFile(path: String): Boolean {
        return Shell.cmd("test -f '$path'").exec().isSuccess
    }

    fun testDirectory(path: String): Boolean {
        return Shell.cmd("test -d '$path'").exec().isSuccess
    }

    fun createDirectory(path: String): Shell.Result {
        return Shell.cmd(
            "mkdir -p '$path'"
        ).exec()
    }

    fun removeDirectory(path: String): Shell.Result {
        return Shell.cmd(
            "rm -rf '$path'"
        ).exec()
    }

    fun moveFile(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "mv '$src' '$dest'"
        ).exec()
    }

    fun chmod(path: String, perm: String): Shell.Result {
        return Shell.cmd(
            "chmod $perm '$path'"
        ).exec()
    }

    fun chown(path: String, owner: String): Shell.Result {
        return Shell.cmd(
            "chown $owner '$path'"
        ).exec()
    }

    fun copyGameLibrary(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "cp '$src' '$dest'",
            "chown system:system '$dest'",
            "chmod 755 '$dest'"
        ).exec()
    }

    fun moveGameLibrary(src: String, dest: String): Shell.Result {
        return Shell.cmd(
            "mv '$src' '$dest'",
            "chown system:system '$dest'",
            "chmod 755 '$dest'"
        ).exec()
    }
}