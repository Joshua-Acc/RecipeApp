package com.example.recipeapp

import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {
    private const val PREF_NAME = "MyAppPrefs"
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var uid: String?
        get() = preferences.getString("uid", null)
        set(value) = preferences.edit().putString("uid", value).apply()

    var username: String?
        get() = preferences.getString("username", null)
        set(value) = preferences.edit().putString("username", value).apply()

    var email: String?
        get() = preferences.getString("email", null)
        set(value) = preferences.edit().putString("email", value).apply()

    var isLoggedIn: Boolean
        get() = preferences.getBoolean("isLoggedIn", false)
        set(value) = preferences.edit().putBoolean("isLoggedIn", value).apply()

    var activateKey: String?
        get() = preferences.getString("activateKey", null)
        set(value) = preferences.edit().putString("activateKey", value).apply()



    fun clearAll() {
        preferences.edit().clear().apply()
    }
}
