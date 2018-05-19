package com.krystianwsul.checkme.firebase.json

import junit.framework.Assert

abstract class ScheduleJson(val startTime: Long = 0, var endTime: Long? = null) {

    init {
        Assert.assertTrue(endTime == null || startTime <= endTime!!)
    }
}
