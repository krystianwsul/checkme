package com.krystianwsul.common.interrupt

actual fun throwIfInterrupted() {
    if (Thread.interrupted()) throw DomainInterruptedException()
}