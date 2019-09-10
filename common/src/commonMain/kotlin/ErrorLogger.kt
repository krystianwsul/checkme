package com.krystianwsul.common

abstract class ErrorLogger {

    companion object {

        lateinit var instance: ErrorLogger
    }

    abstract val enabled: Boolean

    abstract fun log(message: String)

    abstract fun logException(throwable: Throwable)

    abstract fun logMethod(obj: Any)

    abstract fun logMethod(obj: Any, message: String)
}
