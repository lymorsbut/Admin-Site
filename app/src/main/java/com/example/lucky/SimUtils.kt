package com.example.lucky
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.annotation.RequiresPermission

class SimUtils(private val context: Context) {

    @SuppressLint("HardwareIds")
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getSimNumbers(): MutableList<String> {
        return try {
            val simNumbers = mutableListOf<String>()

            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val subscriptionInfos = subscriptionManager.activeSubscriptionInfoList

            if (!subscriptionInfos.isNullOrEmpty()) {
                subscriptionInfos.forEach { subscriptionInfo ->
                    // Try to get the phone number from SubscriptionInfo
                    val simNumber = subscriptionInfo.number

                    if (!simNumber.isNullOrBlank()) {
                        simNumbers.add(simNumber)
                    } else {
                        // Fallback to TelephonyManager for the phone number
                        val subscriptionId = subscriptionInfo.subscriptionId
                        val phoneNumber = telephonyManager.createForSubscriptionId(subscriptionId).line1Number
                        if (!phoneNumber.isNullOrBlank()) {
                            simNumbers.add(phoneNumber)
                        }
                    }
                }
            }
            simNumbers
        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission denied. Please grant the READ_PHONE_STATE permission.", Toast.LENGTH_LONG).show()
            mutableListOf()
        } catch (e: Exception) {
            Toast.makeText(context, "An error occurred while retrieving SIM numbers: ${e.message}", Toast.LENGTH_LONG).show()
            mutableListOf()
        }
    }
}