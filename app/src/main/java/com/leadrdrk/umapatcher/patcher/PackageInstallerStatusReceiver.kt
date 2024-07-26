package com.leadrdrk.umapatcher.patcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PackageInstallerStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmationIntent != null) {
                    context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                contList.forEach { it.resume(true) }
                contList.clear()
            }
            else -> {
                contList.forEach { it.resume(false) }
                contList.clear()
            }
        }
    }

    companion object {
        val contList: MutableList<Continuation<Boolean>> = mutableListOf()
        suspend fun waitForInstallFinish(): Boolean {
            return suspendCoroutine { cont ->
                contList.add(cont)
            }
        }
    }
}
