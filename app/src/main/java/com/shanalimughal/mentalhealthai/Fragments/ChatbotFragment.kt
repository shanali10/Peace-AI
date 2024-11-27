package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.ChatbotAdapter
import com.shanalimughal.mentalhealthai.Models.ChatbotModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.RoomDB.ChatDatabase
import com.shanalimughal.mentalhealthai.RoomDB.ChatMessage
import com.shanalimughal.mentalhealthai.RoomDB.ChatMessageDao
import com.shanalimughal.mentalhealthai.databinding.FragmentChatbotBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.shanalimughal.mentalhealthai.BuildConfig

class ChatbotFragment : Fragment(), TextToSpeech.OnInitListener {
    private lateinit var binding: FragmentChatbotBinding
    private lateinit var arrayList: ArrayList<ChatbotModel>
    private lateinit var adapter: ChatbotAdapter
    private lateinit var db: ChatDatabase
    private lateinit var dao: ChatMessageDao
    private val chatHistory = mutableListOf<ChatMessage>()

    private val REQUEST_CODE_SPEECH_INPUT = 1

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var name: String
    private lateinit var profession: String
    private lateinit var mood: String
    private var sleepHours: Int = 0
    private lateinit var personalGoals: MutableSet<String>

    private lateinit var tts: TextToSpeech

    private lateinit var mediaPlayer: MediaPlayer

    // Firebase database reference
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val chatRef = firebaseDatabase.getReference("Users")

    private var isChatFetchedFromFirebase: Boolean = false

    private lateinit var progressDialog: ProgressDialog

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatbotBinding.inflate(layoutInflater, container, false)

        // tts
        tts = TextToSpeech(requireContext(), this)
        // media player
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.send_sound)

        db = ChatDatabase.getDatabase(requireContext())
        dao = db.chatMessageDao()

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        profession = sharedPreferences.getString("profession", "").toString()
        personalGoals = sharedPreferences.getStringSet("goals", mutableSetOf())!!
        name = sharedPreferences.getString("userName", "").toString()
        mood = sharedPreferences.getString("mood", "").toString()
        sleepHours = sharedPreferences.getInt("sleepHours", 0)

        isChatFetchedFromFirebase = sharedPreferences.getBoolean("isChatFetchedFromFirebase", false)

        val firstName = name.split(" ")[0]
        binding.welcomeText.text =
            "Hello $firstName!\nI am your Mental Health Companion AI Chatbot 😊\n\nAsk Me Anything!"

        arrayList = ArrayList()
        adapter = ChatbotAdapter(arrayList, requireContext(), dao)
        val linearLayout = LinearLayoutManager(requireContext())

        binding.chatbotRecycler.adapter = adapter
        binding.chatbotRecycler.layoutManager = linearLayout

        if (!isChatFetchedFromFirebase) {
            fetchChatsFromFirebase()
        } else {
            loadChatHistory()
        }

        binding.sendIcon.setOnClickListener {
            val prompt = binding.editTextText.text.toString()
            if (prompt.isNotEmpty()) {
                arrayList.add(ChatbotModel(0, "$prompt  ✅", "Typing..."))
                binding.chatbotRecycler.scrollToPosition(arrayList.size - 1)
                adapter.notifyDataSetChanged()

                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.send_sound)
                mediaPlayer.start()

                apiRequest(prompt)

                if (arrayList.isEmpty()) {
                    binding.chatbotPlaceholderIcon.visibility = View.VISIBLE
                    binding.welcomeText.visibility = View.VISIBLE
                } else {
                    binding.chatbotPlaceholderIcon.visibility = View.INVISIBLE
                    binding.welcomeText.visibility = View.INVISIBLE
                }
            }
        }

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.micIcon.visibility = View.INVISIBLE
                binding.sendIcon.visibility = View.VISIBLE
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.micIcon.visibility = View.VISIBLE
                    binding.sendIcon.visibility = View.INVISIBLE
                } else {
                    binding.micIcon.visibility = View.INVISIBLE
                    binding.sendIcon.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        binding.micIcon.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...")
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        }
        return binding.root
    }

    // fetching chats data from the firebase

    private fun fetchChatsFromFirebase() {
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val listener = object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val chatMessages = mutableListOf<ChatMessage>()
                    for (items in snapshot.children) {
                        val chatMessage = items.getValue(ChatMessage::class.java)
                        if (chatMessage != null) {
                            chatMessages.add(chatMessage)
                        }
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        // Save fetched chat messages to the Room database
                        dao.insertAll(chatMessages)
                        withContext(Dispatchers.Main) {
                            chatHistory.clear()
                            chatHistory.addAll(chatMessages)

                            arrayList.clear()
                            arrayList.addAll(chatMessages.map {
                                ChatbotModel(
                                    it.id.toLong(),
                                    it.prompt,
                                    it.response
                                )
                            })

                            adapter.notifyDataSetChanged()
                            binding.chatbotRecycler.scrollToPosition(arrayList.size - 1)
                        }

                        editor.putBoolean("isChatFetchedFromFirebase", true)
                        editor.apply()

                        progressDialog.dismiss()
                    }
                } else {
                    editor.putBoolean("isChatFetchedFromFirebase", true)
                    editor.apply()
                    loadChatHistory()
                    progressDialog.dismiss()
                }
                chatRef.child(userId).child("chats").removeEventListener(this)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "Firebase",
                    "Failed to fetch chats: ${error.message}",
                    error.toException()
                )
                progressDialog.dismiss()
            }
        }

        chatRef.child(userId).child("chats").addValueEventListener(listener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadChatHistory() {
        arrayList.clear()
        val isChatFetchedFromFirebase =
            sharedPreferences.getBoolean("isChatFetchedFromFirebase", false)

        if (isChatFetchedFromFirebase) {
            lifecycleScope.launch(Dispatchers.IO) {
                val chatHistoryFromDb = dao.getAllMessages()
                withContext(Dispatchers.Main) {
                    chatHistory.clear()
                    chatHistory.addAll(chatHistoryFromDb)

                    arrayList.addAll(chatHistoryFromDb.map {
                        ChatbotModel(
                            it.id.toLong(),
                            it.prompt,
                            it.response
                        )
                    })
                    adapter.notifyDataSetChanged()
                    binding.chatbotRecycler.scrollToPosition(arrayList.size - 1)

                    arrayList.reverse()

                    Log.d("chatHistory", chatHistory.reversed().toString())

                    if (arrayList.isEmpty()) {
                        binding.chatbotPlaceholderIcon.visibility = View.VISIBLE
                        binding.welcomeText.visibility = View.VISIBLE
                    } else {
                        binding.chatbotPlaceholderIcon.visibility = View.INVISIBLE
                        binding.welcomeText.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun apiRequest(prompt: String) {
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

        val previousChat = chatHistory.flatMap {
            listOf(
                content(role = "user") { text(it.prompt) },
                content(role = "model") { text(it.response) }
            )
        }

        val userDetails = """
        You are a mental health companion. Always answer me as my mental health doctor/companion. 
        Here are some details about myself:
        - Name: $name
        - mood: $mood
        - Sleep Hours: $sleepHours
        - Profession: $profession
        - Personal Goal: $personalGoals
    """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chat = model.startChat(previousChat)
                val response = chat.sendMessage(
                    content(role = "user") {
                        text(
                            "Previous Chat:\n ${
                                if (chatHistory.size > 25)
                                    chatHistory.take(25).reversed() else chatHistory.reversed()
                            }\nAlways Remember this. Note and don't repeat this again and again to me like hey $name nice to meet you etc." +
                                    " just answer the question according to the prompt given below and don't repeat these name," +
                                    " profession or personal goals in response. okay!:\n$userDetails\nprompt: $prompt"
                        )
                    }
                )

                val updatedResponse = response.text.toString().replace("*", "")
                    .replace("**", "")

                val chatMessage = ChatMessage(prompt = prompt, response = updatedResponse)
                dao.insertMessage(chatMessage)

                // Upload the chat message to Firebase
                uploadChatMessageToFirebase(chatMessage)

                withContext(Dispatchers.Main) {
                    try {
                        binding.editTextText.setText("")
                        chatHistory.add(chatMessage)
                        binding.chatbotRecycler.scrollToPosition(arrayList.size - 1)
                        loadChatHistory()
                        adapter.notifyDataSetChanged()

                        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.receive_sound)
                        mediaPlayer.start()

                        speakOut(updatedResponse)
                    } catch (e: Exception) {
                        e.message
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("apiError", e.message ?: "Error occurred")
            }
        }
    }

    private fun uploadChatMessageToFirebase(chatMessage: ChatMessage) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            chatRef.child(it).child("chats").push().setValue(chatMessage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            binding.editTextText.setText(result?.get(0).toString())
        }
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

    override fun onPause() {
        tts.stop()
        mediaPlayer.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.getBoolean("isChatFetchedFromFirebase", false)
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        mediaPlayer.stop()
        super.onDestroy()
    }
}
