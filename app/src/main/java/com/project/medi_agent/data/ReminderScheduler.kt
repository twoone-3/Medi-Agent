package com.project.medi_agent.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.project.medi_agent.ui.MedicationReminder
import java.util.Calendar

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun schedule(reminder: MedicationReminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_MEDICINE_NAME", reminder.medicineName)
            putExtra("EXTRA_DOSAGE", reminder.dosage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val timeParts = reminder.time.split(":")
        if (timeParts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].trim().toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].trim().toInt())
            calendar.set(Calendar.SECOND, 0)

            // 如果设定的时间已经过了，就设为明天
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("ReminderScheduler", "已为 ${reminder.medicineName} 设置闹钟: ${reminder.time}")
            } catch (e: SecurityException) {
                Log.e("ReminderScheduler", "无法设置精确闹钟，缺少权限: ${e.message}")
            }
        }
    }

    fun cancel(reminder: MedicationReminder) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("ReminderScheduler", "已取消 ${reminder.medicineName} 的闹钟")
    }
}
