package com.example.mychatapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.databinding.ItemContainerBinding
import com.example.mychatapp.Utils.Util
import com.example.mychatapp.interfac.UserClick
import com.example.mychatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.sql.Date

class RecentUserAdapter(var context:Context ,var userList: ArrayList<User>,var userClick: UserClick ): RecyclerView.Adapter<RecentUserAdapter.UserViewHolder>() {

    lateinit var firebaseDatabase: FirebaseDatabase


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val v =LayoutInflater.from(context).inflate(R.layout.item_container,parent,false)
        return UserViewHolder(v)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        firebaseDatabase = FirebaseDatabase.getInstance()
        holder.binding.name.text = user.name
        val senderId = FirebaseAuth.getInstance().uid
        val senderRoom = senderId+user.uid
        firebaseDatabase.reference.child("chats")
            .child(senderRoom)
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val lastmsg =snapshot.child("lastMsg").getValue(String::class.java)
                        val time = snapshot.child("lastMsgTime").getValue(Long::class.java)
                        holder.binding.date.text  = Util.dateDayTime(Date(time!!))
                        holder.binding.lastMessage.text = lastmsg

                    }else{
                        holder.binding.lastMessage.text = "Tap to chart"

                    }


                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.binding.picture.setOnClickListener{
           userClick.zoomImageFromThumb(user, holder.binding.picture)

        }


        Glide.with(context)
            .load(user.profileImage)
            .placeholder(R.drawable.person_crop_circle)
            .into(holder.binding.picture)

        holder.itemView.setOnClickListener{
            userClick.onUserClick(user)
        }
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseDatabase.reference.child("presence")
            .child(user.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val status = snapshot.getValue(String::class.java)
                        if (status=="Offline"){
                            holder.binding.online.visibility = View.GONE

                        }else{
                            holder.binding.online.visibility = View.VISIBLE

                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

    }


    override fun getItemCount(): Int =userList.size

    class UserViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val binding : ItemContainerBinding = ItemContainerBinding.bind(itemView)
    }


}