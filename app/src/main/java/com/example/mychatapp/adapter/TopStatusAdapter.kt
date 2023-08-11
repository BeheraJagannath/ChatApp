package com.example.mychatapp.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.activity.MainActivity
import com.example.mychatapp.databinding.ItemStatusBinding
import com.example.mychatapp.Utils.Util
import com.example.mychatapp.model.UserStatus
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*


class TopStatusAdapter(var context:Context, var userStatus: ArrayList<UserStatus>): RecyclerView.Adapter<TopStatusAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_status,parent,false)
        return UserViewHolder(v)
    }
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userStatus = userStatus[position]
          try {
              val status = userStatus.status!!.get(userStatus.status!!.size-1)
              Glide.with(context)
                  .load(status.imageUrl)
                  .placeholder(R.drawable.person_crop_circle)
                  .into(holder.binding.statusImage)
          }catch (e:java.lang.Exception){

          }

        holder.binding.statusTitle.text = userStatus.name
        holder.binding.statusTime.text = Util.date2DayTime(Date(userStatus.lastUpdated!!))
        holder.binding.circularStatusView.setPortionsCount(userStatus.status!!.size)




        holder.binding.imageLayout.setOnClickListener{
            val myStories: ArrayList<MyStory> = ArrayList<MyStory>()
            for (status in userStatus.status!!) {
                myStories.add(MyStory(status.imageUrl))
            }

            StoryView.Builder((context as MainActivity).supportFragmentManager)
                .setStoriesList(myStories)
                .setStoryDuration(5000)
                .setTitleText(userStatus.name)
                .setSubtitleText("")
                .setTitleLogoUrl(userStatus.profileImage)
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

    override fun getItemCount(): Int =userStatus.size

    class UserViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val binding : ItemStatusBinding = ItemStatusBinding.bind(itemView)
    }
}