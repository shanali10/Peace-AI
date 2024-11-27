package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.PostsAdapter
import com.shanalimughal.mentalhealthai.Models.PostModel
import com.shanalimughal.mentalhealthai.databinding.FragmentCommunityHomeBinding

class CommunityHomeFragment : Fragment() {

    private lateinit var binding: FragmentCommunityHomeBinding
    private lateinit var arrayList: ArrayList<PostModel>
    private lateinit var adapter: PostsAdapter

    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCommunityHomeBinding.inflate(inflater, container, false)

        firebaseDatabase = FirebaseDatabase.getInstance()

        arrayList = ArrayList()

        adapter = PostsAdapter(arrayList, requireContext())
        binding.communityHomeRecycler.adapter = adapter
        binding.communityHomeRecycler.layoutManager = LinearLayoutManager(requireContext())

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        fetchPosts()
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchPosts() {
        progressDialog.show()

        firebaseDatabase.getReference().child("posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        arrayList.clear()
                        for (postSnapshot in snapshot.children) {
                            val postId = postSnapshot.key.toString()
                            val id = postSnapshot.child("id").value.toString()
                            val postModel = postSnapshot.getValue(PostModel::class.java)

                            if (postModel != null) {
                                postModel.postId = postId
                                postModel.id = id

                                arrayList.add(0, postModel)
                                progressDialog.dismiss()
                                adapter.notifyDataSetChanged()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "No posts found", Toast.LENGTH_SHORT)
                            .show()
                        progressDialog.dismiss()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Some error occurred", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }
}