package com.shanalimughal.mentalhealthai.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shanalimughal.mentalhealthai.Activities.MeditationDetailActivity
import com.shanalimughal.mentalhealthai.Models.MeditationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.MeditationSampleLayoutBinding

class MeditationAdapter(
    private val meditationList: ArrayList<MeditationModel>, private val context: Context
) : RecyclerView.Adapter<MeditationAdapter.MediationViewHolder>() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.meditation_sample_layout, parent, false)
        return MediationViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: MediationViewHolder, position: Int) {
        val meditation = meditationList[position]
        holder.binding.meditationThumbnail.setImageResource(meditation.meditationPlaceHolder)
        holder.binding.meditationGoals.text = meditation.meditationGoals.joinToString(", ")
        holder.binding.sleepHours.text = meditation.hoursOfSleep
        holder.binding.meditationDuration.text = meditation.meditationDuration
        holder.binding.meditationDescription.text = meditation.meditationDescription

        var updatedResponse =
            meditation.meditationTitle.replace("*", "").replace("#", "").replace("\n", " ")
                .take(200) + "..."

        if (updatedResponse[0].toString() == " ") {
            updatedResponse = meditation.meditationTitle.replace("*", "")
                .replaceFirst(" ", "")
                .replace("#", "")
                .replace("\n", " ")
                .take(200) + "..."
        }

        holder.binding.meditationTitle.text = updatedResponse

        sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val progress = sharedPreferences.getFloat(meditation.meditationTitle, 0f)
        holder.binding.progressBar.progress = progress.toInt()

        Log.d("medTitle", meditation.meditationTitle)

        val formattedValue = String.format("%.2f", progress)
        holder.binding.progress.text = "Last Time Progress: ${formattedValue}%"

        holder.binding.viewDetails.setOnClickListener {
            val intent = Intent(context, MeditationDetailActivity::class.java)
            intent.putExtra("meditationDescription", meditation.meditationTitle)
            intent.putExtra("currentClass", "main")
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = meditationList.size

    class MediationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MeditationSampleLayoutBinding.bind(itemView)
    }
}
