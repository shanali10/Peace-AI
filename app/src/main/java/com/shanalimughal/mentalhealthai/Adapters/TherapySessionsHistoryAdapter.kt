package com.shanalimughal.mentalhealthai.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shanalimughal.mentalhealthai.Activities.TherapySummaryDetailActivity
import com.shanalimughal.mentalhealthai.Models.TherapySummaryModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.TherapySessionHistorySampleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TherapySessionsHistoryAdapter(
    private val arrayList: ArrayList<TherapySummaryModel>,
    private val context: Context
) : RecyclerView.Adapter<TherapySessionsHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.therapy_session_history_sample, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrayList[position]
        val formattedTime = getRelativeTimeAgo(data.date)
        holder.binding.therapyTime.text = "Session Time: $formattedTime"

        val updatedResponse = data.summary.replace("*", "")
            .replace("#", "")
            .replace("\n", " ")
            .take(150) + "..."

        holder.binding.therapySummary.text = updatedResponse

        holder.binding.viewSummary.setOnClickListener {
            val intent = Intent(context, TherapySummaryDetailActivity::class.java)
            intent.putExtra("summary", data.summary)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = arrayList.size


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = TherapySessionHistorySampleBinding.bind(itemView)
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