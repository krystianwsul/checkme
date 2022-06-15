package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.utils.UserKey

object MyCrashlytics : ErrorLogger() {

    override val enabled = MyApplication.context
            .resources
            .getBoolean(R.bool.crashlytics_enabled)

    fun init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        FirebaseAnalytics.getInstance(MyApplication.context).setAnalyticsCollectionEnabled(enabled)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled

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

    fun setUserKey(userKey: UserKey) {
        FirebaseCrashlytics.getInstance().setCustomKey("UserKey", userKey.key)
    }
}
