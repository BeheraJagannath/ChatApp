package com.example.mychatapp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import com.example.mychatapp.R
import com.example.mychatapp.Utils.Util
import com.example.mychatapp.databinding.ActivityChatMediaBinding
import com.google.firebase.auth.FirebaseAuth
import java.sql.Date


class ChatMediaActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatMediaBinding

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMediaBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        window.setStatusBarColor(ContextCompat.getColor(this, R.color.n_black))


        val bundle = intent.getBundleExtra("bundle")
        val image = bundle!!.getString("image")
        val time = bundle.getString("time")
        val currentUser = bundle.getString("currentUser")
        val senderUid = bundle.getString("senderUid")


        val options = RequestOptions()
        Glide.with(this@ChatMediaActivity)
            .load(image)
            .apply(
                options.centerCrop()
                    .skipMemoryCache(true)
                    .priority(Priority.HIGH)
                    .format(DecodeFormat.PREFER_ARGB_8888)
            )
            .into(binding.expandedListImage)

        binding.chatMediaBack.setOnClickListener { this.finishAfterTransition() }
        binding.chatMediaTime.text = Util.date2DayTime(Date(time!!.toLong()))
        if (senderUid == FirebaseAuth.getInstance().uid){
            binding.chatMediaName.text = "You"
        }else{
            binding.chatMediaName.text = currentUser
        }


    }


}