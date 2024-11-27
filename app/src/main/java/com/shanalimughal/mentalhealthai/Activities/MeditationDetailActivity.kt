package com.shanalimughal.mentalhealthai.Activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityMeditationDetailBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MeditationDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMeditationDetailBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var meditationGoals: MutableSet<String>
    private lateinit var meditationDuration: String

    private lateinit var meditationDescriptionReceived: String
    private lateinit var currentClass: String
    private lateinit var meditationDetails: MutableSet<String>

    private lateinit var mediaPlayer: MediaPlayer

    private var countDownTimer: CountDownTimer? = null
    private var meditationTimer: CountDownTimer? = null

    private lateinit var tts: TextToSpeech

    private lateinit var progressDialog: ProgressDialog

    private lateinit var favoriteMeditations: MutableSet<String>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeditationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        meditationGoals = sharedPreferences.getStringSet("meditationGoal", mutableSetOf())!!
        meditationDuration =
            sharedPreferences.getString("preferredMeditationSessionLength", "") ?: ""
        meditationDetails = sharedPreferences.getStringSet("meditationResponses", mutableSetOf())!!
        favoriteMeditations =
            sharedPreferences.getStringSet("favoriteMeditations", mutableSetOf()) ?: mutableSetOf()

        meditationDescriptionReceived = intent.getStringExtra("meditationDescription").toString()
        currentClass = intent.getStringExtra("currentClass").toString()

        // audio manager
        mediaPlayer = MediaPlayer.create(this, R.raw.meditation_timer_started)

        // text to speech
        tts = TextToSpeech(this@MeditationDetailActivity, this)

        // progress dialog init
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Saving...")
        progressDialog.setCancelable(false)

        var meditationTitle =
            meditationDescriptionReceived.replace("*", "").replace("#", "").replace("\n", " ")
                .take(60) + "..."

        if (meditationTitle[0].toString() == " ") {
            meditationTitle = meditationDescriptionReceived.replace("*", "").replaceFirst(" ", "")
                .replace("#", "").replace("\n", " ").take(60) + "..."
        }

        binding.meditationTitle.text = meditationTitle
        binding.meditationGoal.text = "Meditation Goals: $meditationGoals"
        binding.meditationDuration.text = "Meditation Duration: $meditationDuration"

        val updatedDescription =
            meditationDescriptionReceived.replace("*", "").replace("#", "").trim()
        binding.meditationDetails.text = updatedDescription

        // Start button click listener
        binding.startButton.setOnClickListener {
            if (binding.startButton.text == "Stop Countdown") {
                countDownTimer!!.cancel()
                mediaPlayer.stop()
                binding.startButton.text = "Start Meditation Again"
                getMeditationDuration()

            } else if (binding.startButton.text == "Stop Meditation") {
                meditationTimer!!.cancel()
                mediaPlayer.stop()
                binding.startButton.text = "Start Meditation Again"
                getMeditationDuration()
            } else {
                startCountDown()
                mediaPlayer = MediaPlayer.create(this, R.raw.meditation_timer_started)
                mediaPlayer.start()
                binding.startButton.text = "Stop Countdown"
            }
        }


        // checking what is the meditation duration
        getMeditationDuration()

        binding.backArrow.setOnClickListener {
            onBackPressed()
        }

        binding.copyIcon.setOnClickListener {
            copyToClipboard(updatedDescription)
        }

        binding.shareIcon.setOnClickListener {
            shareText(updatedDescription)
        }

        binding.playIcon.setOnClickListener {
            speakOut(updatedDescription)
        }

        binding.pauseIcon.setOnClickListener {
            stopSpeaking()
        }

        binding.favoriteIcon.setOnClickListener {
            uploadToFirebase(
                meditationDescriptionReceived,
                binding.favoriteIcon,
                binding.favoriteIconFilled
            )
        }

        binding.favoriteIconFilled.setOnClickListener {
            removeFromFavorites(
                meditationDescriptionReceived,
                binding.favoriteIcon,
                binding.favoriteIconFilled
            )
        }

        checkFavoriteStatus()
    }

    private fun checkFavoriteStatus() {
        val favoriteMeditations =
            sharedPreferences.getStringSet("favoriteMeditations", mutableSetOf()) ?: mutableSetOf()

        if (favoriteMeditations.contains(meditationDescriptionReceived)) {
            binding.favoriteIcon.visibility = View.GONE
            binding.favoriteIconFilled.visibility = View.VISIBLE
        } else {
            binding.favoriteIcon.visibility = View.VISIBLE
            binding.favoriteIconFilled.visibility = View.GONE
        }
    }

    private fun startCountDown() {
        countDownTimer = object : CountDownTimer(15 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                binding.timerTextView.text =
                    String.format("%02d:%02d Sec. Countdown Started", minutes, seconds)

                if (seconds.toInt() == 3) {
                    mediaPlayer =
                        MediaPlayer.create(this@MeditationDetailActivity, R.raw.three_two_one)
                    mediaPlayer.start()
                }
            }

            override fun onFinish() {
                when (meditationDuration) {
                    "5 Minutes" -> startMeditationTimer(5 * 60 * 1000)
                    "10 Minutes" -> startMeditationTimer(10 * 60 * 1000)
                    "15 Minutes" -> startMeditationTimer(15 * 60 * 1000)
                    "20 Minutes" -> startMeditationTimer(20 * 60 * 1000)
                }
            }
        }.start()
    }

    private fun startMeditationTimer(durationMillis: Long) {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)

        editor.putFloat(meditationDescriptionReceived, 0f)
        editor.putString("lastMeditationDate", formattedTime)
        editor.putString("lastMeditation", meditationDescriptionReceived)
        editor.apply()
        binding.startButton.text = "Stop Meditation"

        uploadLastMeditationToFirebase(meditationDescriptionReceived, formattedTime)

        meditationTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                binding.timerTextView.text = String.format("%02d:%02d Remaining", minutes, seconds)

                checkHalfTime(durationMillis.toInt(), minutes.toInt(), seconds.toInt())

                val currentProgress = sharedPreferences.getFloat(meditationDescriptionReceived, 0f)
                updateProgress(durationMillis, currentProgress)

                if (minutes == 0L && seconds == 3L) {
                    mediaPlayer =
                        MediaPlayer.create(this@MeditationDetailActivity, R.raw.three_two_one)
                    mediaPlayer.start()
                }
            }

            override fun onFinish() {
                binding.timerTextView.text = "00:00 Done 😍"
                binding.startButton.isEnabled = true
                binding.startButton.text = "Meditation Finished"

                editor.putFloat(meditationDescriptionReceived, 100f)
                editor.apply()
            }
        }.start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("meditation", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            this@MeditationDetailActivity, "Text copied to clipboard", Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }


    private fun uploadToFirebase(
        meditation: String, favoriteIcon: ImageView, favoriteIconFilled: ImageView
    ) {
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("favoriteMeditations")

        val responseKey = myRef.push().key
        if (responseKey != null) {
            myRef.child(responseKey).setValue(meditation).addOnSuccessListener {
                Toast.makeText(
                    this@MeditationDetailActivity,
                    "Meditation added to favorites",
                    Toast.LENGTH_SHORT
                ).show()
                favoriteIcon.visibility = View.GONE
                favoriteIconFilled.visibility = View.VISIBLE
                progressDialog.dismiss()
                favoriteMeditations.add(meditation)
            }.addOnFailureListener {
                Log.d("MeditationFragmentExceptions", it.message.toString())
                Toast.makeText(
                    this@MeditationDetailActivity, "Failed to save meditation", Toast.LENGTH_SHORT
                ).show()
                favoriteIcon.visibility = View.VISIBLE
                favoriteIconFilled.visibility = View.GONE
                progressDialog.dismiss()
            }
        }
    }

    private fun removeFromFavorites(
        meditation: String, favoriteIcon: ImageView, favoriteIconFilled: ImageView
    ) {
        progressDialog.setMessage("Removing...")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("favoriteMeditations")

        // Query the database to find the key of the affirmation to remove
        myRef.orderByValue().equalTo(meditation)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (childSnapshot in snapshot.children) {
                        // Remove the affirmation using the key
                        childSnapshot.ref.removeValue().addOnSuccessListener {
                            Toast.makeText(
                                this@MeditationDetailActivity,
                                "Meditation removed from favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                            favoriteIcon.visibility = View.VISIBLE
                            favoriteIconFilled.visibility = View.GONE
                            progressDialog.dismiss()
                            favoriteMeditations.remove(meditation)
                        }.addOnFailureListener { e ->
                            Log.e("RemoveFromFavorites", "Failed to remove meditation", e)
                            progressDialog.dismiss()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RemoveFromFavorites", "Database error: ${error.message}")
                    progressDialog.dismiss()
                    favoriteIcon.visibility = View.GONE
                    favoriteIconFilled.visibility = View.VISIBLE
                }
            })
    }

    private fun checkHalfTime(millis: Int, minutes: Int, seconds: Int) {
        if (millis == 300000 && minutes == 2 && seconds == 30) {
            mediaPlayer = MediaPlayer.create(
                this@MeditationDetailActivity, R.raw.half_time
            )
            mediaPlayer.start()
        } else if (millis == 600000 && minutes == 5 && seconds == 0) {
            mediaPlayer = MediaPlayer.create(
                this@MeditationDetailActivity, R.raw.half_time
            )
            mediaPlayer.start()
        } else if (millis == 900000 && minutes == 7 && seconds == 30) {
            mediaPlayer = MediaPlayer.create(
                this@MeditationDetailActivity, R.raw.half_time
            )
            mediaPlayer.start()
        } else if (millis == 1200000 && minutes == 10 && seconds == 0) {
            mediaPlayer = MediaPlayer.create(
                this@MeditationDetailActivity, R.raw.half_time
            )
            mediaPlayer.start()
        }
    }

    private fun updateProgress(totalMilliSeconds: Long, currentProgress: Float) {
        if (totalMilliSeconds.toInt() == 300000) {
            editor.putFloat(meditationDescriptionReceived, currentProgress + 0.33f)
            editor.apply()
        } else if (totalMilliSeconds.toInt() == 600000) {
            editor.putFloat(meditationDescriptionReceived, currentProgress + 0.17f)
            editor.apply()
        } else if (totalMilliSeconds.toInt() == 900000) {
            editor.putFloat(meditationDescriptionReceived, currentProgress + 0.11f)
            editor.apply()
        } else if (totalMilliSeconds.toInt() == 1200000) {
            editor.putFloat(meditationDescriptionReceived, currentProgress + 0.08f)
            editor.apply()
        }
    }

    private fun getMeditationDuration() {
        when (meditationDuration) {
            "5 Minutes" -> {
                binding.timerTextView.text = "5:00 Minutes Duration"
            }

            "10 Minutes" -> {
                binding.timerTextView.text = "10:00 Minutes Duration"
            }

            "15 Minutes" -> {
                binding.timerTextView.text = "15:00 Minutes Duration"
            }

            "20 Minutes" -> {
                binding.timerTextView.text = "20:00 Minutes Duration"
            }
        }
    }

    private fun uploadLastMeditationToFirebase(lastMeditation: String, lastMeditationDate: String) {
        FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child("LastMeditationDetails")
            .child("lastMeditation").setValue(lastMeditation)

        FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child("LastMeditationDetails")
            .child("lastMeditationDate").setValue(lastMeditationDate)
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
                        this@MeditationDetailActivity, "Some error occurred", Toast.LENGTH_SHORT
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

    override fun onResume() {
        super.onResume()
        tts = TextToSpeech(this@MeditationDetailActivity, this)
        favoriteMeditations =
            sharedPreferences.getStringSet("favoriteMeditations", mutableSetOf()) ?: mutableSetOf()
        checkFavoriteStatus()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        countDownTimer?.cancel()
        meditationTimer?.cancel()
        mediaPlayer.stop()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}