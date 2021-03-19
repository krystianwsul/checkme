package com.krystianwsul.common.interrupt

fun interface InterruptionChecker {

    companion object {

        var instance: InterruptionChecker? = null

        fun throwIfInterrupted() {
            if (instance?.isInterrupted() == true) throw DomainInterruptedException()
        }
    }

    fun isInterrupted(): Boolean
}