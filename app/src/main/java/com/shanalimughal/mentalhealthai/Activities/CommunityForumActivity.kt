package com.shanalimughal.mentalhealthai.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.shanalimughal.mentalhealthai.Fragments.CommunityHomeFragment
import com.shanalimughal.mentalhealthai.Fragments.CreatePostFragment
import com.shanalimughal.mentalhealthai.Fragments.MyPostsFragment
import com.shanalimughal.mentalhealthai.Fragments.NotificationsFragment
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityCommunityForumBinding

class CommunityForumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommunityForumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityForumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backArrowIcon.setOnClickListener {
            onBackPressed()
        }

        binding.communityTopText.text = resources.getString(R.string.community)
        replaceFragment(CommunityHomeFragment())

        binding.communityBottomBar.onItemSelected = {
            when (it) {
                0 -> {
                    binding.communityTopText.text = resources.getString(R.string.community)
                    replaceFragment(CommunityHomeFragment())
                }

                1 -> {
                    binding.communityTopText.text = "Create Post"
                    replaceFragment(CreatePostFragment())
                }

                2 -> {
                    binding.communityTopText.text = resources.getString(R.string.notifications)
                    replaceFragment(NotificationsFragment())
                }

                3 -> {
                    binding.communityTopText.text = "My Posts"
                    replaceFragment(MyPostsFragment())
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.communityLinearLayout, fragment)
        fragmentTransaction.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}