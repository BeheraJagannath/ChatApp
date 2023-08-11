package com.example.mychatapp.interfac

import android.view.View
import com.example.mychatapp.model.User
import de.hdodenhof.circleimageview.CircleImageView

interface UserClick {

    fun onUserClick(users: User)
    fun zoomImageFromThumb(imageurl: User, binding: View )

}