package com.shanalimughal.mentalhealthai.BackgroundTasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userPrefs = "userPrefs"
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(userPrefs, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("dailyMood", false)
        editor.apply()
    }
}
