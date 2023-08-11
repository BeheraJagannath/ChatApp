package com.example.mychatapp.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.databinding.ActivitySettingsBinding
import com.example.mychatapp.model.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream


class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    lateinit var user: User
    lateinit var databaseReference: DatabaseReference
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var firebaseStorage: FirebaseStorage
    lateinit var firbaseauth : FirebaseAuth
    var dialog: ProgressDialog? = null

    lateinit var selectedImage: Uri
    lateinit var selectedImages: ByteArray

    private val IMAGEPICK_GALLERY_REQUEST = 300
    private val IMAGE_PICKCAMERA_REQUEST = 400

    private var mCurrentAnimator: Animator? = null
    var mShortAnimationDuration : Int = 0
    lateinit var startBounds :Rect
    var startScale: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this)
        dialog!!.setMessage("Uploading Profile...")
        dialog!!.setCancelable(false)
        mShortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        startBounds = Rect()

        firebaseDatabase = FirebaseDatabase.getInstance()
        firbaseauth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        databaseReference = firebaseDatabase.getReference("users")

        binding.settingBack.setOnClickListener(View.OnClickListener { finish() })
        binding.addProfile.setOnClickListener{

            val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
            val bottomsheetView: View = LayoutInflater.from(this).inflate(R.layout.layout_bottom, null)
            bottomSheetDialog.setContentView(bottomsheetView)
            bottomSheetDialog.show()


            val camera = bottomSheetDialog.findViewById<ImageView>(R.id.camera)
            val gallery = bottomSheetDialog.findViewById<ImageView>(R.id.gallery)
            camera!!.setOnClickListener{
                if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                    ActivityCompat.requestPermissions(this, arrayOf<String> (
                        Manifest.permission.CAMERA), IMAGE_PICKCAMERA_REQUEST)
                    bottomSheetDialog.dismiss()

                }else {
                    takeImage()
                    bottomSheetDialog.dismiss()
                }

            }
            gallery!!.setOnClickListener {
                if  (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE), IMAGEPICK_GALLERY_REQUEST)
                    bottomSheetDialog.dismiss()
                }else {
                    selectImage()
                    bottomSheetDialog.dismiss()
                }
            }
        }

        databaseReference.child(firbaseauth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                    binding.userName.text = user.name
                    binding.userphoneNo.text = user.phoneNumber
                    binding.userAbout.text = user.about

                    Glide.with(this@SettingsActivity)
                        .load(user.profileImage)
                        .placeholder(R.drawable.person_crop_circle)
                        .into(binding.profilePicture)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SettingsActivity, "Fail to get data.", Toast.LENGTH_SHORT).show()
                }
            })

        binding.linear.setOnClickListener{
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.editname_dialog)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(true)
            dialog.show()

            val edit_userName: EditText
            val editname_cancel: TextView
            val editname_save: TextView
            edit_userName = dialog.findViewById(R.id.edit_userName)
            editname_cancel = dialog.findViewById(R.id.editname_cancel)
            editname_save = dialog.findViewById(R.id.editname_save)
            edit_userName.setText(user.name)
            editname_save.setOnClickListener{
                val image = user.profileImage
                val uid =user.uid
                val phone =user.phoneNumber
                val name = edit_userName.text.toString()
                val about =user.about
                val user =User(uid!!,name,phone,image,about)

                firebaseDatabase.reference
                    .child("users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener{
                        dialog.dismiss()
                    }
            }
            editname_cancel.setOnClickListener{
                dialog.dismiss()
            }

        }

        binding.linear2.setOnClickListener{
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.editname_dialog)
            dialog.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(true)
            dialog.show()

            val edit_userName: EditText
            val editname_cancel: TextView
            val editname_save: TextView
            edit_userName = dialog.findViewById(R.id.edit_userName)
            editname_cancel = dialog.findViewById(R.id.editname_cancel)
            editname_save = dialog.findViewById(R.id.editname_save)

            edit_userName.setText(user.about)
            editname_save.setOnClickListener{

                val image = user.profileImage
                val uid =user.uid
                val name = user.name
                val phone =user.phoneNumber
                val about = edit_userName.text.toString()
                val user =User(uid!!,name,phone,image,about)

                firebaseDatabase.reference
                    .child("users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener{
                        dialog.dismiss()
                    }
            }
            editname_cancel.setOnClickListener{
                dialog.dismiss()
            }

        }

        binding.profilePicture.setOnClickListener(View.OnClickListener {
            val image_url = user.profileImage
            zoomImageFromThumb(  image_url)
        })

    }
    fun zoomImageFromThumb(  imageUrl: String?) {
        if (mCurrentAnimator != null) {
            mCurrentAnimator!!.cancel()
        }
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.person_crop_circle)
            .into(binding.expandedListImage)

        val finalBounds = Rect()
        val globalOffset = Point()
        binding.profilePicture.getGlobalVisibleRect(startBounds)
        binding.mainRltv.getGlobalVisibleRect(finalBounds, globalOffset)
        startBounds.offset(-globalOffset.x, -globalOffset.y)
        finalBounds.offset(-globalOffset.x, -globalOffset.y)
        if (finalBounds.width().toFloat() / finalBounds.height() > startBounds.width()
                .toFloat() / startBounds.height()
        ) {
            startScale = startBounds.height().toFloat() / finalBounds.height()
            val startWidth = startScale * finalBounds.width()
            val deltaWidth = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            startScale = startBounds.width().toFloat() / finalBounds.width()
            val startHeight = startScale * finalBounds.height()
            val deltaHeight = (startHeight - startBounds.height()) / 2
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }
        binding.profilePicture.alpha = 0f
        binding.expandedListRltv.visibility = View.VISIBLE
        binding.expandedListImage.visibility = View.VISIBLE
        binding.expandedListRltv.pivotX = 0f
        binding.expandedListRltv.pivotY = 0f
        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(binding.expandedListRltv, View.X, startBounds.left.toFloat(), finalBounds.left.toFloat()))
            .with(ObjectAnimator.ofFloat(binding.expandedListRltv, View.Y, startBounds.top.toFloat(), finalBounds.top.toFloat()))
            .with(ObjectAnimator.ofFloat( binding.expandedListRltv, View.SCALE_X, startScale, 1f)).with(ObjectAnimator.ofFloat(binding.expandedListRltv, View.SCALE_Y, startScale, 1f))
        set.duration = mShortAnimationDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mCurrentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                mCurrentAnimator = null
            }
        })
        set.start()
        mCurrentAnimator = set
        binding.expandedListRltv.setOnClickListener {

        }
        binding.expendImageBack.setOnClickListener {
            expendImageBack(startBounds,startScale)
        }
    }

    private fun expendImageBack( startBounds: Rect, startScale: Float) {
        if (mCurrentAnimator != null) {
            mCurrentAnimator!!.cancel()
        }
        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(binding.expandedListRltv, View.X, startBounds.left.toFloat()))
            .with(ObjectAnimator.ofFloat(binding.expandedListRltv, View.Y, startBounds.top.toFloat()))
            .with(ObjectAnimator.ofFloat(binding.expandedListRltv, View.SCALE_X, startScale))
            .with(ObjectAnimator.ofFloat(binding.expandedListRltv, View.SCALE_Y, startScale))
        set.duration = mShortAnimationDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.profilePicture.alpha = 1f
                binding.expandedListImage.visibility = View.GONE
                binding.expandedListRltv.visibility = View.GONE
                mCurrentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                binding.profilePicture.alpha = 1f
                binding.expandedListImage.visibility = View.GONE
                binding.expandedListRltv.visibility = View.GONE
                mCurrentAnimator = null
            }
        })
        set.start()
        mCurrentAnimator = set

    }

    private fun selectImage() {
        var intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,IMAGEPICK_GALLERY_REQUEST)
    }
    private fun takeImage(){
      val camerIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         startActivityForResult(camerIntent, IMAGE_PICKCAMERA_REQUEST)

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            IMAGE_PICKCAMERA_REQUEST -> {
                if (grantResults.size > 0) {
                    val camera_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (camera_accepted) {
                        takeImage()
                    } else {
                        Toast.makeText(this, "Please Enable Camera Permissions", Toast.LENGTH_LONG).show()
                    }
                }
            }
            IMAGEPICK_GALLERY_REQUEST -> {
                if (grantResults.size > 0) {
                    val writeStorageaccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageaccepted) {
                        selectImage()
                    } else {
                        Toast.makeText(this, "Please Enable Storage Permissions", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGEPICK_GALLERY_REQUEST) {
                selectedImage = data!!.data!!
                uploadProfileCoverPhoto(selectedImage)
            }
            if (requestCode == IMAGE_PICKCAMERA_REQUEST) {

                val thumbnail = data!!.extras!!["data"] as Bitmap?
                val byteArrayOutputStream = ByteArrayOutputStream()
                thumbnail!!.compress(Bitmap.CompressFormat.JPEG,90,byteArrayOutputStream)
                selectedImages= byteArrayOutputStream.toByteArray()
                binding.profilePicture.setImageBitmap(thumbnail)
                UpdateProfile()

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun uploadProfileCoverPhoto(selectedImage: Uri) {
        dialog!!.show()
        if (selectedImage !=null){
            var refrence = firebaseStorage.reference.child("Profile").child(firbaseauth.uid!!)
            refrence.putFile(selectedImage).addOnCompleteListener{task->
                if (task.isSuccessful){
                    refrence.downloadUrl.addOnSuccessListener { uri->

                        val imageUrl = uri.toString()
                        val uid =user.uid
                        val phone =user.phoneNumber
                        val name = user.name
                        val about = user.about
                        val user =User(uid!!,name,phone,imageUrl,about)

                        firebaseDatabase.reference
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener{
                                dialog!!.dismiss()
                            }

                    }
                }

            }


        }
    }

    private fun UpdateProfile() {
        dialog!!.show()
        if (selectedImages !=null){
            var refrence = firebaseStorage.reference.child("Profile").child(firbaseauth.uid!!)
            refrence.putBytes(selectedImages).addOnCompleteListener{task->
                if (task.isSuccessful){
                    refrence.downloadUrl.addOnSuccessListener { uri->

                        val imageUrl = uri.toString()
                        val uid =user.uid
                        val phone =user.phoneNumber
                        val name = user.name
                        val about = user.about
                        val user =User(uid!!,name,phone,imageUrl,about)

                        firebaseDatabase.reference
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener{
                                dialog!!.dismiss()
                            }
                    }
                }

            }

        }

    }

    override fun onBackPressed() {
        if (binding.expandedListRltv.isVisible){
            expendImageBack(startBounds , startScale)
        }else{
            super.onBackPressed()
        }
    }

}