package com.shanalimughal.mentalhealthai.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shanalimughal.mentalhealthai.R

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private val splashException = "splashException"
    private val REQUEST_CODE_USE_EXACT_ALARM = 1001

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var isUserPreferencesSaved: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        isUserPreferencesSaved = sharedPreferences.getBoolean("isUserPreferencesOnBoard", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API level 34
            if (checkSelfPermission(Manifest.permission.USE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.USE_EXACT_ALARM),
                    REQUEST_CODE_USE_EXACT_ALARM
                )
            } else {
                proceedToNextActivity()
            }
        } else {
            proceedToNextActivity()
        }
    }

    private fun proceedToNextActivity() {
        Thread {
            run {
                try {
                    Thread.sleep(1500)
                } catch (e: Exception) {
                    e.message?.let { Log.v(splashException, it) }
                } finally {
                    val verification = Firebase.auth.currentUser?.isEmailVerified
                    if (FirebaseAuth.getInstance().currentUser != null && verification == true) {
                        if (!(isUserPreferencesSaved!!)) {
                            startActivity(Intent(this, UserPreferencesActivity::class.java))
                            finish()
                        } else {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                    }
                }
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_USE_EXACT_ALARM -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    proceedToNextActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Permission required to set exact alarms.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }
}
