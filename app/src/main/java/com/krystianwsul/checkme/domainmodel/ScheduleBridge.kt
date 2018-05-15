package com.krystianwsul.checkme.domainmodel

import android.support.v4.util.Pair

import com.krystianwsul.checkme.utils.TaskKey

interface ScheduleBridge {
    
    val startTime: Long

    fun getEndTime(): Long?

    val rootTaskKey: TaskKey

    val remoteCustomTimeKey: Pair<String, String>?

    fun setEndTime(endTime: Long)

    fun delete()
}
