package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import net.danlew.android.joda.JodaTimeAndroid

class MyApplication : Application() {

    companion object {

        private const val TOKEN_KEY = "token"

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MyApplication
            private set
    }

    lateinit var sharedPreferences: SharedPreferences
        private set

    var token: String?
        get() = sharedPreferences.getString(TOKEN_KEY, null)
        set(value) = sharedPreferences.edit()
                .putString(TOKEN_KEY, value)
                .apply()

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        instance = this

        JodaTimeAndroid.init(this)

        MyCrashlytics.initialize(this)

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        DatabaseWrapper.initialize(this)

        if (token == null)
            FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnSuccessListener { token = it.token }
    }
}
