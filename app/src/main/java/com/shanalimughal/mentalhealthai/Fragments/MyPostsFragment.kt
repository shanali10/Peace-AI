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
import com.shanalimughal.mentalhealthai.Adapters.PostsAdapter
import com.shanalimughal.mentalhealthai.Models.PostModel
import com.shanalimughal.mentalhealthai.databinding.FragmentMyPostsBinding

class MyPostsFragment : Fragment() {
    private lateinit var binding: FragmentMyPostsBinding
    private lateinit var arrayList: ArrayList<PostModel>
    private lateinit var adapter: PostsAdapter

    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMyPostsBinding.inflate(inflater, container, false)

        firebaseDatabase = FirebaseDatabase.getInstance()

        arrayList = ArrayList()

        adapter = PostsAdapter(arrayList, requireContext())
        val layoutManager = LinearLayoutManager(requireContext())

        binding.myPostsRecycler.adapter = adapter
        binding.myPostsRecycler.layoutManager = layoutManager

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        fetchPosts()
        return binding.root
    }

    private fun fetchPosts() {
        progressDialog.show()
        firebaseDatabase.getReference("posts").addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                arrayList.clear()
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                var hasPosts = false

                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        val postId = postSnapshot.key // Getting the unique postId
                        val id = postSnapshot.child("id").value as String
                        val postModel = postSnapshot.getValue(PostModel::class.java)

                        if (postModel != null && currentUserId == id) {
                            postModel.postId = postId.toString()
                            postModel.id = id
                            arrayList.add(0, postModel)
                            hasPosts = true
                        }
                    }
                    progressDialog.dismiss()

                    if (hasPosts) {
                        adapter.notifyDataSetChanged()
                        binding.noPostFoundText.visibility = View.GONE
                    } else {
                        binding.noPostFoundText.visibility = View.VISIBLE
                    }
                } else {
                    progressDialog.dismiss()
                    binding.noPostFoundText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("postsError", error.message)
                progressDialog.dismiss()
                binding.noPostFoundText.visibility = View.VISIBLE
                binding.noPostFoundText.text = "Some error occurred"
            }
        })
    }
}
