package com.example.mychatapp.fragments

import android.animation.Animator
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mychatapp.activity.AllUserActivity
import com.example.mychatapp.activity.ChatActivity
import com.example.mychatapp.adapter.RecentUserAdapter
import com.example.mychatapp.databinding.FragmentHomeBinding
import com.example.mychatapp.interfac.UserClick
import com.example.mychatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class HomeFragment : Fragment(),UserClick {
    lateinit var binding:FragmentHomeBinding

    var dialog: ProgressDialog?=null
    var users: ArrayList<User>?=null
    private var userAdapter: RecentUserAdapter?=null
    lateinit var user: User
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var databaseReference: DatabaseReference
    lateinit var firbaseauth : FirebaseAuth

    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firbaseauth = FirebaseAuth.getInstance()
        databaseReference = firebaseDatabase.getReference("users")
        users = ArrayList<User>()
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)


        dialog=ProgressDialog(requireContext())
        dialog!!.setMessage("Updating Image...")
        dialog!!.setCancelable(false)

        databaseReference.child(firbaseauth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
                }
            })

        databaseReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users!!.clear()
                for (snapshot1 in snapshot.children){
                    val userr:User =snapshot1.getValue(User::class.java)!!
                    if (!userr.uid.equals(firbaseauth.uid))users!!.add(userr)
                }
                if (users!!.size!=0){
                    binding.recycler.visibility = View.VISIBLE
                    binding.noData.visibility = View.GONE

                }else{
                    binding.recycler.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE

                }
                userAdapter!!.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }

        })


        binding.fabNewChat.setOnClickListener{
            val intent = Intent(requireContext() , AllUserActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }


    private fun setAdapter() {
        userAdapter= RecentUserAdapter(requireContext(),users!!,this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.layoutManager = layoutManager
        binding.recycler.adapter = userAdapter
        Collections.sort(users, CustomComparator())

    }
    class CustomComparator : Comparator<User?> {
        override fun compare(p0: User?, p1: User?): Int {
            return p0!!.name!!.compareTo(p1!!.name!!)
        }
    }

    override fun onResume() {
        super.onResume()
        setAdapter()
        val currentId = firbaseauth.uid
        firebaseDatabase.reference.child("presence")
            .child(currentId!!).setValue("Online")
    }


    override fun onPause() {
        super.onPause()
        val currentId = firbaseauth.uid
        firebaseDatabase.reference.child("presence")
            .child(currentId!!).setValue("Offline")
    }

    override fun onUserClick(users: User) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("name",users.name)
            intent.putExtra("image",users.profileImage)
            intent.putExtra("uid",users.uid)
            intent.putExtra("token",users.token)
            intent.putExtra("phonenumber",users.phoneNumber)
            intent.putExtra("currentUser",user.name)
            startActivity(intent)
    }

    override fun zoomImageFromThumb(user: User, view: View) {


    }
}