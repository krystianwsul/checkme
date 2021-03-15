package com.krystianwsul.common.firebase

import com.krystianwsul.common.utils.getThreadInfo

class SchedulerException() :
        Exception("scheduler: " + SchedulerTypeHolder.instance.get() + ", threadInfo: " + getThreadInfo())