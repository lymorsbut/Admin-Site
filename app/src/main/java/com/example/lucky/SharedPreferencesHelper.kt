package com.example.lucky

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment

class SharedPreferencesHelper(context: Context) {

    companion object {
        val Fragment.sharedPref:SharedPreferencesHelper
            get() = SharedPreferencesHelper(requireActivity())
        val Context.sharedPref:SharedPreferencesHelper
            get() = SharedPreferencesHelper(this)

        private const val PREFS_NAME = "YourAppPrefs"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun clear(){
        sharedPreferences.edit().clear().apply()
    }

    // Add more methods for other data types as needed
}
