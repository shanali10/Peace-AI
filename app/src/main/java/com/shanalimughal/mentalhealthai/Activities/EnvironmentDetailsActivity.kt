package com.shanalimughal.mentalhealthai.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shanalimughal.mentalhealthai.databinding.ActivityEnvironmentDetailsBinding
import java.util.Locale

class EnvironmentDetailsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityEnvironmentDetailsBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var envDescriptionReceived: String
    private lateinit var envDetails: MutableSet<String>
    private lateinit var naturePreferredInformation: String

    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnvironmentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        envDescriptionReceived = intent.getStringExtra("envDescription").toString()
        envDetails = sharedPreferences.getStringSet("natureResponses", mutableSetOf())!!
        naturePreferredInformation =
            sharedPreferences.getString("naturePreferredInformation", "").toString()

        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)

        binding.titleText.text = "Content Type: $naturePreferredInformation"

        val updatedDescription = envDescriptionReceived.replace("*", "").replace("#", "").trim()
        binding.envDetails.text = updatedDescription

        binding.backArrow.setOnClickListener {
            onBackPressed()
        }

        binding.copyIconEnv.setOnClickListener {
            copyToClipboard(updatedDescription)
        }

        binding.shareIconEnv.setOnClickListener {
            shareText(updatedDescription)
        }

        binding.playIconEnv.setOnClickListener {
            binding.playIconEnv.visibility = View.GONE
            binding.pauseIconEnv.visibility = View.VISIBLE
            ttsInitialized = true
            speakOut(updatedDescription)
        }

        binding.pauseIconEnv.setOnClickListener {
            stopSpeaking()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("nature", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language not supported!")
            } else {
                ttsInitialized = true
                setupUtteranceProgressListener()
                Log.i("TTS", "Text-to-Speech initialized successfully")
            }
        } else {
            Log.e("TTS", "Initialization failed with status: $status")
            Toast.makeText(this@EnvironmentDetailsActivity, "inside else's else", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUtteranceProgressListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                runOnUiThread {
                    binding.playIconEnv.visibility = View.GONE
                    binding.pauseIconEnv.visibility = View.VISIBLE
                    Log.i("TTS", "TTS started: $utteranceId")
                }
            }

            override fun onDone(utteranceId: String) {
                runOnUiThread {
                    binding.playIconEnv.visibility = View.VISIBLE
                    binding.pauseIconEnv.visibility = View.GONE
                    Log.i("TTS", "TTS finished: $utteranceId")
                }
            }

            override fun onError(utteranceId: String) {
                runOnUiThread {
                    Toast.makeText(
                        this@EnvironmentDetailsActivity,
                        "Some error occurred",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.playIconEnv.visibility = View.VISIBLE
                    binding.pauseIconEnv.visibility = View.GONE
                    Log.e("TTS", "TTS error occurred: $utteranceId")
                }
            }
        })
    }

    private fun speakOut(text: String) {
        val params = Bundle()
        val utteranceId = "shan123"
        if (ttsInitialized) {
            Log.i("TTS", "Speaking text: $text")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            Log.e("TTS", "TTS is not initialized")
            binding.playIconEnv.visibility = View.VISIBLE
            binding.pauseIconEnv.visibility = View.INVISIBLE
        }
    }

    private fun stopSpeaking() {
        if (tts.isSpeaking) {
            tts.stop()
            binding.playIconEnv.visibility = View.VISIBLE
            binding.pauseIconEnv.visibility = View.GONE
            Log.i("TTS", "TTS stopped speaking")
        }
    }

    override fun onPause() {
        super.onPause()
        if (tts.isSpeaking) {
            tts.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        tts = TextToSpeech(this@EnvironmentDetailsActivity, this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

}
