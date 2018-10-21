package com.krystianwsul.checkme

import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

object MyCrashlytics {

    var enabled = false
        private set

    fun initialize(myApplication: MyApplication) {
        enabled = myApplication.resources.getBoolean(R.bool.crashlytics_enabled)

        if (enabled)
            Fabric.with(myApplication, Crashlytics())
    }

    fun log(message: String) {
        check(!TextUtils.isEmpty(message))

        Log.e("asdf", "MyCrashLytics.log: $message")
        if (enabled)
            Crashlytics.log(message)
    }

    fun logException(throwable: Throwable) {
        Log.e("asdf", "MyCrashLytics.logException", throwable)
        if (enabled)
            Crashlytics.logException(throwable)
    }
}
