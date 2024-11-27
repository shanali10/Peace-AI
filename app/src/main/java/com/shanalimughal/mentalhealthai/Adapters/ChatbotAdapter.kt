package com.shanalimughal.mentalhealthai.Adapters

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.shanalimughal.mentalhealthai.Activities.MainActivity
import com.shanalimughal.mentalhealthai.Models.ChatbotModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.RoomDB.ChatMessageDao
import com.shanalimughal.mentalhealthai.databinding.ChatbotSampleLayoutBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatbotAdapter(
    private val chatbotList: ArrayList<ChatbotModel>,
    private val context: Context,
    private val chatMessageDao: ChatMessageDao
) : RecyclerView.Adapter<ChatbotAdapter.ViewHolder>(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var profileImageUrl: String
    private val sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.chatbot_sample_layout, parent, false)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = chatbotList[position]
        holder.binding.userPromptText.text = data.userPrompt
        holder.binding.responseText.text = data.response

        // user prompt
        holder.binding.copyUserPrompt.setOnClickListener {
            copyToClipboard(data.userPrompt)
        }
        holder.binding.shareUserPrompt.setOnClickListener {
            shareText(data.userPrompt)
        }

        // ai response
        holder.binding.copyResponseText.setOnClickListener {
            copyToClipboard(data.response)
        }
        holder.binding.shareResponseText.setOnClickListener {
            shareText(data.response)
        }

        // initializing tts
        tts = TextToSpeech(context, this)

        holder.binding.playUserPrompt.setOnClickListener {
            speakOut(data.userPrompt, holder.binding.playUserPrompt, holder.binding.pauseUserPrompt)
        }

        holder.binding.pauseUserPrompt.setOnClickListener {
            stopSpeaking(holder.binding.playUserPrompt, holder.binding.pauseUserPrompt)
        }

        holder.binding.playResponseText.setOnClickListener {
            speakOut(
                data.response,
                holder.binding.playResponseText,
                holder.binding.pauseResponseText
            )
        }

        holder.binding.pauseResponseText.setOnClickListener {
            stopSpeaking(holder.binding.playResponseText, holder.binding.pauseResponseText)
        }

        // deleting the message
        holder.binding.deleteResponseText.setOnClickListener {
            showDeleteConfirmationDialog(position)
        }

        profileImageUrl = sharedPreferences.getString("profileImageUrl", "").toString()
        if (profileImageUrl.isNotEmpty()) {
            Picasso.get().load(profileImageUrl).placeholder(R.drawable.profile_placeholder)
                .into(holder.binding.userProfileIcon)
        }
    }

    override fun getItemCount() = chatbotList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ChatbotSampleLayoutBinding.bind(itemView)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
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

    private fun speakOut(message: String, playIcon: ImageView, pauseIcon: ImageView) {
        val params = Bundle()
        val utteranceId = "shan123"

        playIcon.visibility = View.INVISIBLE
        pauseIcon.visibility = View.VISIBLE
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                (context as? MainActivity)?.runOnUiThread {
                    playIcon.visibility = View.INVISIBLE
                    pauseIcon.visibility = View.VISIBLE
                }
            }

            override fun onDone(utteranceId: String) {
                (context as? MainActivity)?.runOnUiThread {
                    playIcon.visibility = View.VISIBLE
                    pauseIcon.visibility = View.INVISIBLE
                }
            }

            override fun onError(utteranceId: String) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
                    playIcon.visibility = View.VISIBLE
                    pauseIcon.visibility = View.INVISIBLE
                }
            }
        })

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun stopSpeaking(playIcon: ImageView, pauseIcon: ImageView) {
        if (tts.isSpeaking) {
            tts.stop()
            playIcon.visibility = View.VISIBLE
            pauseIcon.visibility = View.INVISIBLE
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Yes") { dialog, id ->
                deleteChatMessage(position)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun deleteChatMessage(position: Int) {
        val chatMessage = chatbotList[position]

        // Delete from Room database
        CoroutineScope(Dispatchers.IO).launch {

            chatMessageDao.deleteMessageById(chatMessage.id)
            // Update RecyclerView on the main thread
            withContext(Dispatchers.Main) {
                chatbotList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        tts.stop()
        tts.shutdown()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}