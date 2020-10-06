package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.krystianwsul.common.ErrorLogger

object MyCrashlytics : ErrorLogger() {

    override val enabled = false /* MyApplication.context
            .resources
            .getBoolean(R.bool.crashlytics_enabled)
            */

    fun init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)

        instance = this
    }

    override fun log(message: String) {
        check(message.isNotEmpty())

        Log.e("asdf", "MyCrashLytics.log: $message")
        if (enabled)
            FirebaseCrashlytics.getInstance().log(message)
    }

    override fun logException(throwable: Throwable) {
        Log.e("asdf", "MyCrashLytics.logException", throwable)
        if (enabled)
            FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override fun logMethod(obj: Any) {
        val stackTraceElements = Thread.currentThread().stackTrace
        val caller = stackTraceElements[3]

        val method = caller.methodName

        log(obj.javaClass.simpleName + "." + method + " " + obj.hashCode())
    }

    override fun logMethod(obj: Any, message: String) {
        val stackTraceElements = Thread.currentThread().stackTrace
        val caller = stackTraceElements[3]

        val method = caller.methodName

        log(obj.javaClass.simpleName + "." + method + " " + obj.hashCode() + ": $message")
    }
}
