package com.example.lucky

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lucky.NewUtils.isAppActive
import com.example.lucky.NewUtils.viewBinding
import com.example.lucky.ui.MainActivity.Companion.owner
import com.example.lucky.databinding.ActivitySmsBinding



class SmsActivity : AppCompatActivity() {
    val binding by viewBinding(ActivitySmsBinding::inflate)

    lateinit var recyclerview: RecyclerView
    lateinit var list: ArrayList<MessageDetail>
    var message: String = ""

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        list = ArrayList()
        val user = intent.getParcelableExtra<UserModel>("user")

        recyclerview = findViewById(R.id.recyclerview)
//        database = FirebaseDatabase.getInstance().getReference().child(owner).child("messages").child(user!!.key)
//
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (!isAppActive){
//                    return
//                }
//                list.clear()
//                for (snap in snapshot.children) {
//                    var sms = snap.getValue(MessageDetail::class.java)
//                    if (message!=sms?.message?.ifEmpty {sms.e}){
//                        sms?.let { list.add(it) }
//                        message = sms?.message?.ifEmpty {sms.e}.toString()
//                    }
//
//                    Log.i("TAG", "onDataChange: address: ${sms?.number?.ifEmpty {sms.b}}")
//                    Log.i("TAG", "onDataChange: body: ${sms?.message?.ifEmpty {sms.e}}")
//                    showNotification(sms?.number!!, sms.message)
//                    list.reverse()
//                }
//                recyclerview.layoutManager = LinearLayoutManager(this@SmsActivity)
//                list.sortByDescending { it.timeStamp }
//                recyclerview.adapter = SmsAdapter(list, this@SmsActivity)
//                Log.i("TAG", "recyclerview.adapter: ${list.size}")
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//
//        })
    }

    private fun showNotification(sender: String, sms: String) {
        val soundUri: Uri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.water_droplet)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(sender)
            .setContentText(sms)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        notificationManager.notify(1, builder.build())

    }
}