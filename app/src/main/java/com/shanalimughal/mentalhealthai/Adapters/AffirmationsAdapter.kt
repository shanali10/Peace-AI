package com.shanalimughal.mentalhealthai.Adapters

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Activities.FavoriteAffirmationsActivity
import com.shanalimughal.mentalhealthai.Activities.MainActivity
import com.shanalimughal.mentalhealthai.Models.AffirmationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.AffirmationsSampleLayoutBinding
import java.util.Locale

class AffirmationsAdapter(
    private val affirmationList: ArrayList<AffirmationModel>,
    private val context: Context
) : RecyclerView.Adapter<AffirmationsAdapter.AffirmationViewHolder>(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech

    private lateinit var progressDialog: ProgressDialog

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var favoriteAffirmations: MutableSet<String>

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AffirmationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.affirmations_sample_layout, parent, false)
        return AffirmationViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AffirmationViewHolder, position: Int) {
        val affirmation = affirmationList[position]
        holder.binding.affirmationImage.setImageResource(affirmation.affirmationPlaceholder)
        holder.binding.affirmationTitle.text = affirmation.affirmationTitle
        holder.binding.affirmationTheme.text = affirmation.affirmationTheme.joinToString(", ")
        holder.binding.preferredAffirmation.text = affirmation.preferredAffirmation

        sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        favoriteAffirmations =
            sharedPreferences.getStringSet(
                "favoriteAffirmations",
                emptySet<String?>().toMutableSet()
            ) ?: mutableSetOf()


        // Progress dialog
        (context as? MainActivity)?.runOnUiThread {
            progressDialog = ProgressDialog(context)
            progressDialog.setCancelable(false)
        }

        holder.binding.copyIcon.setOnClickListener {
            copyToClipboard(holder.binding.affirmationTitle.text.toString())
        }

        holder.binding.shareIcon.setOnClickListener {
            shareText(holder.binding.affirmationTitle.text.toString())
        }

        holder.binding.playIcon.setOnClickListener {
            speakOut(affirmation.affirmationTitle, holder)
        }

        holder.binding.pauseIcon.setOnClickListener {
            stopSpeaking(holder)
        }

        holder.binding.favoriteIcon.setOnClickListener {
            uploadToFirebase(
                affirmation.affirmationTitle,
                holder.binding.favoriteIcon,
                holder.binding.favoriteIconFilled
            )
        }

        holder.binding.favoriteIconFilled.setOnClickListener {
            removeFromFavorites(
                affirmation.affirmationTitle,
                holder.binding.favoriteIcon,
                holder.binding.favoriteIconFilled
            )
        }

        if (favoriteAffirmations.contains(affirmation.affirmationTitle)) {
            holder.binding.favoriteIcon.visibility = View.GONE
            holder.binding.favoriteIconFilled.visibility = View.VISIBLE
        } else {
            holder.binding.favoriteIcon.visibility = View.VISIBLE
            holder.binding.favoriteIconFilled.visibility = View.GONE
        }
        Log.d("Favorite Affirmations", favoriteAffirmations.toString())
        Log.d("Favorite Affirmations", favoriteAffirmations.size.toString())
    }


    override fun getItemCount() = affirmationList.size

    class AffirmationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = AffirmationsSampleLayoutBinding.bind(itemView)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Affirmation", text)
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
            }
        } else {
            Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut(affirmationTitle: String, holder: AffirmationViewHolder) {
        val params = Bundle()
        val utteranceId = "shan123"

        holder.binding.playIcon.visibility = View.GONE
        holder.binding.pauseIcon.visibility = View.VISIBLE
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {

                (context as? MainActivity)?.runOnUiThread {
                    holder.binding.playIcon.visibility = View.GONE
                    holder.binding.pauseIcon.visibility = View.VISIBLE
                }
                (context as? FavoriteAffirmationsActivity)?.runOnUiThread {
                    holder.binding.playIcon.visibility = View.GONE
                    holder.binding.pauseIcon.visibility = View.VISIBLE
                }
            }

            override fun onDone(utteranceId: String) {
                (context as? MainActivity)?.runOnUiThread {
                    holder.binding.playIcon.visibility = View.VISIBLE
                    holder.binding.pauseIcon.visibility = View.GONE
                }
                (context as? FavoriteAffirmationsActivity)?.runOnUiThread {
                    holder.binding.playIcon.visibility = View.VISIBLE
                    holder.binding.pauseIcon.visibility = View.GONE
                }
            }

            override fun onError(utteranceId: String) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
                    holder.binding.playIcon.visibility = View.VISIBLE
                    holder.binding.pauseIcon.visibility = View.GONE
                }
            }
        })

        tts.speak(affirmationTitle, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun stopSpeaking(holder: AffirmationViewHolder) {
        if (tts.isSpeaking) {
            tts.stop()

            holder.binding.playIcon.visibility = View.VISIBLE
            holder.binding.pauseIcon.visibility = View.GONE
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        tts.stop()
        tts.shutdown()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private fun uploadToFirebase(
        affirmation: String,
        favoriteIcon: ImageView,
        favoriteIconFilled: ImageView
    ) {
        progressDialog.setMessage("Saving...")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("favoriteAffirmations")

        val responseKey = myRef.push().key
        if (responseKey != null) {
            myRef.child(responseKey).setValue(affirmation)
                .addOnSuccessListener {
                    Toast.makeText(context, "Affirmation added to favorites", Toast.LENGTH_SHORT)
                        .show()
                    progressDialog.dismiss()

                    favoriteIcon.visibility = View.GONE
                    favoriteIconFilled.visibility = View.VISIBLE
                }
                .addOnFailureListener {
                    Log.d("MeditationFragmentExceptions", it.message.toString())
                    Toast.makeText(context, "Failed to save affirmation", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
        }
    }


    private fun removeFromFavorites(
        affirmationTitle: String,
        favoriteIcon: ImageView,
        favoriteIconFilled: ImageView
    ) {
        (context as? MainActivity)?.runOnUiThread {
            progressDialog.setMessage("Removing...")
            progressDialog.show()
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("favoriteAffirmations")

        // Query the database to find the key of the affirmation to remove
        myRef.orderByValue().equalTo(affirmationTitle)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (childSnapshot in snapshot.children) {
                            // Remove the affirmation using the key
                            childSnapshot.ref.removeValue().addOnSuccessListener {

                                Toast.makeText(
                                    context,
                                    "Affirmation removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()

                                (context as? MainActivity)?.runOnUiThread {
                                    favoriteIcon.visibility = View.VISIBLE
                                    favoriteIconFilled.visibility = View.GONE
                                    progressDialog.dismiss()
                                }

                            }.addOnFailureListener { e ->
                                Log.e("RemoveFromFavorites", "Failed to remove affirmation", e)
                                Toast.makeText(
                                    context,
                                    "Failed to remove affirmation",
                                    Toast.LENGTH_SHORT
                                ).show()
                                progressDialog.dismiss()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Affirmation not found", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RemoveFromFavorites", "Database error: ${error.message}")
                    Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                    progressDialog.dismiss()
                }
            })
    }
}
