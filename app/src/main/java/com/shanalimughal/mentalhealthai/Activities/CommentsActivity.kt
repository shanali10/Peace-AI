package com.shanalimughal.mentalhealthai.Activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.CommentsAdapter
import com.shanalimughal.mentalhealthai.Models.CommentsModel
import com.shanalimughal.mentalhealthai.Models.NotificationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityCommentsBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var arrayList: ArrayList<CommentsModel>
    private lateinit var adapter: CommentsAdapter

    private val REQUEST_CODE_SPEECH_INPUT = 101

    private lateinit var getPostId: String
    private lateinit var getUserId: String

    private lateinit var getUserName: String
    private lateinit var getProfileUrl: String

    private lateinit var progressDialog: ProgressDialog
    private lateinit var progressDialogNew: ProgressDialog

    private var totalPostLikes: Int = 0
    private var totalPostComments: Int = 0

    private lateinit var mediaPlayer: MediaPlayer

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        mediaPlayer = MediaPlayer.create(this, R.raw.send_sound)

        getPostId = intent.getStringExtra("postId").toString()
        getUserId = intent.getStringExtra("userId").toString()

        getUserName = sharedPreferences.getString("userName", "").toString()
        getProfileUrl = sharedPreferences.getString("profileImageUrl", "").toString()

        arrayList = ArrayList()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        adapter = CommentsAdapter(arrayList, this)
        val layoutManager = LinearLayoutManager(this)

        binding.commentsRecycler.adapter = adapter
        binding.commentsRecycler.layoutManager = layoutManager

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.micIcon.visibility = View.INVISIBLE
                binding.sendIcon.visibility = View.VISIBLE
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.micIcon.visibility = View.VISIBLE
                    binding.sendIcon.visibility = View.INVISIBLE
                } else {
                    binding.micIcon.visibility = View.INVISIBLE
                    binding.sendIcon.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // nothing to do here
            }

        })

        binding.micIcon.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...")
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        }

        binding.sendIcon.setOnClickListener {
            addComment()
        }

        binding.backArrowIcon.setOnClickListener {
            onBackPressed()
        }

        binding.heartIcon.setOnClickListener {
            binding.heartIcon.visibility = View.INVISIBLE
            binding.heartIconFilled.visibility = View.VISIBLE

            if (totalPostLikes < 1) {
                binding.totalLikesText.text = "${totalPostLikes + 1} Like"
            } else {
                binding.totalLikesText.text = "${totalPostLikes + 1} Likes"
            }

            likePost(getUserName, getProfileUrl, getPostId, getUserId)
        }

        binding.heartIconFilled.setOnClickListener {
            binding.heartIcon.visibility = View.VISIBLE
            binding.heartIconFilled.visibility = View.INVISIBLE

            if (totalPostLikes < 3) {
                binding.totalLikesText.text = "${totalPostLikes - 1} Like"
            } else {
                binding.totalLikesText.text = "${totalPostLikes - 1} Likes"
            }

            unlikePost(getPostId)
        }

        // getting user details and post details
        userDetails()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            binding.editTextText.setText(result?.get(0).toString())
        }
    }

    private fun userDetails() {
        progressDialog.show()
        FirebaseDatabase.getInstance().getReference("posts")
            .child(getPostId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val postDescription = snapshot.child("postDescription").value.toString()
                        totalPostLikes = snapshot.child("postLikes").value.toString().toInt()
                        totalPostComments =
                            snapshot.child("postComments").value.toString().toInt()
                        val time = snapshot.child("postTime").value.toString()

                        val userId = snapshot.child("id").value.toString()
                        val formattedTime = getRelativeTimeAgo(time)

                        val userRef = FirebaseDatabase.getInstance().getReference("Users")
                            .child(userId)
                        userRef
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val userName =
                                            snapshot.child("name").value.toString()
                                        val userProfileUrl =
                                            snapshot.child("profileImage").value.toString()


                                        binding.userName.text = userName
                                        binding.postDescription.text = postDescription
                                        binding.postTime.text = formattedTime


                                        if (totalPostLikes < 2) {
                                            binding.totalLikesText.text = "$totalPostLikes Like"
                                        } else {
                                            binding.totalLikesText.text = "$totalPostLikes Likes"
                                        }

                                        if (totalPostComments < 2) {
                                            binding.totalCommentsText.text =
                                                "$totalPostComments Comment"
                                        } else {
                                            binding.totalCommentsText.text =
                                                "$totalPostComments Comments"
                                        }

                                        Picasso.get()
                                            .load(userProfileUrl)
                                            .placeholder(R.drawable.profile_placeholder)
                                            .into(binding.profileImage)

                                        // checking if the current post is liked by the current user
                                        checkIfLiked(getPostId)
                                        // fetching comments
                                        fetchComments()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }

                            })

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                    Toast.makeText(this@CommentsActivity, "Some error occurred", Toast.LENGTH_SHORT)
                        .show()
                }

            })
    }

    private fun fetchComments() {
        FirebaseDatabase.getInstance().getReference("posts")
            .child(getPostId)
            .child("comments")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        arrayList.clear()
                        for (snap in snapshot.children) {
                            val data = snap.getValue(CommentsModel::class.java)
                            if (data != null) {

                                arrayList.add(0, data)

                                adapter.notifyDataSetChanged()
                                progressDialog.dismiss()

                                if (arrayList.isEmpty()) {
                                    binding.noCommentsText.visibility = View.VISIBLE
                                } else {
                                    binding.noCommentsText.visibility =
                                        View.INVISIBLE
                                }

                                binding.constraintLayoutBg.visibility =
                                    View.VISIBLE
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        binding.noCommentsText.visibility = View.VISIBLE
                        binding.constraintLayoutBg.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("error", "Some error occurred: ${error.message}")
                    progressDialog.dismiss()
                    Toast.makeText(this@CommentsActivity, "Some error occurred", Toast.LENGTH_SHORT)
                        .show()
                }
            })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addComment() {
        progressDialogNew = ProgressDialog(this@CommentsActivity)
        progressDialogNew.setMessage("Uploading comment...")
        progressDialogNew.show()

        val comment = binding.editTextText.text.toString()
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        val model = CommentsModel(currentUserId, getUserName, getProfileUrl, comment, formattedTime)

        FirebaseDatabase.getInstance().getReference("posts")
            .child(getPostId)
            .child("comments")
            .push()
            .setValue(model)
            .addOnSuccessListener {
                updateTotalComments()
            }
    }

    private fun updateTotalComments() {
        val ref = FirebaseDatabase.getInstance().getReference("posts").child(getPostId)

        ref.child("postComments").addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalComments = snapshot.getValue(Int::class.java) ?: 0
                ref.child("postComments").setValue(totalComments + 1)

                if (getUserId == FirebaseAuth.getInstance().currentUser!!.uid) {
                    binding.editTextText.text.clear()

                    if (arrayList.isEmpty()) {
                        binding.noCommentsText.visibility = View.VISIBLE
                    } else {
                        binding.noCommentsText.visibility = View.INVISIBLE
                    }

                    progressDialogNew.dismiss()
                    mediaPlayer.start()

                    // getting latest details of the user and the post
                    userDetails()
                } else {
                    sendNotification()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CommentsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
                progressDialogNew.dismiss()
            }

        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sendNotification() {
        val notification = "$getUserName has commented on your post"
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        val notificationModel =
            NotificationModel(getPostId, currentUserId, getProfileUrl, notification, formattedTime)

        FirebaseDatabase.getInstance().getReference("Users")
            .child(getUserId)
            .child("notifications")
            .push()
            .setValue(notificationModel)
            .addOnSuccessListener {
                binding.editTextText.text.clear()
                progressDialogNew.dismiss()
                mediaPlayer.start()

                if (arrayList.isEmpty()) {
                    binding.noCommentsText.visibility = View.VISIBLE
                } else {
                    binding.noCommentsText.visibility = View.INVISIBLE
                }

                // getting latest details of the user and the post
                userDetails()
            }
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

    private fun likePost(userName: String, userProfileUrl: String, postId: String, id: String) {
        val postRef = FirebaseDatabase.getInstance().getReference("posts")
            .child(postId)
            .child("postLikes")

        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                    val newLikes = currentLikes + 1
                    postRef.setValue(newLikes)

                    FirebaseDatabase.getInstance().getReference("posts")
                        .child(postId)
                        .child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .setValue(true)

                    // getting latest details of the user and the post
                    userDetails()

                    if (id != FirebaseAuth.getInstance().currentUser!!.uid) {
                        sendNotificationForLike(userName, userProfileUrl, postId, id)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CommentsActivity, "Some error occurred", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun checkIfLiked(postId: String) {
        FirebaseDatabase.getInstance().getReference("posts")
            .child(postId)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val value = snapshot.value
                        if (value == true) {
                            binding.heartIcon.visibility = View.INVISIBLE
                            binding.heartIconFilled.visibility = View.VISIBLE
                        } else {
                            binding.heartIcon.visibility = View.VISIBLE
                            binding.heartIconFilled.visibility = View.INVISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", error.message)
                }

            })
    }

    private fun unlikePost(postId: String) {
        val postRef = FirebaseDatabase.getInstance().getReference("posts")
            .child(postId)
            .child("postLikes")

        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                    val newLikes = currentLikes - 1
                    postRef.setValue(newLikes)

                    FirebaseDatabase.getInstance().getReference("posts")
                        .child(postId)
                        .child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .setValue(false)

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CommentsActivity, "Some error occurred", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sendNotificationForLike(
        userName: String,
        userProfileUrl: String,
        postId: String,
        id: String
    ) {
        val notification = "$userName likes your post"
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        val notificationModel =
            NotificationModel(postId, currentUserId, userProfileUrl, notification, formattedTime)

        FirebaseDatabase.getInstance().getReference("Users")
            .child(id)
            .child("notifications")
            .push()
            .setValue(notificationModel)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.stop()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        progressDialog.dismiss()
        finish()
    }

}