package com.example.lucky.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lucky.Member
import com.example.lucky.MySmsService
import com.example.lucky.NewUtils.showToast
import com.example.lucky.R
import com.example.lucky.Request
import com.example.lucky.SimUtils
import me.ibrahimsn.lib.SmoothBottomBar

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this@MainActivity, "permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun askNotificationPermission(
        context: Context,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                }

                context is Activity && context.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    lateinit var bottomNav: SmoothBottomBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askNotificationPermission(this, requestPermissionLauncher)
        var simNumbers = SimUtils(this).getSimNumbers()
        showToast("numbers: $simNumbers")


        createNotificationChannel()
        val intent = Intent(this, MySmsService::class.java)
        startService(intent)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, Member())
            .commit()

        supportActionBar?.hide()

        bottomNav = findViewById(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {

            when (it) {
                0 -> replaceFragment(Member())
                1 -> replaceFragment(Request())
            }

        }

    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "foxandroidReminderChannel"

            val description = "Channel For Alarm Manger"
            val importance: Int = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel("foxandroid", name, importance)
            channel.description = description

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startService(intent)
    }

    companion object {
        var owner = "dukhan"
    }
}