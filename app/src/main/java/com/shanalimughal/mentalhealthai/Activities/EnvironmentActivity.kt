package com.shanalimughal.mentalhealthai.Activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.shanalimughal.mentalhealthai.Adapters.EnvironmentAdapter
import com.shanalimughal.mentalhealthai.Models.EnvModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityEnvironmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shanalimughal.mentalhealthai.BuildConfig

class EnvironmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnvironmentBinding

    private lateinit var arrayList: ArrayList<EnvModel>
    private lateinit var envAdapter: EnvironmentAdapter

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var interestedAreas: MutableSet<String>
    private lateinit var importance: String
    private lateinit var preferredInformation: String

    private lateinit var profession: String
    private lateinit var mood: String
    private lateinit var personalGoals: MutableSet<String>

    private lateinit var natureResponses: MutableSet<String>
    private var isFirstTimeNatureAndEnvironmentGenerated: Boolean = false

    private lateinit var progressDialog: ProgressDialog

    private lateinit var prompt: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnvironmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        interestedAreas =
            sharedPreferences.getStringSet("natureInterestedAreas", mutableSetOf())
                ?: mutableSetOf()
        importance =
            sharedPreferences.getString("natureImportance", "").toString()
        preferredInformation =
            sharedPreferences.getString("naturePreferredInformation", "").toString()

        profession = sharedPreferences.getString("profession", "").toString()
        mood = sharedPreferences.getString("mood", "").toString()
        personalGoals = sharedPreferences.getStringSet("goals", mutableSetOf())!!

        natureResponses =
            sharedPreferences.getStringSet(
                "natureResponses",
                emptySet<String?>().toMutableSet()
            ) ?: mutableSetOf()

        isFirstTimeNatureAndEnvironmentGenerated =
            sharedPreferences.getBoolean("isFirstTimeNatureAndEnvironmentGenerated", false)

        arrayList = ArrayList()

        envAdapter = EnvironmentAdapter(arrayList, this)
        binding.natureRecycler.layoutManager = LinearLayoutManager(this)
        binding.natureRecycler.adapter = envAdapter
        binding.natureRecycler.visibility = View.VISIBLE

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        if (isFirstTimeNatureAndEnvironmentGenerated) {
            fetchAllEnvContentFromFirebase()
        } else {
            settingArrayListForTheFirstTime()
        }

        binding.generateMoreContent.visibility = View.INVISIBLE
        binding.envIcon.visibility = View.INVISIBLE

        binding.generateMoreContent.setOnClickListener {
            settingArrayListForTheFirstTime()
        }

        binding.settingIcon.setOnClickListener {
            startActivity(Intent(this, EnvironmentUserPreferencesActivity::class.java))
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun settingArrayListForTheFirstTime() {
        if (arrayList.size < 1) {
            binding.natureErrorText.visibility = View.VISIBLE
            binding.natureErrorText.text = "Loading..."
        } else {
            progressDialog.setMessage("Generating Content")
            progressDialog.show()
        }
        val previousMeditations = natureResponses
        prompt = if (previousMeditations.size > 1) {
            "Write a single $preferredInformation for the following preferences as a mental health companion & Nature and Environment expert. Tell user in the response that why Nature and Environment plays a huge role in Mental health and what they can do to maintain their Mental health and Nature and Environment better:\n\n" +
                    "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                    "Mood: ${mood}\nEnvironment Interested Areas: $interestedAreas\n" +
                    "Provide a detailed $preferredInformation including its benefits and steps to follow.\n\n" +
                    "Important Note: Please don't include any of these previous generated content again.\n" +
                    "Suggest something different than these contents. These contents are given below:\n${natureResponses} "
        } else {
            "Write a single $preferredInformation for the following preferences as a mental health companion & Nature and Environment. Tell user in the response that why Nature and Environment plays a huge role in Mental health and what they can do to main Mental health good and Nature and Environment better:\n\n" +
                    "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                    "Mood: ${mood}\nEnvironment Interested Areas: $interestedAreas\n" +
                    "Provide a detailed $preferredInformation including its benefits and steps to follow."

        }
        apiRequest(prompt) { responseText ->
            handleApiResponse(responseText)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleApiResponse(responseText: String) {
        if (responseText.isNotEmpty()) {
            arrayList.add(
                0,
                EnvModel(
                    R.drawable.nature_design,
                    responseText,
                    listOf("Interested Areas: $interestedAreas"),
                    "Importance: $importance",
                    "Preferred Information: $preferredInformation",
                )
            )

            envAdapter.notifyDataSetChanged()
            binding.natureErrorText.visibility = View.INVISIBLE

            // Upload to Firebase
            uploadToFirebase(responseText)

        } else {
            binding.natureErrorText.visibility = View.VISIBLE
            binding.natureErrorText.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun apiRequest(prompt: String, callback: (String) -> Unit) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 5000
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
                val response = model.startChat(listOf()).sendMessage(prompt)
                withContext(Dispatchers.Main) {
                    response.text?.let { callback(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback("")
                }
            }
        }
    }

    private fun uploadToFirebase(response: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("natureResponses")

        val responseKey = myRef.push().key
        if (responseKey != null) {
            myRef.child(responseKey).setValue(response)
                .addOnSuccessListener {
                    database.getReference("Users").child(userId)
                        .child("isFirstTimeNatureAndEnvironmentGenerated")
                        .setValue(true)

                    fetchEnvContentFromFirebase()
                }
                .addOnFailureListener {
                    Log.d("MeditationFragmentExceptions", it.message.toString())
                }
        }
    }

    private fun fetchEnvContentFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("natureResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fetchedMeditationResponses = mutableSetOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { response ->
                            fetchedMeditationResponses.add(response)
                        }
                    }

                    arrayList.clear()
                    for (snaps in snapshot.children) {
                        val responseValue = snaps.getValue(String::class.java)
                        if (responseValue != null) {
                            arrayList.add(
                                0,
                                EnvModel(
                                    R.drawable.nature_design,
                                    responseValue,
                                    listOf("Interested Areas: $interestedAreas"),
                                    "Importance: $importance",
                                    "Preferred Information: $preferredInformation",
                                )
                            )
                        }

                        // Update SharedPreferences
                        natureResponses.clear()
                        natureResponses.addAll(fetchedMeditationResponses)
                        editor.putStringSet("natureResponses", natureResponses)
                        editor.apply()

                        database.getReference("Users")
                            .child(userId)
                            .child("isFirstTimeNatureAndEnvironmentGenerated")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val valueFb = snapshot.value as Boolean
                                    if (valueFb) {
                                        editor.putBoolean(
                                            "isFirstTimeNatureAndEnvironmentGenerated",
                                            true
                                        )
                                        editor.apply()
                                    } else {
                                        editor.putBoolean(
                                            "isFirstTimeNatureAndEnvironmentGenerated",
                                            false
                                        )
                                        editor.apply()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.d("MeditationFragmentExceptions", error.message)
                                }
                            })

                        binding.generateMoreContent.visibility = View.VISIBLE
                        binding.envIcon.visibility = View.VISIBLE
                        binding.natureErrorText.visibility = View.INVISIBLE
                        envAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.natureErrorText.visibility = View.VISIBLE
                binding.natureRecycler.visibility = View.INVISIBLE
            }
        })
    }

    private fun fetchAllEnvContentFromFirebase() {
        progressDialog.setMessage("Fetching Content")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("natureResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fetchedMeditationResponses = mutableSetOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { response ->
                            fetchedMeditationResponses.add(response)
                        }
                    }

                    arrayList.clear()
                    for (snaps in snapshot.children) {
                        val responseValue = snaps.getValue(String::class.java)
                        if (responseValue != null) {
                            arrayList.add(
                                0,
                                EnvModel(
                                    R.drawable.nature_design,
                                    responseValue,
                                    listOf("Interested Areas: $interestedAreas"),
                                    "Importance: $importance",
                                    "Preferred Information: $preferredInformation",
                                )
                            )
                        }


                        // Update SharedPreferences
                        natureResponses.clear()
                        natureResponses.addAll(fetchedMeditationResponses)
                        editor.putStringSet("natureResponses", natureResponses)
                        editor.apply()

                        database.getReference("Users")
                            .child(userId)
                            .child("isFirstTimeNatureAndEnvironmentGenerated")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val valueFb = snapshot.value as Boolean
                                    if (valueFb) {
                                        editor.putBoolean(
                                            "isFirstTimeNatureAndEnvironmentGenerated",
                                            true
                                        )
                                        editor.apply()
                                    } else {
                                        editor.putBoolean(
                                            "isFirstTimeNatureAndEnvironmentGenerated",
                                            false
                                        )
                                        editor.apply()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.d("MeditationFragmentExceptions", error.message)
                                }
                            })
                        binding.generateMoreContent.visibility = View.VISIBLE
                        binding.envIcon.visibility = View.VISIBLE

                        progressDialog.dismiss()
                        envAdapter.notifyDataSetChanged()
                    }
                } else {
                    progressDialog.dismiss()
                    binding.natureErrorText.visibility = View.VISIBLE
                    binding.natureRecycler.visibility = View.INVISIBLE

                    binding.natureErrorText.text = "No Content Found"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.natureErrorText.visibility = View.VISIBLE
                binding.natureRecycler.visibility = View.INVISIBLE
            }
        })
    }

    override fun onPause() {
        progressDialog.dismiss()
        super.onPause()
    }
}