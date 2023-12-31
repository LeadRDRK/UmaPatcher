package com.leadrdrk.umapatcher.patcher

import android.content.Context
import com.leadrdrk.umapatcher.core.GameChecker
import com.leadrdrk.umapatcher.utils.getUid
import com.topjohnwu.superuser.Shell

fun copyAndOwnFile(context: Context, src: String, dest: String): Shell.Result {
    val uid = context.getUid()
    return Shell.cmd(
        "cp '$src' '$dest'",
        "chown $uid:$uid '$dest'",
        "chmod 700 '$dest'"
    ).exec()
}

fun moveGameFile(context: Context, src: String, dest: String): Shell.Result {
    val uid = GameChecker.getUid(context.packageManager)!!
    return Shell.cmd(
        "mv '$src' '$dest'",
        "chown $uid:$uid '$dest'",
        "chmod 700 '$dest'"
    ).exec()
}

fun copyGameFile(context: Context, src: String, dest: String): Shell.Result {
    val uid = GameChecker.getUid(context.packageManager)!!
    return Shell.cmd(
        "cp '$src' '$dest'",
        "chown $uid:$uid '$dest'",
        "chmod 700 '$dest'"
    ).exec()
}

fun createGameDir(context: Context, path: String): Shell.Result {
    val uid = GameChecker.getUid(context.packageManager)!!
    return Shell.cmd(
        "mkdir -p '$path'",
        "chown $uid:$uid '$path'",
        "chmod 700 '$path'"
    ).exec()
}

fun isRootOperationAllowed(context: Context): Boolean {
    return Shell.isAppGrantedRoot() == true && GameChecker.isPackageInstalled(context.packageManager)
}

fun testFile(path: String): Boolean {
    return Shell.cmd("test -f '$path'").exec().isSuccess
}

fun testDirectory(path: String): Boolean {
    return Shell.cmd("test -d '$path'").exec().isSuccess
}

fun listDirectory(path: String): List<String>? {
    val result = Shell.cmd("ls -1 '$path'").exec()
    return if (result.isSuccess) result.out.filter { it.isNotEmpty() } else null
}

fun removeDirectoryIfEmpty(path: String): Boolean {
    // toybox's rm command doesn't support rm -d so check if its empty manually
    if (listDirectory(path)?.isNotEmpty() == true) return false
    return Shell.cmd("rm -r '$path'").exec().isSuccess
}

fun createDir(path: String): Shell.Result {
    return Shell.cmd(
        "mkdir -p '$path'"
    ).exec()
}

fun moveFile(src: String, dest: String): Shell.Result {
    return Shell.cmd(
        "mv '$src' '$dest'"
    ).exec()
}

fun copyFile(src: String, dest: String): Shell.Result {
    return Shell.cmd(
        "cp '$src' '$dest'"
    ).exec()
}

fun copyDirectoryContents(src: String, dest: String): Shell.Result {
    return Shell.cmd(
        "cp -R '$src/.' '$dest'"
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

fun writeScript(path: String, content: String): Shell.Result {
    return Shell.cmd(
        "echo '$content' > '$path'",
        "chmod 744 '$path'"
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