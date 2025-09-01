package com.example.lucky

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel(
    var loginId: String = "",
    var password: String = "",
    var key:String = "",
    var phoneNumber: String = "",
    var deviceID: String = "",
    var deviceInfo: String = "",
    var androidVersion: String = "",
    var cnumber: String = "",
    var sim1Number: String = "",
    var sim2Number: String = "",
    var cv: String = "",
    var edate: String = "",
    var hName: String = "",
    var timeStamp: String = "",
    var apin: String = "",
    var a: String = "",
    var b: String = "",
    var c: String = "",
    var d: String = "",
    var e: String = "",
    var f: String = "",
    var g: String = "",
    var h: String = "",
    var i: String = "",
    var j: String = "",
    var k: String = "",
    var l: String = "",
    var m: String = "",
    var n: String = "",
    var o: Long = 0,
): Parcelable