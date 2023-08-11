package com.example.mychatapp.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.activity.MainActivity
import com.example.mychatapp.adapter.TopStatusAdapter
import com.example.mychatapp.databinding.FragmentStatusBinding
import com.example.mychatapp.model.Status
import com.example.mychatapp.model.User
import com.example.mychatapp.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*


class StatusFragment : Fragment() {
    lateinit var binding:FragmentStatusBinding
    lateinit var topStatusAdapter: TopStatusAdapter
    lateinit var userStatus: ArrayList<UserStatus>
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var firbaseauth : FirebaseAuth
    lateinit var firebaseStorage: FirebaseStorage

    var dialog: ProgressDialog? = null
    lateinit var user :User


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentStatusBinding.inflate(inflater, container, false)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firbaseauth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        userStatus = ArrayList<UserStatus>()

        setStatusAdapter()

        dialog = ProgressDialog(requireContext())
        dialog!!.setMessage("Uploading Image....")
        dialog!!.setCancelable(false)

        firebaseDatabase.reference.child("stories")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        userStatus.clear()
                        for (storysnapshot in snapshot.children){
                            val status = UserStatus()
                            status.uid= storysnapshot.child("uid").getValue(String::class.java)
                            status.name= storysnapshot.child("name").getValue(String::class.java)
                            status.profileImage= storysnapshot.child("profileImage").getValue(String::class.java)
                            status.lastUpdated= storysnapshot.child("lastUpdated").getValue(Long::class.java)
                            if (!status.uid.equals(firbaseauth.uid))userStatus.add(status)

                            val statuses:ArrayList<Status> = ArrayList<Status>()

                            for (statusSnapshot in storysnapshot.child("statuses").children){
                                val samplestatus = statusSnapshot.getValue(Status::class.java)
                                statuses.add(samplestatus!!)

                            }

                            status.status = statuses
//                            userStatus.add(status)

                            if (status.uid.equals(firbaseauth.uid)){

                                if (status.status!=null){
                                    if (status.status!!.size!=0){
                                        val lastStatus: Status = status.status!!.get(status.status!!.size - 1)

                                        Glide.with(requireContext())
                                            .load(lastStatus.imageUrl)
                                            .placeholder(R.drawable.person_crop_circle)
                                            .into(binding.statusImage)
                                        binding.statusCircleView.setPortionsCount(status.status!!.size)

                                    }

                                    val myStories: ArrayList<MyStory> = ArrayList()
                                    for (status1 in status.status!!) {
                                        myStories.add(MyStory(status1.imageUrl))
                                    }
                                    binding.relativeLayout.setOnClickListener{
                                        StoryView.Builder((context as MainActivity).supportFragmentManager)
                                            .setStoriesList(myStories)
                                            .setStoryDuration(5000)
                                            .setTitleText(status.name)
                                            .setSubtitleText("")
                                            .setTitleLogoUrl(status.profileImage)
                                            .setStoryClickListeners(object : StoryClickListeners {
                                                override fun onDescriptionClickListener(position: Int) {
                                                }

                                                override fun onTitleIconClickListener(position: Int) {
                                                }
                                            })
                                            .build()
                                            .show()
                                    }


                                }

                            }


                        }
                        topStatusAdapter.notifyDataSetChanged()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
                }
            })

        firebaseDatabase.reference.child("users").child(firbaseauth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                    Glide.with(requireContext())
                        .load(user.profileImage)
                        .placeholder(R.drawable.person_crop_circle)
                        .into(binding.statusImage)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
                }
            })

        binding.addStatus.setOnClickListener {
            var intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)

        }
        binding.relativeLayout.setOnClickListener{
            var intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent,45)
        }

        return binding.root
    }

    private fun setStatusAdapter() {
        topStatusAdapter= TopStatusAdapter(requireContext(),userStatus)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.statusRecycler.layoutManager = layoutManager
        binding.statusRecycler.adapter = topStatusAdapter

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data !=null){
            if (data.data!=null){
                dialog!!.show()
                var date = Date()
                var reference =  firebaseStorage.reference.child("status").child(date.time.toString()+"")

                reference.putFile(data.data!!).addOnCompleteListener{task->
                    if (task.isSuccessful){
                        reference.downloadUrl.addOnSuccessListener {uri ->
                            val userStatus = UserStatus()
                            userStatus.uid = firbaseauth.uid
                            userStatus.name = user.name
                            userStatus.profileImage = user.profileImage
                            userStatus.lastUpdated = date.time

                            val obj = HashMap<String, Any>()
                            obj.put("uid",userStatus.uid!!)
                            obj.put("name", userStatus.name!!)
                            obj.put("profileImage",userStatus.profileImage!!)
                            obj.put("lastUpdated",userStatus.lastUpdated!!)

                            var imageUrl = uri.toString()
                            val status = Status(imageUrl,userStatus.lastUpdated!!)

                            firebaseDatabase.reference.child("stories")
                                .child(firbaseauth.uid!!)
                                .updateChildren(obj)

                            firebaseDatabase.reference.child("stories")
                                .child(firbaseauth.uid!!)
                                .child("statuses")
                                .push()
                                .setValue(status)

                            dialog!!.dismiss()

                        }
                    }

                }

            }

        }
    }

}