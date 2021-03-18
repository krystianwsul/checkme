package com.krystianwsul.common.firebase

interface DomainThreadChecker {

    companion object {

        lateinit var instance: DomainThreadChecker
    }

    fun isDomainThread(): Boolean

    fun setDomainThread()

    fun requireDomainThread() {
        if (!isDomainThread()) throw SchedulerException()
    }
}