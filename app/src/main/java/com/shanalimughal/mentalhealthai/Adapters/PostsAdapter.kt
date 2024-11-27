package com.shanalimughal.mentalhealthai.Adapters

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Activities.CommentsActivity
import com.shanalimughal.mentalhealthai.Models.NotificationModel
import com.shanalimughal.mentalhealthai.Models.PostModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.PostSampleLayoutBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostsAdapter(
    private val arrayList: ArrayList<PostModel>, private val context: Context
) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentUserName: String
    private lateinit var currentUserProfileUrl: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_sample_layout, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrayList[position]
        holder.binding.userName.text = data.userName

        val postUpdated = data.postDescription.take(250) + "... see more"

        if (data.postDescription.length > 250) {
            holder.binding.postDescription.text = postUpdated
        } else {
            holder.binding.postDescription.text = data.postDescription
        }

        val formattedTime = getRelativeTimeAgo(data.postTime)
        holder.binding.postTime.text = formattedTime

        sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        currentUserName = sharedPreferences.getString("userName", "").toString()
        currentUserProfileUrl = sharedPreferences.getString("profileImageUrl", "").toString()

        FirebaseDatabase.getInstance().getReference("Users")
            .child(data.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName = snapshot.child("name").value.toString()
                        val userProfileUrl = snapshot.child("profileImage").value.toString()

                        holder.binding.userName.text = userName
                        Picasso.get().load(userProfileUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.binding.profileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("postError", error.message)
                }
            })

        if (data.postLikes < 2) {
            holder.binding.totalLikesText.text = "${data.postLikes} Like"
        } else {
            holder.binding.totalLikesText.text = "${data.postLikes} Likes"
        }

        if (data.postComments < 2) {
            holder.binding.totalCommentsText.text = "${data.postComments} Comment"
        } else {
            holder.binding.totalCommentsText.text = "${data.postComments} Comments"
        }

        holder.binding.postDescription.setOnLongClickListener {
            copyToClipboard(data.postDescription)
            true
        }

        holder.binding.heartIcon.setOnClickListener {
            likePost(currentUserName, currentUserProfileUrl, data.postId, data.id)
        }

        holder.binding.heartIconFilled.setOnClickListener {
            unlikePost(data.postId)
        }

        // check if the user has liked the post
        checkIfLiked(data.postId, holder)

        holder.binding.constraintLayoutBg.setOnClickListener {
            goToCommentActivity(data.postId, data.id)
        }

        holder.binding.postDescription.setOnClickListener {
            goToCommentActivity(data.postId, data.id)
        }

        holder.binding.commentIcon.setOnClickListener {
            goToCommentActivity(data.postId, data.id)
        }
        holder.binding.totalCommentsText.setOnClickListener {
            goToCommentActivity(data.postId, data.id)
        }
    }

    override fun getItemCount() = arrayList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = PostSampleLayoutBinding.bind(itemView)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun likePost(userName: String, userProfileUrl: String, postId: String, id: String) {
        val postRef =
            FirebaseDatabase.getInstance().getReference("posts").child(postId).child("postLikes")

        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                    val newLikes = currentLikes + 1
                    postRef.setValue(newLikes)

                    FirebaseDatabase.getInstance().getReference("posts").child(postId)
                        .child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(true)

                    if (id != FirebaseAuth.getInstance().currentUser!!.uid) {
                        sendNotification(userName, userProfileUrl, postId, id)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkIfLiked(postId: String, holder: ViewHolder) {
        FirebaseDatabase.getInstance().getReference("posts").child(postId)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == true) {
                        holder.binding.heartIcon.visibility = View.INVISIBLE
                        holder.binding.heartIconFilled.visibility = View.VISIBLE
                    } else {
                        holder.binding.heartIcon.visibility = View.VISIBLE
                        holder.binding.heartIconFilled.visibility = View.INVISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", error.message)
                }
            })
    }

    private fun unlikePost(postId: String) {
        val postRef =
            FirebaseDatabase.getInstance().getReference("posts").child(postId).child("postLikes")

        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                    val newLikes = currentLikes - 1
                    postRef.setValue(newLikes)

                    FirebaseDatabase.getInstance().getReference("posts").child(postId)
                        .child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sendNotification(
        userName: String, userProfileUrl: String, postId: String, id: String
    ) {
        val notification = "$userName likes your post"
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)
        val time = formattedTime

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val notificationModel =
            NotificationModel(postId, currentUserId, userProfileUrl, notification, time)

        FirebaseDatabase.getInstance().getReference("Users").child(id).child("notifications").push()
            .setValue(notificationModel)
    }

    private fun goToCommentActivity(
        postId: String, userId: String
    ) {
        val intent = Intent(context, CommentsActivity::class.java)
        intent.putExtra("postId", postId)
        intent.putExtra("userId", userId)
        context.startActivity(intent)
    }

    fun getRelativeTimeAgo(dateString: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date: Date = dateFormat.parse(dateString) ?: return "unknown time"

        val now = Date()
        val diffInMillies = now.time - date.time
        val diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillies)
        val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillies)
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillies)
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillies)

        return when {
            diffInSeconds < 60 -> "just now"
            diffInMinutes < 60 -> "$diffInMinutes minute${if (diffInMinutes > 1) "s" else ""} ago"
            diffInHours < 24 -> "$diffInHours hour${if (diffInHours > 1) "s" else ""} ago"
            diffInDays < 7 -> "$diffInDays day${if (diffInDays > 1) "s" else ""} ago"
            diffInDays < 30 -> "${diffInDays / 7} week${if ((diffInDays / 7) > 1) "s" else ""} ago"
            diffInDays < 365 -> "${diffInDays / 30} month${if ((diffInDays / 30) > 1) "s" else ""} ago"
            else -> "${diffInDays / 365} year${if ((diffInDays / 365) > 1) "s" else ""} ago"
        }
    }
}
