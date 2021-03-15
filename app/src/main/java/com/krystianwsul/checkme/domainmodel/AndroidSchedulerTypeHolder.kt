package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder

class AndroidSchedulerTypeHolder : SchedulerTypeHolder {

    private val threadLocal = ThreadLocal<SchedulerType>()

    override fun get(): SchedulerType? = threadLocal.get()

    override fun set(schedulerType: SchedulerType) = threadLocal.set(schedulerType)
}