package com.shanalimughal.mentalhealthai.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shanalimughal.mentalhealthai.databinding.ActivityTherapySummaryDetailBinding
import java.util.Locale

class TherapySummaryDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityTherapySummaryDetailBinding
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTherapySummaryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        val summary = intent.getStringExtra("summary")
        binding.summaryText.text = summary

        binding.backArrow.setOnClickListener {
            onBackPressed()
        }

        binding.copyIcon.setOnClickListener {
            val text = binding.summaryText.text.toString()
            copyToClipboard(text)
        }

        binding.shareIcon.setOnClickListener {
            val text = binding.summaryText.text.toString()
            shareText(text)
        }

        binding.playIcon.setOnClickListener {
            val text = binding.summaryText.text.toString()
            speakOut(text)
        }

        binding.pauseIcon.setOnClickListener {
            stopSpeaking()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("summary", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            this@TherapySummaryDetailActivity, "Text copied to clipboard", Toast.LENGTH_SHORT
        ).show()
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
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speakOut(meditationDetails: String) {
        val params = Bundle()
        val utteranceId = "shan123"

        binding.playIcon.visibility = View.GONE
        binding.pauseIcon.visibility = View.VISIBLE

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                runOnUiThread {
                    binding.playIcon.visibility = View.GONE
                    binding.pauseIcon.visibility = View.VISIBLE
                }
            }

            override fun onDone(utteranceId: String) {
                runOnUiThread {
                    binding.playIcon.visibility = View.VISIBLE
                    binding.pauseIcon.visibility = View.GONE
                }
            }

            override fun onError(utteranceId: String) {
                runOnUiThread {
                    Toast.makeText(
                        this@TherapySummaryDetailActivity, "Some error occurred", Toast.LENGTH_SHORT
                    ).show()
                    binding.playIcon.visibility = View.VISIBLE
                    binding.pauseIcon.visibility = View.GONE
                }
            }
        })

        tts.speak(meditationDetails, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun stopSpeaking() {
        if (tts.isSpeaking) {
            tts.stop()
            binding.playIcon.visibility = View.VISIBLE
            binding.pauseIcon.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        if (tts.isSpeaking) {
            tts.stop()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}