package com.example.mychatapp.model

class User {
    var uid: String? =null
    var name: String? =null
    var phoneNumber: String? =null
    var profileImage: String? =null
    var about: String? =null
    var token: String? =null
    constructor(){}
    constructor(uid:String, name: String?, phoneNumber: String?, profileImage: String?,about: String?){
        this.uid = uid
        this.name = name
        this.phoneNumber = phoneNumber
        this.profileImage = profileImage
        this.about = about
    }
}