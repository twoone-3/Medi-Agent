package com.project.medi_agent.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.project.medi_agent.ui.ReminderActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("EXTRA_MEDICINE_NAME") ?: "药品"
        val dosage = intent.getStringExtra("EXTRA_DOSAGE") ?: ""
        
        Log.d("ReminderReceiver", "收到闹钟广播: $medicineName")

        // 启动全屏提醒 Activity
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MEDICINE_NAME", medicineName)
            putExtra("EXTRA_DOSAGE", dosage)
        }
        
        context.startActivity(fullScreenIntent)
    }
}
