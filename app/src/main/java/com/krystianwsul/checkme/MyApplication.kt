package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.krystianwsul.checkme.firebase.DatabaseWrapper

import net.danlew.android.joda.JodaTimeAndroid

class MyApplication : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MyApplication
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        context = this

        JodaTimeAndroid.init(this)

        MyCrashlytics.initialize(this)

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        DatabaseWrapper.initialize(this)
    }
}
