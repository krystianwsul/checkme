package com.krystianwsul.checkme.domainmodel

import android.content.Context
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
import java.util.*

class SingleSchedule(domainFactory: DomainFactory, private val singleScheduleBridge: SingleScheduleBridge) : Schedule(domainFactory) {

    override val scheduleBridge get() = singleScheduleBridge

    private val time: Time
        get() {
            val customTimeKey = singleScheduleBridge.customTimeKey
            return if (customTimeKey != null) {
                domainFactory.getCustomTime(customTimeKey)
            } else {
                val hour = singleScheduleBridge.hour!!
                val minute = singleScheduleBridge.minute!!
                NormalTime(hour, minute)
            }
        }

    val timePair: TimePair
        get() {
            val customTimeKey = singleScheduleBridge.customTimeKey
            val hour = singleScheduleBridge.hour
            val minute = singleScheduleBridge.minute

            return if (customTimeKey != null) {
                Assert.assertTrue(hour == null)
                Assert.assertTrue(minute == null)

                TimePair(customTimeKey)
            } else {
                TimePair(HourMinute(hour!!, minute!!))
            }
        }

    val date get() = Date(singleScheduleBridge.year, singleScheduleBridge.month, singleScheduleBridge.day)

    private val dateTime get() = DateTime(date, time)

    override val customTimeKey get() = singleScheduleBridge.customTimeKey

    override val scheduleType get() = ScheduleType.SINGLE

    override fun getScheduleText(context: Context) = dateTime.getDisplayText(context)

    private fun getInstance(task: Task): Instance {
        val instanceKey = InstanceKey(task.taskKey, date, timePair)

        return domainFactory.getInstance(instanceKey)
    }

    override fun getNextAlarm(now: ExactTimeStamp): TimeStamp? {
        val timeStamp = dateTime.timeStamp
        return if (timeStamp.toExactTimeStamp() > now)
            timeStamp
        else
            null
    }

    override fun getInstances(task: Task, givenStartExactTimeStamp: ExactTimeStamp?, givenExactEndTimeStamp: ExactTimeStamp): List<Instance> {
        val instances = ArrayList<Instance>()

        val singleScheduleExactTimeStamp = dateTime.timeStamp.toExactTimeStamp()

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp > singleScheduleExactTimeStamp)
            return instances

        if (givenExactEndTimeStamp <= singleScheduleExactTimeStamp)
            return instances

        val endExactTimeStamp = getEndExactTimeStamp()
        if (endExactTimeStamp != null && singleScheduleExactTimeStamp >= endExactTimeStamp)
        // timezone hack
            return instances

        instances.add(getInstance(task))

        return instances
    }

    override fun isVisible(task: Task, now: ExactTimeStamp): Boolean {
        Assert.assertTrue(current(now))

        return getInstance(task).isVisible(now)
    }
}
