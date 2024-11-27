package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.PostModel
import com.shanalimughal.mentalhealthai.databinding.FragmentCreatePostBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.shanalimughal.mentalhealthai.BuildConfig

class CreatePostFragment : Fragment() {
    private lateinit var binding: FragmentCreatePostBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var userid: String

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var userName: String
    private lateinit var userProfileUrl: String
    private lateinit var postDescription: String
    private lateinit var postTime: String

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        firebaseDatabase = FirebaseDatabase.getInstance()

        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userName = sharedPreferences.getString("userName", "").toString()
        userProfileUrl = sharedPreferences.getString("profileImageUrl", "").toString()

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("AI is authenticating...")
        progressDialog.setCancelable(false)

        userid = (FirebaseAuth.getInstance().currentUser?.uid ?: "")

        binding.postBtn.setOnClickListener {
            uploadPost()
        }

        return binding.root
    }

    private fun uploadPost() {
        postDescription = binding.createPostEditText.text.toString()

        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)
        postTime = formattedTime

        if (postDescription.isEmpty()) {
            binding.createPostEditText.error = "Please write something"
        } else {
            progressDialog.show()
            aiAuthenticityRequest(postDescription)
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun aiAuthenticityRequest(prompt: String) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 500
                responseMimeType = "text/plain"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
            ),
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chat = model.startChat()
                val response = chat.sendMessage(
                    "A user has posted this content in the Community Forum of my application." +
                            " Please check and verify whether this content is offensive, sexual, or contains any inappropriate material." +
                            " If the content contains harmful text, respond with 'harmful'; otherwise, respond with 'safe'.\n" +
                            "\n" +
                            "Content: $prompt"
                )

                withContext(Dispatchers.Main) {
                    if (response.text!!.contains("safe", true)) {
                        progressDialog.setMessage("Uploading...")
                        uploadToFirebase(
                            PostModel(
                                userid,
                                userName,
                                userProfileUrl,
                                postDescription,
                                postTime,
                                0,
                                0
                            )
                        )
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "Content is inappropriate and cannot be posted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("apiError", e.message ?: "Error occurred")
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Content is inappropriate and cannot be posted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun uploadToFirebase(model: PostModel) {
        firebaseDatabase.getReference().child("posts").push().setValue(model)
            .addOnSuccessListener {
                progressDialog.dismiss()
                binding.createPostEditText.text.clear()
                Toast.makeText(requireContext(), "Post uploaded successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to upload post.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
