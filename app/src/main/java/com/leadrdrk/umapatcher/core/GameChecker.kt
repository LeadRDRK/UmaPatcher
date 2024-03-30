package com.leadrdrk.umapatcher.core

import android.content.pm.PackageInfo
import android.content.pm.PackageManager

object GameChecker {
    private const val PACKAGE_NAME_JP = "jp.co.cygames.umamusume"

    private var _packageInfo: PackageInfo? = null
    private var _uid: Int? = null

    private fun populatePackageInfo(pm: PackageManager): Boolean {
        return try {
            _packageInfo = pm.getPackageInfo(PACKAGE_NAME_JP, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getPackageInfo(pm: PackageManager): PackageInfo? {
        if (_packageInfo == null) populatePackageInfo(pm)
        return _packageInfo
    }

    fun isPackageInstalled(pm: PackageManager): Boolean {
        return getPackageInfo(pm) != null
    }

    fun getUid(pm: PackageManager): Int? {
        if (_uid == null) {
            val packageInfo = getPackageInfo(pm)
            if (packageInfo != null)
                _uid = pm.getPackageUid(packageInfo.packageName, 0)
        }
        return _uid
    }
}