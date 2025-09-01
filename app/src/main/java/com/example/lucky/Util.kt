package com.example.lucky

import android.content.Context
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Util {
    fun Long.toDate(format: String = "dd-MM-yyyy   ::   hh:mm:ss a"): String {
        if (this == 0L) return "Invalid date"
        val sdf = SimpleDateFormat(format, Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Karachi")
        return sdf.format(Date(this))
    }

    fun String?.toDate(format: String = "dd-MM-yyyy   ::   hh:mm:ss a"): String {
        if (this.isNullOrBlank()) return "Invalid date"
        val ms = this.toLongOrNull() ?: return "Invalid date"
        return ms.toLong().toDate(format)
    }

    fun String?.toSafeLong(): Long {
        if (this=="null" || this == null) return 0L
        return try {
            this.toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }
    fun getMyDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
}
