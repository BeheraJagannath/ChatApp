package com.example.mychatapp.interfac

import android.view.View
import android.widget.ImageView
import com.example.mychatapp.model.Message

interface DeleteClick {

    fun onSenderClick(position: Int, message: Message)
    fun onReceiverClick(position: Int, message: Message)
    fun animateIntent(position: Message, view: View)
    fun animateIntentReceiver(position: Message, view: View)
}