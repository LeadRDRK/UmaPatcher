package com.leadrdrk.umapatcher.core

import android.content.pm.PackageInfo
import android.content.pm.PackageManager

object GameChecker {
    private val packageNames = arrayOf(
        "jp.co.cygames.umamusume",
        "com.komoe.kmumamusumegp",
        "com.komoe.umamusumeofficial",
        "com.kakaogames.umamusume"
    )

    var currentPackageName: String? = null

    fun init(pm: PackageManager) {
        // Defaults to whichever version is installed
        for (packageName in packageNames) {
            currentPackageName = packageName
            if (getPackageInfo(pm) != null) {
                return
            }
        }
        // Otherwise, the jp ver
        currentPackageName = packageNames[0]
    }

    fun getPackageInfo(pm: PackageManager): PackageInfo? {
        val packageName = currentPackageName ?: return null
        return try {
            pm.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isPackageInstalled(pm: PackageManager): Boolean {
        return getPackageInfo(pm) != null
    }

    fun getAllPackageInfo(pm: PackageManager): Array<PackageInfo?> {
        return Array(packageNames.size) {
            try {
                pm.getPackageInfo(packageNames[it], 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}