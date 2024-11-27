package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.TherapySessionsHistoryAdapter
import com.shanalimughal.mentalhealthai.Models.TherapySummaryModel
import com.shanalimughal.mentalhealthai.databinding.ActivityTherapySessionHistoryBinding

class TherapySessionHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTherapySessionHistoryBinding

    private lateinit var arrayList: ArrayList<TherapySummaryModel>
    private lateinit var adapter: TherapySessionsHistoryAdapter

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTherapySessionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        arrayList = ArrayList()
        adapter = TherapySessionsHistoryAdapter(arrayList, this)
        binding.therapySessionHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.therapySessionHistoryRecycler.adapter = adapter

        binding.backArrow.setOnClickListener {
            onBackPressed()
        }

        // fetching history
        fetchHistory()
    }

    private fun fetchHistory() {
        progressDialog.show()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("therapySessions")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (snaps in snapshot.children) {
                        val model = snaps.getValue(TherapySummaryModel::class.java)
                        val id = snaps.child("id").value as String
                        if (model != null) {
                            model.id = id
                            arrayList.add(
                                0, TherapySummaryModel(
                                    model.summary,
                                    model.date
                                )
                            )
                        }
                    }
                    adapter.notifyDataSetChanged()

                    if (arrayList.isEmpty()) {
                        binding.noHistoryText.visibility = View.VISIBLE
                    } else {
                        binding.noHistoryText.visibility = View.INVISIBLE
                    }

                    progressDialog.dismiss()
                } else {
                    progressDialog.dismiss()
                    binding.noHistoryText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                binding.noHistoryText.text = "Some error occurred"
            }

        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}