package com.shanalimughal.mentalhealthai.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shanalimughal.mentalhealthai.Activities.EnvironmentDetailsActivity
import com.shanalimughal.mentalhealthai.Models.EnvModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.MeditationSampleLayoutBinding

class EnvironmentAdapter(
    private val arrayList: ArrayList<EnvModel>,
    private val context: Context
) : RecyclerView.Adapter<EnvironmentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.meditation_sample_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = arrayList[position]
        holder.binding.meditationThumbnail.setImageResource(data.envPlaceHolder)
        holder.binding.meditationGoals.text = data.envInterestedAreas.joinToString(", ")
        holder.binding.sleepHours.text = data.envImportance
        holder.binding.meditationDuration.text = data.envPreferredInfo

        holder.binding.progress.visibility = View.GONE
        holder.binding.progressBar.visibility = View.GONE

        holder.binding.viewDetails.text = "Read More"

        var updatedResponse =
            data.envTitle.replace("*", "").replace("#", "").replace("\n", " ")
                .take(200) + "..."

        if (updatedResponse[0].toString() == " ") {
            updatedResponse = data.envTitle.replace("*", "")
                .replaceFirst(" ", "")
                .replace("#", "")
                .replace("\n", " ")
                .take(200) + "..."
        }

        holder.binding.meditationTitle.text = updatedResponse

        holder.binding.viewDetails.setOnClickListener {
            val intent = Intent(context, EnvironmentDetailsActivity::class.java)
            intent.putExtra("envDescription", data.envTitle)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = arrayList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MeditationSampleLayoutBinding.bind(itemView)
    }

}