package com.example.mychatapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mychatapp.R
import com.example.mychatapp.Utils.Util
import com.example.mychatapp.databinding.ReceiverContainerBinding
import com.example.mychatapp.databinding.SentContainerBinding
import com.example.mychatapp.interfac.DeleteClick
import com.example.mychatapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.sql.Date
import java.util.*


class MessageAdapter(context: Context, messages: ArrayList<Message>, senderRoom: String, receiverRoom: String, profile: String, receiverUid: String,deleteClick: DeleteClick ) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var context: Context
    var messages: ArrayList<Message>
    val ITEM_SENT = 1
    val ITEM_RECEIVE = 2
    var senderRoom: String
    var receiverRoom: String
    var profileImage: String
    var receiverUID: String
    var deleteClick:DeleteClick

    lateinit var mPlayer: MediaPlayer

    init {
        this.context = context
        this.messages = messages
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRoom
        profileImage = profile
        receiverUID = receiverUid
        this.deleteClick = deleteClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val view = LayoutInflater.from(context).inflate(R.layout.sent_container, parent, false)
            SentViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(context).inflate(R.layout.receiver_container, parent, false)
            ReceiveViewHolder(view)
        }
    }


    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) {
            ITEM_SENT
        }  else {
            ITEM_RECEIVE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder.javaClass == SentViewHolder::class.java) {
            val viewHolder = holder as SentViewHolder
            if (message.message == "photo") {
                viewHolder.binding.senderCardview.visibility = View.VISIBLE
                viewHolder.binding.relativelayout.visibility = View.GONE
                Glide.with(context).load(message.imageUrl).placeholder(R.color.hint_color)
                    .into(viewHolder.binding.sendImage)
                viewHolder.itemView.setOnClickListener {

                    deleteClick.animateIntent(message,viewHolder.binding.sendImage)

                }
            } else if (message.message == "audio"){
                viewHolder.binding.senderCardview.visibility = View.GONE
                viewHolder.binding.relativelayout.visibility = View.GONE
                viewHolder.binding.senderAudio .visibility = View.VISIBLE
                mPlayer = MediaPlayer()
                try {
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDataSource(message.audioUrl)
                    mPlayer.prepare()
                } catch (e: IOException) {
                    Log.e("TAG", "prepare() failed")
                }
                viewHolder.binding.audioPlay.setOnClickListener {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause()
                        viewHolder.binding.senderSeekbar.max =mPlayer.duration/1000
                        viewHolder.binding.audioPlay.setImageResource(R.drawable.ic_play)

                    } else {
                        mPlayer.start()
                         viewHolder.binding.senderSeekbar.max =mPlayer.duration/1000
                        viewHolder.binding.audioPlay.setImageResource(R.drawable.ic_pause)

                    }
                }
                mPlayer.setOnCompletionListener {
                    viewHolder.binding.audioPlay.setImageResource(R.drawable.ic_play)

                }

                viewHolder.binding.senderSeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean,
                    ) {
                        if (mPlayer != null && fromUser) {
                            mPlayer.seekTo(progress * 1000)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                val durationTotal: Int = mPlayer.duration / 1000
                viewHolder.binding.senderEndTime.setText(formattedTime(durationTotal))

            } else{
                viewHolder.binding.senderCardview.visibility = View.GONE
                viewHolder.binding.relativelayout.visibility = View.VISIBLE
            }
            viewHolder.binding.sendText.text = message.message

            holder.binding.senderTime.text  = Util.date2DayTime(Date(message.timeStamp))
            holder.binding.senderImageTime.text  = Util.date2DayTime(Date(message.timeStamp))

            viewHolder.itemView.setOnLongClickListener {
                deleteClick.onSenderClick(position, message)
                false
            }

            FirebaseDatabase.getInstance().reference.child("presences").child(receiverUID)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val status = snapshot.getValue(String::class.java)
                            if (status != null && !status.isEmpty()) {
                                if (status == "Seen") {
                                    viewHolder.binding.tick.setColorFilter(ContextCompat.getColor(context, R.color.purple_500))

                                } else {
                                    viewHolder.binding.tick.setColorFilter(ContextCompat.getColor(context, R.color.black))

                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

        } else {
            val viewHolder2 = holder as ReceiveViewHolder
            if (message.message == "photo") {
                viewHolder2.binding.receiverCardview.visibility = View.VISIBLE
                viewHolder2.binding.relativelayout.visibility = View.GONE
                Glide.with(context).load(message.imageUrl).placeholder(R.color.hint_color).into(viewHolder2.binding.receiveImage)

                viewHolder2.itemView.setOnClickListener {

                    deleteClick.animateIntentReceiver(message,viewHolder2.binding.receiveImage)

                }

            }else if (message.message == "audio"){
                viewHolder2.binding.receiverCardview.visibility = View.GONE
                viewHolder2.binding.relativelayout.visibility = View.GONE
                viewHolder2.binding.receiverrAudio .visibility = View.VISIBLE
                mPlayer = MediaPlayer()
                try {
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDataSource(message.audioUrl)
                    mPlayer.prepare()
                } catch (e: IOException) {
                    Log.e("TAG", "prepare() failed")
                }



                viewHolder2.binding.audioPlay.setOnClickListener {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause()
                        viewHolder2.binding.receiverSeekbar.max =mPlayer.duration/1000
                        viewHolder2.binding.audioPlay.setImageResource(R.drawable.ic_play)

                    } else {
                        mPlayer.start()
                        viewHolder2.binding.receiverSeekbar.max =mPlayer.duration/1000
                        viewHolder2.binding.audioPlay.setImageResource(R.drawable.ic_pause)

                    }
                }

                val durationTotal: Int = mPlayer.duration / 1000
                viewHolder2.binding.receiverEndTime.setText(formattedTime(durationTotal))
                mPlayer.setOnCompletionListener {
                    viewHolder2.binding.audioPlay.setImageResource(R.drawable.ic_play)

                }
            } else {
                viewHolder2.binding.receiverCardview.visibility = View.GONE
                viewHolder2.binding.relativelayout.visibility = View.VISIBLE
            }
            viewHolder2.binding.receiveText.text = message.message
            Glide.with(context).load(profileImage).placeholder(R.drawable.person_crop_circle).into(viewHolder2.binding.imageProfile)
            holder.binding.receiverTime.text  = Util.date2DayTime(Date(message.timeStamp))
            holder.binding.receiverImageTime.text  = Util.date2DayTime(Date(message.timeStamp))
            viewHolder2.itemView.setOnLongClickListener {
                deleteClick.onReceiverClick(position, message)
                false
            }
        }
    }
    private fun formattedTime(mCurrentPosition: Int): String? {
        var totalOut = ""
        var totalNew = ""
        val seconds = (mCurrentPosition % 60).toString()
        val minutes = (mCurrentPosition / 60).toString()
        totalOut = "$minutes:$seconds"
        totalNew = "$minutes:0$seconds"
        return if (seconds.length == 1) {
            totalNew
        } else {
            totalOut
        }
    }



    override fun getItemCount(): Int {
        return messages.size
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: SentContainerBinding

        init {
            binding = SentContainerBinding.bind(itemView)
        }
    }

    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ReceiverContainerBinding

        init {
            binding = ReceiverContainerBinding.bind(itemView)
        }
    }
}
