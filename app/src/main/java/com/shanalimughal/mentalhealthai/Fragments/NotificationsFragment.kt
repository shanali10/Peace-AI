package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.NotificationAdapter
import com.shanalimughal.mentalhealthai.Models.NotificationModel
import com.shanalimughal.mentalhealthai.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding

    private lateinit var arrayList: ArrayList<NotificationModel>
    private lateinit var adapter: NotificationAdapter

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        arrayList = ArrayList()

        adapter = NotificationAdapter(arrayList, requireContext())
        val layoutManager = LinearLayoutManager(requireContext())

        binding.notificationRecycler.adapter = adapter
        binding.notificationRecycler.layoutManager = layoutManager

        // progress dialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        // getting all notifications
        getNotifications()
        return binding.root
    }

    private fun getNotifications() {
        progressDialog.show()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        ref.child("notifications").addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    arrayList.clear()
                    for (items in snapshot.children) {
                        val data = items.getValue(NotificationModel::class.java)
                        val postId = items.child("postId").value.toString()
                        val userId = items.child("userId").value.toString()

                        Log.d("userId", userId)
                        Log.d("userId", "current: ${FirebaseAuth.getInstance().currentUser!!.uid}")

                        if (data != null) {
                            data.postId = postId
                            data.userId = userId

                            arrayList.add(
                                0,
                                data
                            )
                            progressDialog.dismiss()
                            adapter.notifyDataSetChanged()

                            if (arrayList.isEmpty()) {
                                binding.noNotificationsText.visibility =
                                    View.VISIBLE
                            }
                        }
                    }
                } else {
                    binding.noNotificationsText.visibility = View.VISIBLE
                    progressDialog.dismiss()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.noNotificationsText.visibility = View.VISIBLE
                binding.noNotificationsText.text = "Some error occurred"

                progressDialog.dismiss()
            }

        })
    }
}