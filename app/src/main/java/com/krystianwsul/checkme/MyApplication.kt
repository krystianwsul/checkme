package com.krystianwsul.checkme

import android.app.Application

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.krystianwsul.checkme.firebase.DatabaseWrapper

import net.danlew.android.joda.JodaTimeAndroid

class MyApplication : Application() {

    companion object {

        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        JodaTimeAndroid.init(this)

        MyCrashlytics.initialize(this)

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        DatabaseWrapper.initialize(this)
    }
}
