package com.shanalimughal.mentalhealthai.BackgroundTasks

import android.content.Context
import android.content.Intent

class DailySchedulerClass {
    companion object {
        fun scheduleDailyReset(context: Context) {
            val intent = Intent(context, DailyResetJobService::class.java)
            DailyResetJobService().enqueueWork(context, intent)
        }
    }
}
