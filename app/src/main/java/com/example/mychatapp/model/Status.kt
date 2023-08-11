package com.example.mychatapp.model

class Status {
    var imageUrl: String? =null
    var timeStamp: Long? = null
    constructor(){}
    constructor(imageUrl: String,timeStamp: Long){
        this.imageUrl = imageUrl
        this.timeStamp = timeStamp

    }
}