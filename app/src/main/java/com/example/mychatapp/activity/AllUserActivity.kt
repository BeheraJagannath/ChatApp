package com.example.mychatapp.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mychatapp.R
import com.example.mychatapp.adapter.UserAdapter
import com.example.mychatapp.databinding.ActivityAllUserBinding
import com.example.mychatapp.interfac.UserClick
import com.example.mychatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class AllUserActivity : AppCompatActivity(),UserClick {
    lateinit var binding: ActivityAllUserBinding

    var dialog: ProgressDialog?=null
    var users: ArrayList<User>?=null
    lateinit var userAdapter: UserAdapter
    lateinit var user: User
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var databaseReference: DatabaseReference
    lateinit var firbaseauth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firbaseauth = FirebaseAuth.getInstance()
        databaseReference = firebaseDatabase.getReference("users")
        users = ArrayList<User>()

        dialog=ProgressDialog(this)
        dialog!!.setMessage("Updating Image...")
        dialog!!.setCancelable(false)

        setSupportActionBar(binding.toolbar)


        databaseReference.child(firbaseauth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AllUserActivity, "Fail to get data.", Toast.LENGTH_SHORT).show()
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
                    binding.allUserRecycler.visibility = View.VISIBLE
                    binding.noData.visibility = View.GONE

                }else{
                    binding.allUserRecycler.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE

                }
                userAdapter.notifyDataSetChanged()
                binding.totalContacts.text = users!!.size.toString()+" contacts"

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AllUserActivity, "Fail to get data.", Toast.LENGTH_SHORT).show()
            }

        })
        binding.alluserBack.setOnClickListener{
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView?

        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filter(newText)
                return false
            }
        })
        return true
    }
    private fun filter(text: String) {
        val filteredlist: ArrayList<User> = ArrayList<User>()

        for (item in users!!) {
            if (item.name!!.toLowerCase().contains(text.lowercase(Locale.getDefault()))) {
                filteredlist.add(item)
            }
        }
        if (filteredlist.isEmpty()) {
            Toast.makeText(this, "No Data Found..", Toast.LENGTH_SHORT).show()
        } else {
            userAdapter.filterList(filteredlist)
        }
    }



    private fun setAdapter() {
        userAdapter= UserAdapter(this@AllUserActivity,users!!,this)
        val layoutManager = LinearLayoutManager(this@AllUserActivity)
        binding.allUserRecycler.layoutManager = layoutManager
        binding.allUserRecycler.adapter = userAdapter

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
            val intent = Intent(this,ChatActivity::class.java)
            intent.putExtra("name",users.name)
            intent.putExtra("image",users.profileImage)
            intent.putExtra("uid",users.uid)
            intent.putExtra("token",users.token)
            intent.putExtra("phonenumber",users.phoneNumber)
            intent.putExtra("currentUser",user.name)
            startActivity(intent)
            finish()
    }

    override fun zoomImageFromThumb(imageurl: User, binding: View) {
        TODO("Not yet implemented")
    }

}