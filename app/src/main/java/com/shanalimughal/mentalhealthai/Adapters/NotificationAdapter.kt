package com.shanalimughal.mentalhealthai.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Activities.CommentsActivity
import com.shanalimughal.mentalhealthai.Models.NotificationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.NotificationSampleLayoutBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val arrayList: ArrayList<NotificationModel>,
    private val context: Context
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.notification_sample_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrayList[position]

        holder.binding.notificationDescription.text = data.notificationDescription

        val formattedTime = getRelativeTimeAgo(data.notificationTime)
        holder.binding.notificationTime.text = formattedTime

        FirebaseDatabase.getInstance().getReference("Users")
            .child(data.userId)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userName =
                            snapshot.child("name").value.toString()
                        val userProfileUrl =
                            snapshot.child("profileImage").value.toString()

                        if (data.notificationDescription.contains("likes your post")
                        ) {
                            holder.binding.notificationDescription.text =
                                "$userName likes your post"
                        } else {
                            holder.binding.notificationDescription.text =
                                "$userName has commented on your post"
                        }

                        Picasso.get().load(userProfileUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.binding.profileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("notifyError", error.message)
                }

            })

        holder.binding.constraintLayoutBg.setOnClickListener {
            val intent = Intent(context, CommentsActivity::class.java)
            intent.putExtra("postId", data.postId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = arrayList.size


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = NotificationSampleLayoutBinding.bind(itemView)
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