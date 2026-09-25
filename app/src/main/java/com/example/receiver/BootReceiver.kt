package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.PlanovoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Rescheduling all upcoming alarms.")
            val app = context.applicationContext as? PlanovoApp ?: return
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    app.repository.rescheduleUpcomingNotifications()
                }
            }
        }
    }
}
