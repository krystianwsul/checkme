package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.DomainThreadChecker

class AndroidDomainThreadChecker : DomainThreadChecker {

    private val threadLocal = ThreadLocal<Boolean>()

    override fun isDomainThread() = threadLocal.get() ?: false

    override fun setDomainThread() = threadLocal.set(true)
}