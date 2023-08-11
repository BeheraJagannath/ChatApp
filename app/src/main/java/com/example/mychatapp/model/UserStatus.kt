package com.example.mychatapp.model

class UserStatus {
    var uid: String? =null
    var name: String? =null
    var profileImage: String? =null
    var lastUpdated: Long? = null
    var status:ArrayList<Status>? =null
    constructor(){}

    constructor( uid: String? ,name: String?,profileImage: String?,lastUpdated: Long?,status:ArrayList<Status>?){
        this.uid = uid
        this.name = name
        this.profileImage = profileImage
        this.lastUpdated = lastUpdated
        this.status = status

    }
}