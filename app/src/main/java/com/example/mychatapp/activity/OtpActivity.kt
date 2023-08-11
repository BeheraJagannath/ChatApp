package com.example.mychatapp.activity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mychatapp.R
import com.example.mychatapp.databinding.ActivityOtpBinding
import com.example.mychatapp.model.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


class OtpActivity : AppCompatActivity() {
    lateinit var binding: ActivityOtpBinding
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var verificationId:String
    lateinit var dialog: ProgressDialog
    lateinit var phoneNumber:String

    var selectedImage: Uri? = null
    lateinit var selectedImages:ByteArray
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var firebaseStorage: FirebaseStorage
    private val REQUEST_CODE_STORAGE_PERMISSION = 1
    private val REQUEST_CODE_CAMERA_PERMISSION = 2
    var capture:Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setProfile()

        firebaseAuth= FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        dialog=ProgressDialog(this)
        dialog.setMessage("Sending OTP...")
        dialog.setCancelable(false)
        dialog.show()

        phoneNumber = intent.getStringExtra("phoneNumber")!!
        binding.number.text = phoneNumber

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setActivity(this)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(object :PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    TODO("Not yet implemented")
                }

                override fun onVerificationFailed(p0: FirebaseException) {

                }
                override fun onCodeSent(verifyId: String, resendToken: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verifyId, resendToken)
                    dialog.dismiss()
                    verificationId= verifyId
                    val  imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
                    binding.otpView.requestFocus()
                }

            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        binding.otpView.setOtpCompletionListener{otp->
            val credential = PhoneAuthProvider.getCredential(verificationId,otp)
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener{task->
                if (task.isSuccessful){
                    binding.openOtpView.visibility=View.GONE
                    binding.openProfile.visibility=View.VISIBLE
//                    val intent= Intent(this, ProfileActivity::class.java)
//                    startActivity(intent)
//                    finishAffinity()

                }else{
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    private fun setProfile() {
        binding.addProfile.setOnClickListener{
            selectImage()
        }

        binding.saveProfile.setOnClickListener{
           val dialog=ProgressDialog(this)
            dialog.setMessage("Updating Profile...")
            dialog.setCancelable(false)
            dialog.show()
            val pName:String = binding.profileName.text.toString()
            if (pName.isEmpty()){
                Toast.makeText(this, "Please type a Username", Toast.LENGTH_SHORT).show()
            }

            if (capture == true){
                if (selectedImages !=null){
                    var refrence = firebaseStorage.reference.child("Profile").child(firebaseAuth.uid!!)
                    refrence.putBytes(selectedImages).addOnCompleteListener{task->
                        if (task.isSuccessful){
                            refrence.downloadUrl.addOnSuccessListener { uri->
                                val imageUrl = uri.toString()

                                val uid =firebaseAuth.uid
                                val phone =firebaseAuth.currentUser!!.phoneNumber
                                val name = binding.profileName.text.toString()
                                val about = "Hard work"
                                val user = User(uid!!,name,phone,imageUrl,about)

                                firebaseDatabase.reference
                                    .child("users")
                                    .child(uid)
                                    .setValue(user)
                                    .addOnSuccessListener{
                                        dialog.dismiss()
                                        val intent= Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }

                            }
                        }

                    }

                }

            } else if (selectedImage != null){

                var refrence = firebaseStorage.reference.child("Profile").child(firebaseAuth.uid!!)
                refrence.putFile(selectedImage!!).addOnCompleteListener{task->
                    if (task.isSuccessful){
                        refrence.downloadUrl.addOnSuccessListener { uri->
                            val imageUrl = uri.toString()

                            val uid =firebaseAuth.uid
                            val phone =firebaseAuth.currentUser!!.phoneNumber
                            val name = binding.profileName.text.toString()
                            val about = "Hard work"
                            val user = User(uid!!,name,phone,imageUrl,about)

                            firebaseDatabase.reference
                                .child("users")
                                .child(uid)
                                .setValue(user)
                                .addOnSuccessListener{
                                    dialog.dismiss()
                                    val intent= Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }

                        }
                    }

                }
            }else{

                val uid =firebaseAuth.uid
                val phone =firebaseAuth.currentUser!!.phoneNumber
                val name = binding.profileName.text.toString()
                val about = "Hard work"
                val user = User(uid!!,name,phone,"No Image",about)

                firebaseDatabase.reference
                    .child("users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener{
                        dialog.dismiss()
                        val intent= Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

            }

        }
    }
    private fun selectImage() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomsheetView: View = LayoutInflater.from(this).inflate(R.layout.layout_bottom, null)
        bottomSheetDialog.setContentView(bottomsheetView)
        bottomSheetDialog.show()


        val camera = bottomSheetDialog.findViewById<ImageView>(R.id.camera)
        val gallery = bottomSheetDialog.findViewById<ImageView>(R.id.gallery)
        camera!!.setOnClickListener{
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf<String> (Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
                bottomSheetDialog.dismiss()

            }else {
                pickCamera()
                bottomSheetDialog.dismiss()
            }
        }
        gallery!!.setOnClickListener {
            if  (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
                bottomSheetDialog.dismiss()
            }else {
                pickGallery()
                bottomSheetDialog.dismiss()

            }

        }

    }

    private fun pickGallery() {
        var intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,REQUEST_CODE_STORAGE_PERMISSION)
    }
    private fun pickCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CODE_CAMERA_PERMISSION)
        capture = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSION -> {
                if (grantResults.size > 0) {
                    val camera_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (camera_accepted) {
                        pickCamera()
                    } else {
                        Toast.makeText(this, "Please Enable Camera Permissions", Toast.LENGTH_LONG).show()
                    }
                }
            }
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.size > 0) {
                    val writeStorageaccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageaccepted) {
                        pickGallery()
                    } else {
                        Toast.makeText(this, "Please Enable Storage Permissions", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION && resultCode == Activity.RESULT_OK) {
            val thumbnail = data!!.extras!!["data"] as Bitmap?
            val byteArrayOutputStream = ByteArrayOutputStream()
            thumbnail!!.compress(Bitmap.CompressFormat.JPEG,90,byteArrayOutputStream)
            selectedImages= byteArrayOutputStream.toByteArray()
            binding.profilePicture.setImageBitmap(thumbnail)

        }else if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && resultCode == Activity.RESULT_OK){
            if (data!= null){
                if (data.data!=null){
                    binding.profilePicture.setImageURI(data.data)
                    selectedImage= data.data!!
                }

            }

        }
    }
}