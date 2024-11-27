package com.shanalimughal.mentalhealthai.Fragments

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Models.TherapySummaryModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentTherapyQuestionsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.shanalimughal.mentalhealthai.BuildConfig

class TherapyQuestionsFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var binding: FragmentTherapyQuestionsBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var userName: String
    private lateinit var userProfession: String
    private var sleepHours: Int = 0
    private lateinit var mood: String
    private lateinit var personalGoals: MutableSet<String>

    private val REQUEST_CODE_SPEECH_INPUT = 102


//    private var totalQuestions: Int = 10

    private lateinit var tts: TextToSpeech

    private lateinit var progressDialog: ProgressDialog

    private lateinit var aiQuestions: MutableSet<String>
    private lateinit var sessionHistory: MutableSet<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTherapyQuestionsBinding.inflate(inflater, container, false)
        sharedPreferences =
            requireActivity().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        userName = sharedPreferences.getString("userName", "").toString()
        userProfession = sharedPreferences.getString("profession", "").toString()
        mood = sharedPreferences.getString("mood", "").toString()
        personalGoals = sharedPreferences.getStringSet("goals", mutableSetOf())!!
        sleepHours = sharedPreferences.getInt("sleepHours", 0)

        aiQuestions =
            sharedPreferences.getStringSet("aiQuestions", mutableSetOf()) ?: mutableSetOf()
        sessionHistory =
            sharedPreferences.getStringSet("sessionHistory", mutableSetOf()) ?: mutableSetOf()

        tts = TextToSpeech(requireContext(), this)

        val firstName = userName.split(" ")[0]
        binding.mainTitle.text = "Hi $firstName, I am your Mental Health Therapist AI"

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("AI is thinking...")
        progressDialog.setCancelable(false)

        val progressBar = requireActivity().findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = 50

        binding.startIcon.setOnClickListener {
            getSessionsHistory()
        }

        binding.submitBtn.setOnClickListener {
            val answer = binding.answerEditText.text.toString()
            if (answer.isEmpty()) {
                binding.answerEditText.error = "Please enter your answer"
            } else {
                submitResponse(answer)
            }
        }

        binding.endSession.setOnClickListener {
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.linearLayoutTherapy, TherapyFeedbackFragment())
            fragmentTransaction.commit()

            tts.shutdown()
        }

        binding.micIcon.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...")
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        }

        return binding.root
    }

    private fun getAiQuestion() {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 1000
                responseMimeType = "text/plain"
            })

        val userDetails = """
        You are my mental health Therapist and we are having a Therapy session right now.
         Always question me as my mental health therapist. 
         also don't me ask too many questions and also recommend some tips or anything useful for me according to the conversation. 
        Make sure to be a friendly and helpful therapist behaviour. also use my previous sessions history to tailor the questions. 
        Here are some details about myself:
        - Name: $userName
        - mood: $mood
        - Sleep Hours: $sleepHours
        - Profession: $userProfession
        - Personal Goal: $personalGoals
    """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = model.startChat().sendMessage(
                    ("\n$userDetails\nPrevious Sessions History:\n$sessionHistory\nNow ask any question to me just one question also don't repeat any of these questions of this session: $aiQuestions")
                )
                val updatedResponse =
                    response.text.toString().replace("*", "").replace("**", "").replace("\n", " ")

                aiQuestions.add("AI question: $updatedResponse")
                editor.putStringSet("aiQuestions", aiQuestions)
                editor.apply()

                withContext(Dispatchers.Main) {
                    try {
                        progressDialog.dismiss()
//                        binding.questionsRemainingText.text = "Questions Remaining: $totalQuestions"
                        binding.questionText.text = "Q. $updatedResponse"

                        binding.startIcon.visibility = View.GONE
                        binding.aiDescription.visibility = View.GONE

                        binding.questionText.visibility = View.VISIBLE
                        binding.inputLayout.visibility = View.VISIBLE
                        binding.submitLayout.visibility = View.VISIBLE

                        speakOut(updatedResponse)
                    } catch (e: Exception) {
                        e.message
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), "Some error occurred, try again", Toast.LENGTH_SHORT
                    )
                    binding.questionText.text = "Some error occurred, try again"
                    binding.submitBtn.setText("Retry")
                    progressDialog.dismiss()
                }
                e.printStackTrace()
                Log.d("apiError", e.message ?: "Error occurred")
            }
        }
    }

    private fun submitResponse(answer: String) {
        progressDialog.show()
        val model = GenerativeModel(
            "gemini-1.5-flash-latest",
            "AIzaSyALq4iq4hdfcgOfllsA3GTltp522avhSWA",
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 1000
                responseMimeType = "text/plain"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
            ),
        )

        val userDetails = """
        You are my mental health Therapist and we are having a Therapy session right now. Always question me as my mental health therapist.
        Here are some details about myself:
        - Name: $userName
        - mood: $mood
        - Sleep Hours: $sleepHours
        - Profession: $userProfession
        - Personal Goal: $personalGoals
    """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = model.startChat().sendMessage(
                    ("$userDetails\nPrevious Sessions History:\n" + "$sessionHistory\nprevious discussion in this session: ${aiQuestions}\nUser answer: $answer\nNote: Just answer the last user response not all the responses!!! also don't say $userName you mentioned this or that. don't do this. just answer the last response of the user. That's it!!")
                )

                val updatedResponse =
                    response.text.toString().replace("*", "").replace("**", "").replace("\n", " ")

                aiQuestions.add("User answer: $answer")
                aiQuestions.add("AI question: $updatedResponse")
                editor.putStringSet("aiQuestions", aiQuestions)
                editor.apply()

                withContext(Dispatchers.Main) {
                    try {
                        progressDialog.dismiss()
                        binding.answerEditText.text.clear()
                        binding.questionText.text = "Q. $updatedResponse"

//                        totalQuestions -= 1
//                        binding.questionsRemainingText.text = "Questions Remaining: $totalQuestions"

                        speakOut(updatedResponse)
                    } catch (e: Exception) {
                        e.message
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), "Some error occurred, try again", Toast.LENGTH_SHORT
                    )
                    progressDialog.dismiss()
                }
                e.printStackTrace()
                Log.d("apiError", e.message ?: "Error occurred")
            }
        }
    }

    private fun getSessionsHistory() {
        progressDialog.show()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child("therapySessions")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (snaps in snapshot.children) {
                        val model = snaps.getValue(TherapySummaryModel::class.java)
                        if (model != null) {
                            sessionHistory.add(model.summary)
                        }
                    }
                    getAiQuestion()
                } else {
                    getAiQuestion()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Some error occurred", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }

        })

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language not supported!")
            } else {
                Log.e("TTS", "Some other error occurred")
            }
        }
    }

    private fun speakOut(message: String) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            binding.answerEditText.setText(result?.get(0).toString())
        }
    }

    override fun onPause() {
        super.onPause()
        tts.stop()
    }

    override fun onResume() {
        super.onResume()
        tts = TextToSpeech(requireContext(), this)
    }
}