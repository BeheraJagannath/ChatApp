package com.example.mychatapp.activity

import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.adapter.MessageAdapter
import com.example.mychatapp.databinding.ActivityChatBinding
import com.example.mychatapp.interfac.DeleteClick
import com.example.mychatapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.annotations.NotNull
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.internal.Intrinsics


class ChatActivity : AppCompatActivity(),DeleteClick {
        lateinit var binding: ActivityChatBinding
        var messageAdapter: MessageAdapter? = null
        lateinit var messages: ArrayList<Message>
        var senderRoom: String? = null
        var receiverRoom: String? = null
        var database: FirebaseDatabase? = null
        var storage: FirebaseStorage? = null
        var auth: FirebaseAuth? = null
        var dialog: ProgressDialog? = null
        var senderUid: String? = null
        var receiverUid: String? = null
        var phoneNumber: String? = null
        var currentUser: String? = null
        var name: String? = null
        var profile: String? = null
        private val PHONE_STORAGE_PERMISSION = 1
        private val IMAGEPICK_GALLERY_REQUEST = 300
        private val IMAGE_PICKCAMERA_REQUEST = 2
        private val RECORD_AUDIO_REQUEST = 3

        private var mRecorder: MediaRecorder? = null
        private var mFileName: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.getRoot())
            dialog = ProgressDialog(this)
            dialog!!.setMessage("Uploading Image....")
            dialog!!.setCancelable(false)


            database = FirebaseDatabase.getInstance()
            storage = FirebaseStorage.getInstance()
            auth = FirebaseAuth.getInstance()
            messages = ArrayList()
            name = intent.getStringExtra("name")
            receiverUid = intent.getStringExtra("uid")
            profile = intent.getStringExtra("image")
            val token = intent.getStringExtra("token")
            phoneNumber = intent.getStringExtra("phonenumber")
            currentUser = intent.getStringExtra("currentUser")
            senderUid = FirebaseAuth.getInstance().uid

            setSupportActionBar(binding.chatToolbar)

            /* get user online offline status */
            database!!.reference.child("presence")
                .child(receiverUid!!).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val status = snapshot.getValue(String::class.java)
                            if (status != null && !status.isEmpty()) {
                                if (status == "Offline") {
                                    database!!.reference.child("presence").child("date")
                                        .child(receiverUid!!)
                                        .addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (snapshot.exists()) {
                                                    val date = snapshot.getValue(String::class.java)
                                                    if (date != null) {
                                                        binding.textViewAvailability.setText(getSmsTodayYestFromMilli(convertDateToLong(date)))
                                                        Log.e("date", date)

                                                    }
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                } else {
                                    binding.textViewAvailability.setText(status)
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            binding.title.setText(name)
            Glide.with(this).load(profile).placeholder(R.drawable.person_crop_circle)
                .into(binding.chatProfile)
            binding.chatBack.setOnClickListener(View.OnClickListener { onBackPressed() })
            binding.message.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                }
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    database!!.reference.child("presence").child(senderUid!!).setValue("typing...")
                    if (editable != null && editable.length >= 1) {
                        binding.attachment.visibility = View.VISIBLE
                        binding.OpenCamera.visibility = View.GONE
                        binding.send.setVisibility(View.VISIBLE)
                        binding.chatVoice.visibility = View.GONE
                        database!!.reference.child("presence").child(senderUid!!).setValue("typing...")
                    } else {
                        binding.attachment.visibility = View.VISIBLE
                        binding.OpenCamera.visibility = View.VISIBLE
                        binding.send.setVisibility(View.GONE)
                        binding.chatVoice.visibility = View.VISIBLE
                        database!!.reference.child("presence").child(senderUid!!).setValue("Online")
                    }
                }
            })
            senderRoom = senderUid + receiverUid
            receiverRoom = receiverUid + senderUid
            setAdapter()

            database!!.reference.child("chats")
                .child(senderRoom!!)
                .child("messages")
                .addValueEventListener(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        messages.clear()
                        for (snapshot1 in snapshot.children) {
                            val message = snapshot1.getValue(Message::class.java)
                            message!!.messageId = snapshot1.key
                            messages.add(message)
                        }
                        messageAdapter!!.notifyDataSetChanged()
                        if (messages.size != 0) {
                            messageAdapter!!.notifyItemRangeInserted(messages.size, messages.size)
                            binding.chatrecycler.smoothScrollToPosition(messages.size - 1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            binding.send.setOnClickListener(View.OnClickListener {
                val messageText: String = binding.message.getText().toString()
                if (messageText.isEmpty()) {
                    Toast.makeText(this, "Please type a Massage", Toast.LENGTH_SHORT).show()
                }
                val date = Date()
                val message = Message(messageText, senderUid!!, date.time)
                binding.message.setText("")
                val randomKey = database!!.reference.push().key
                val lastMessageObj = HashMap<String, Any?>()
                lastMessageObj["lastMsg"] = message.message
                lastMessageObj["lastMsgTime"] = date.time
                database!!.reference.child("chats").child(senderRoom!!)
                    .updateChildren(lastMessageObj)
                database!!.reference.child("chats").child(receiverRoom!!)
                    .updateChildren(lastMessageObj)
                database!!.reference.child("chats")
                    .child(senderRoom!!)
                    .child("messages")
                    .child(randomKey!!)
                    .setValue(message)
                    .addOnSuccessListener {

                        database!!.reference.child("chats")
                            .child(receiverRoom!!)
                            .child("messages")
                            .child(randomKey)
                            .setValue(message).addOnSuccessListener {

                                sendNotification(currentUser, message.message, token)
                            }
                        val lastMessageObj = HashMap<String, Any?>()
                        lastMessageObj["lastMsg"] = message.message
                        lastMessageObj["lastMsgTime"] = date.time
                        database!!.reference.child("chats").child(senderRoom!!)
                            .updateChildren(lastMessageObj)
                        database!!.reference.child("chats").child(receiverRoom!!)
                            .updateChildren(lastMessageObj)
                    }


            })
            binding.OpenCamera.setOnClickListener{

                if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                    ActivityCompat.requestPermissions(this, arrayOf<String> (
                        Manifest.permission.CAMERA), IMAGE_PICKCAMERA_REQUEST)

                }else {
                    val intent = Intent(this,CameraActivity::class.java)
                    startActivity(intent)
//                    takeImage()
                }
            }
            binding.attachment.setOnClickListener(View.OnClickListener {
                if  (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE), IMAGEPICK_GALLERY_REQUEST)
                }else {
                    selectImage()
                }
            })

            binding.chatVoice.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN ->{
                            startRecording()
                        }
                        MotionEvent.ACTION_UP -> {

                            stopRecording()
                        }
                    }

                    return true
                }
            })
        }

    private fun setAdapter() {
        messageAdapter = MessageAdapter(this, messages, senderRoom!!, receiverRoom!!, profile!!, receiverUid!!,this)
        binding.chatrecycler.setLayoutManager(LinearLayoutManager(this))
        binding.chatrecycler.setAdapter(messageAdapter)
    }

    private fun startRecording() {
        if (CheckPermissions()) {
            mFileName = Environment.getExternalStorageDirectory().absolutePath
            mFileName += "/recored_audio.3gp"

        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder!!.setOutputFile(mFileName)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        try {
            mRecorder!!.prepare()
        } catch (e: IOException) {
            Log.e("LOG_TAG", "prepare() failed" + e.message)
        }
        mRecorder!!.start()
            Toast.makeText(this@ChatActivity, "Recording start...", Toast.LENGTH_SHORT).show()
        }else{
            RequestPermissions()
        }
    }

    private fun stopRecording() {
        if (CheckPermissions()) {
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
            Toast.makeText(this@ChatActivity, "Recording stop...", Toast.LENGTH_SHORT).show()
            Log.e("LOG_TAG", mFileName.toString())
            val uri = Uri.fromFile(File(mFileName))
            uploadRecoderToFirebase(uri)

        }else{
            RequestPermissions()
        }

    }

    fun CheckPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun RequestPermissions() {
        ActivityCompat.requestPermissions(
            this@ChatActivity,
            arrayOf<String>(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE), RECORD_AUDIO_REQUEST)
    }
    fun selectImage() {
       val intent = Intent()
       intent.type = "image/*"
       intent.action = Intent.ACTION_GET_CONTENT
       startActivityForResult(intent, IMAGEPICK_GALLERY_REQUEST)
   }
    fun takeImage(){
        val chooserIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(chooserIntent, IMAGE_PICKCAMERA_REQUEST)
    }

    fun sendNotification(name: String?, message: String?, token: String?) {
        try {
            val queue = Volley.newRequestQueue(this)
            val url = "https://fcm.googleapis.com/fcm/send"
            val data = JSONObject()
            data.put("title", name)
            data.put("body", message)
            val notificationData = JSONObject()
            notificationData.put("notification", data)
            notificationData.put("to", token)
            val request: JsonObjectRequest =
                object : JsonObjectRequest(url, notificationData, Response.Listener<JSONObject?> {

                },
                    Response.ErrorListener { error ->
                        Toast.makeText(this@ChatActivity, error.localizedMessage, Toast.LENGTH_SHORT).show() }) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val map = HashMap<String, String>()
                        val key =
                            "Key=AAAAreGRxNw:APA91bG3i8pEMzTGf2qgwxKkus1PWyZ4ea5AG3UPQbIuIYBLu6Kj1r0Ofm7aQ7ZG-zexF-yYrArrjnSO5v9sLd6EkAnGek-_R5NELMYmHr6V3MU9VyjCdWUguGwTv19FwWrt41ajTZZm"
                        map["Content-Type"] = "application/json"
                        map["Authorization"] = key
                        return map
                    }
                }
            queue.add(request)
        } catch (ex: Exception) {
        }
    }

    private fun isMessageSeen(receiverUid: String?) {
            /*DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot1: snapshot.getChildren()){
                    Message message = snapshot1.getValue(Message.class);
                    AvngsZu9SSM2DQoVK8XsFHi3t043
                    NmGuwORmp8eKc3F1cGaoFAnPQOp2
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });*/
        }

    @SuppressLint("SimpleDateFormat")
    fun convertDateToLong(date: String?): Long {
        Intrinsics.checkNotNullParameter(date, "date")
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        format.timeZone = TimeZone.getDefault()
        var dateToTime: Date? = null
        dateToTime = try {
            format.parse(date)
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }
        return dateToTime?.time ?: 0L
    }

    @NotNull
    fun getSmsTodayYestFromMilli(msgTimeMillis: Long?): String {
        val messageTime = Calendar.getInstance()
        messageTime.timeInMillis = msgTimeMillis!!
        val now = Calendar.getInstance()
        val startTime = "h:mm aa"
        val startDate = "dd MMM h:mm aa"
        val startDay = "EEEE h:mm aa"
        return if (now[Calendar.DATE] == messageTime[Calendar.DATE] && now[Calendar.MONTH] == messageTime[Calendar.MONTH] && now[Calendar.YEAR] == messageTime[Calendar.YEAR]) StringBuilder()
            .append("last seen today at ")
            .append(DateFormat.format(startTime as CharSequence, messageTime))
            .toString() else if (now[Calendar.DATE] - messageTime[Calendar.DATE] == 1 && now[Calendar.MONTH] == messageTime[Calendar.MONTH] && now[Calendar.YEAR] == messageTime[Calendar.YEAR]) StringBuilder()
            .append("last seen yesterday at ")
            .append(DateFormat.format(startTime as CharSequence, messageTime))
            .toString() else if (now[Calendar.DATE] - messageTime[Calendar.DATE] >= 2 && now[Calendar.DATE] - messageTime[Calendar.DATE] <= 7 && now[Calendar.MONTH] == messageTime[Calendar.MONTH] && now[Calendar.YEAR] == messageTime[Calendar.YEAR]) StringBuilder()
            .append("last seen ")
            .append(DateFormat.format(startDay as CharSequence, messageTime))
            .toString() else StringBuilder()
            .append("last seen ")
            .append(DateFormat.format(startDate as CharSequence, messageTime)).toString()
    }


    private fun SaveImage(bm: Bitmap?, context: ChatActivity): Uri {
        val  imagesFolder = File(context.cacheDir,"images")
        var uri : Uri? = null
        try {

            imagesFolder.mkdir()
            val  file = File(imagesFolder,"captured_image_jpg")
            val fileOutputStream = FileOutputStream(file)
            bm!!.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            uri = FileProvider.getUriForFile(applicationContext,"com.example.mychatapp"+".provider",file)


        }catch (e:FileNotFoundException){
            e.printStackTrace()
        }

        return uri!!

    }

    private fun uploadRecoderToFirebase(uri: Uri?) {
        val calendar = Calendar.getInstance()
        val reference = storage!!.reference.child("chats").child(calendar.timeInMillis.toString() + "")
        reference.putFile(uri!!).addOnCompleteListener { task ->
            dialog!!.dismiss()
            if (task.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val audioFile = uri.toString()
                    val messageText: String = binding.message.getText().toString()
                    val date = Date()
                    val message = Message(messageText, senderUid!!, date.time)

                    message.message = "audio"
                    message.audioUrl = audioFile
                    binding.message.setText("")
                    val randomKey = database!!.reference.push().key
                    val lastMessageObj =
                        HashMap<String, Any?>()
                    lastMessageObj["lastMsg"] = message.message
                    lastMessageObj["lastMsgTime"] = date.time
                    database!!.reference.child("chats").child(senderRoom!!)
                        .updateChildren(lastMessageObj)
                    database!!.reference.child("chats").child(receiverRoom!!)
                        .updateChildren(lastMessageObj)
                    database!!.reference.child("chats")
                        .child(senderRoom!!)
                        .child("messages")
                        .child(randomKey!!)
                        .setValue(message)
                        .addOnSuccessListener {

                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener {

                                }
                            val lastMessageObj =
                                HashMap<String, Any?>()
                            lastMessageObj["lastMsg"] = message.message
                            lastMessageObj["lastMsgTime"] = date.time
                            database!!.reference.child("chats")
                                .child(senderRoom!!)
                                .updateChildren(lastMessageObj)
                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .updateChildren(lastMessageObj)
                        }
                }
            }
        }

    }

    private fun uploadImageToFirebase(selectedImage: Uri) {
        val calendar = Calendar.getInstance()
        val reference = storage!!.reference.child("chats").child(calendar.timeInMillis.toString() + "")

        reference.putFile(selectedImage).addOnCompleteListener { task ->
            dialog!!.dismiss()
            if (task.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val imageFile = uri.toString()
                    val messageText: String = binding.message.getText().toString()
                    val date = Date()
                    val message = Message(messageText, senderUid!!, date.time)
                    message.message = "photo"
                    message.imageUrl = imageFile

                    binding.message.setText("")
                    val randomKey = database!!.reference.push().key
                    val lastMessageObj =
                        HashMap<String, Any?>()
                    lastMessageObj["lastMsg"] = message.message
                    lastMessageObj["lastMsgTime"] = date.time
                    database!!.reference.child("chats").child(senderRoom!!)
                        .updateChildren(lastMessageObj)
                    database!!.reference.child("chats").child(receiverRoom!!)
                        .updateChildren(lastMessageObj)
                    database!!.reference.child("chats")
                        .child(senderRoom!!)
                        .child("messages")
                        .child(randomKey!!)
                        .setValue(message)
                        .addOnSuccessListener {

                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener {

                                }
                            val lastMessageObj =
                                HashMap<String, Any?>()
                            lastMessageObj["lastMsg"] = message.message
                            lastMessageObj["lastMsgTime"] = date.time
                            database!!.reference.child("chats")
                                .child(senderRoom!!)
                                .updateChildren(lastMessageObj)
                            database!!.reference.child("chats")
                                .child(receiverRoom!!)
                                .updateChildren(lastMessageObj)
                        }
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater

        inflater.inflate(R.menu.chat_menu, menu)
        return super.onCreateOptionsMenu(menu)

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
             R.id.phone -> {
                 if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                     ActivityCompat.requestPermissions(this@ChatActivity,
                         arrayOf<String>(Manifest.permission.CALL_PHONE), PHONE_STORAGE_PERMISSION)
                 } else {
                     Calling()
                 }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    fun Calling(){
        val phone_intent = Intent(Intent.ACTION_CALL)
        phone_intent.data = Uri.parse("tel:" +phoneNumber )
        Log.d("df","tel:" + phoneNumber)
        startActivity(phone_intent)

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
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            selectImage()
                        }
                    } else {
                        Toast.makeText(this, "Please Enable Storage Permissions", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            RECORD_AUDIO_REQUEST -> {
                if (grantResults.size > 0) {
                    val permissionToRecord = grantResults[0] === PackageManager.PERMISSION_GRANTED
                    val permissionToStore = grantResults[1] === PackageManager.PERMISSION_GRANTED
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGEPICK_GALLERY_REQUEST) {
            if (data != null) {
                if (data.data != null) {
                    dialog!!.show()
                    val selectedImage = data.data!!
                    Log.d("selectedImage",selectedImage.toString())

                    uploadImageToFirebase(selectedImage)
                }
            }
        } else if (requestCode == IMAGE_PICKCAMERA_REQUEST ) {
            if (data!=null){
                if (data.data!=null){
                    val imageUri :Uri
                    val photo: Bitmap? = data.extras!!["data"] as Bitmap?
                    val result = WeakReference<Bitmap>(Bitmap.createScaledBitmap(photo!!,photo.height , photo.width,false).copy(
                        Bitmap.Config.RGB_565,true))
                    val bm = result.get()
                    imageUri = SaveImage(bm,this@ChatActivity)

                    uploadImageToFirebase(imageUri)
                    dialog!!.show()

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        database!!.reference.child("presence").child(senderUid!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database!!.reference.child("presence").child(senderUid!!).setValue("Offline")
    }

    override fun onSenderClick(position: Int, message: Message) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_layout)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        val delete_forme = dialog.findViewById(R.id.sender_delete_forme) as TextView
        val delete_everyone = dialog.findViewById(R.id.sender_delete_everyone) as TextView
        val cancel = dialog.findViewById(R.id.sender_cancel) as TextView
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        delete_forme.setOnClickListener {
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(senderRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(null)
            dialog.dismiss()
        }
        delete_everyone.setOnClickListener{
            message.message="This message was deleted"

            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(senderRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(message)
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(receiverRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(message)
            dialog.dismiss()

        }

        dialog.show()

    }

    override fun onReceiverClick(position: Int, message: Message) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.receiver_deletelayout)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        val delete_forme = dialog.findViewById(R.id.receiver_delete_forme) as TextView
        val cancel = dialog.findViewById(R.id.receiver_cancel) as TextView
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        delete_forme.setOnClickListener {
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(senderRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(null)
            dialog.dismiss()
        }
        /*delete_everyone.setOnClickListener{
            message.message="This message was deleted"

            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(senderRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(message)
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(receiverRoom!!)
                .child("messages")
                .child(message.messageId!!).setValue(message)
            dialog.dismiss()

        }*/

        dialog.show()

    }


    override fun animateIntent(position: Message, view: View) {
        val intent = Intent(this, ChatMediaActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view,
            "imageMain")

        val bundle = Bundle()
        bundle.putString("image", position.imageUrl)
        bundle.putString("time", position.timeStamp.toString())
        bundle.putString("currentUser", currentUser)
        bundle.putString("senderUid", senderUid)
        intent.putExtra("bundle", bundle)
        startActivity( intent, options.toBundle())
    }

    override fun animateIntentReceiver(position: Message, view: View) {

        val intent = Intent(this, ChatMediaActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view,
            "imageMain")

        val bundle = Bundle()
        bundle.putString("image", position.imageUrl)
        bundle.putString("time", position.timeStamp.toString())
        bundle.putString("currentUser", name)
        bundle.putString("senderUid", receiverUid)
        intent.putExtra("bundle", bundle)
        startActivity( intent, options.toBundle())

    }


}