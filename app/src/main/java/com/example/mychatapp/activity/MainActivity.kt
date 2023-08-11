package com.example.mychatapp.activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.mychatapp.R
import com.example.mychatapp.databinding.ActivityMainBinding
import com.example.mychatapp.fragments.HomeFragment
import com.example.mychatapp.fragments.StatusFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var firbaseauth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         binding = ActivityMainBinding.inflate(layoutInflater)
         setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firbaseauth = FirebaseAuth.getInstance()
        setSupportActionBar(binding.toolbar)


        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        SetNotification()

    }
    private fun SetNotification() {
        FirebaseMessaging.getInstance()
            .token.addOnSuccessListener { s ->
                val map = HashMap<String, Any>()
                map["token"] = s
                firebaseDatabase.reference.child("users")
                    .child(firbaseauth.getUid()!!)
                    .updateChildren(map)
            }
    }

    class ViewPagerAdapter(fm: FragmentManager?) :
        FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            var fragment: Fragment? = null
            if (position == 0) {
                fragment = HomeFragment()
            } else if (position == 1) {
                fragment = StatusFragment()
            }
            return fragment!!
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            var title: String? = null
            if (position == 0) {
                title = "Chats"
            } else if (position == 1) {
                title = "Status"
            }
            return title
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.setting_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.setting -> {
                val intent = Intent(this,SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


}