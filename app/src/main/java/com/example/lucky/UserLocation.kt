package com.example.lucky

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserLocation(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var timestamp: Long = 0,
): Parcelable