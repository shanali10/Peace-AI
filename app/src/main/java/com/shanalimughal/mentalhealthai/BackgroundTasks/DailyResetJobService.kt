package com.shanalimughal.mentalhealthai.BackgroundTasks

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import java.util.Calendar

class DailyResetJobService : JobIntentService() {
    private var JOB_ID: Int = 1000

    fun enqueueWork(context: Context?, intent: Intent?) {
        enqueueWork(context!!, DailyResetJobService::class.java, JOB_ID, intent!!)
    }

    override fun onHandleWork(intent: Intent) {
        scheduleDailyReset(this)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, MidnightResetReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            getMidnightTime(),
            pendingIntent
        )
    }

    private fun getMidnightTime(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= currentTimeMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
