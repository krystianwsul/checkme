package com.krystianwsul.checkme

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.krystianwsul.common.ErrorLogger
import io.fabric.sdk.android.Fabric

object MyCrashlytics : ErrorLogger() {

    override val enabled = MyApplication.context
            .resources
            .getBoolean(R.bool.crashlytics_enabled)

    fun init() {
        if (enabled)
            Fabric.with(MyApplication.instance, Answers(), Crashlytics())

        instance = this
    }

    override fun log(message: String) {
        check(message.isNotEmpty())

        Log.e("asdf", "MyCrashLytics.log: $message")
        if (enabled)
            Crashlytics.log(message)
    }

    override fun logException(throwable: Throwable) {
        Log.e("asdf", "MyCrashLytics.logException", throwable)
        if (enabled)
            Crashlytics.logException(throwable)
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

    val answers get() = if (enabled) Answers.getInstance() else null
}
