package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair



import java.util.regex.Pattern

class RemoteInstanceRecord(create: Boolean, private val domainFactory: DomainFactory, private val remoteTaskRecord: RemoteTaskRecord, override val createObject: InstanceJson, val scheduleKey: ScheduleKey) : RemoteRecord(create) {

    companion object {

        private val hourMinutePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val customTimePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$")

        fun scheduleKeyToString(domainFactory: DomainFactory, projectId: String, scheduleKey: ScheduleKey): String {
            var key = scheduleKey.scheduleDate.year.toString() + "-" + scheduleKey.scheduleDate.month + "-" + scheduleKey.scheduleDate.day
            key += scheduleKey.scheduleTimePair.let {
                if (it.customTimeKey != null) {
                    check(it.hourMinute == null)

                    "-" + domainFactory.getRemoteCustomTimeId(projectId, it.customTimeKey)
                } else {
                    check(it.hourMinute != null)

                    "-" + it.hourMinute!!.hour + "-" + it.hourMinute.minute
                }
            }

            return key
        }

        fun stringToScheduleKey(domainFactory: DomainFactory, projectId: String, key: String): ScheduleKey {
            val hourMinuteMatcher = hourMinutePattern.matcher(key)

            if (hourMinuteMatcher.matches()) {
                val year = Integer.valueOf(hourMinuteMatcher.group(1))
                val month = Integer.valueOf(hourMinuteMatcher.group(2))
                val day = Integer.valueOf(hourMinuteMatcher.group(3))
                val hour = Integer.valueOf(hourMinuteMatcher.group(4))
                val minute = Integer.valueOf(hourMinuteMatcher.group(5))

                return ScheduleKey(Date(year, month, day), TimePair(HourMinute(hour, minute)))
            } else {
                val customTimeMatcher = customTimePattern.matcher(key)
                check(customTimeMatcher.matches())

                val year = Integer.valueOf(customTimeMatcher.group(1))
                val month = Integer.valueOf(customTimeMatcher.group(2))
                val day = Integer.valueOf(customTimeMatcher.group(3))

                val customTimeId = customTimeMatcher.group(4)
                check(!TextUtils.isEmpty(customTimeId))

                val customTimeKey = domainFactory.getCustomTimeKey(projectId, customTimeId)

                return ScheduleKey(Date(year, month, day), TimePair(customTimeKey))
            }
        }
    }

    override val key by lazy { remoteTaskRecord.key + "/instances/" + scheduleKeyToString(domainFactory, remoteTaskRecord.projectId, scheduleKey) }

    val taskId by lazy { remoteTaskRecord.id }

    var done: Long?
        get() = createObject.done
        set(done) {
            if (done == createObject.done)
                return

            createObject.done = done
            addValue("$key/done", done)
        }

    val scheduleYear by lazy { scheduleKey.scheduleDate.year }

    val scheduleMonth by lazy { scheduleKey.scheduleDate.month }

    val scheduleDay by lazy { scheduleKey.scheduleDate.day }

    val scheduleCustomTimeId by lazy {
        scheduleKey.scheduleTimePair
                .customTimeKey
                ?.let { domainFactory.getRemoteCustomTimeId(remoteTaskRecord.projectId, it) }
    }

    val scheduleHour by lazy {
        scheduleKey.scheduleTimePair
                .hourMinute
                ?.hour
    }

    val scheduleMinute by lazy {
        scheduleKey.scheduleTimePair
                .hourMinute
                ?.minute
    }

    val instanceYear get() = createObject.instanceYear

    val instanceMonth get() = createObject.instanceMonth

    val instanceDay get() = createObject.instanceDay

    var instanceCustomTimeId
        get() = createObject.instanceCustomTimeId
        set(instanceCustomTimeId) {
            if (instanceCustomTimeId == createObject.instanceCustomTimeId)
                return

            createObject.instanceCustomTimeId = instanceCustomTimeId
            addValue("$key/instanceCustomTimeId", instanceCustomTimeId)
        }

    var instanceHour
        get() = createObject.instanceHour
        set(instanceHour) {
            if (instanceHour == createObject.instanceHour)
                return

            createObject.instanceHour = instanceHour
            addValue("$key/instanceHour", instanceHour)
        }

    var instanceMinute
        get() = createObject.instanceMinute
        set(instanceMinute) {
            if (instanceMinute == createObject.instanceMinute)
                return

            createObject.instanceMinute = instanceMinute
            addValue("$key/instanceMinute", instanceMinute)
        }

    val ordinal get() = createObject.ordinal

    fun setInstanceYear(instanceYear: Int) {
        if (instanceYear == createObject.instanceYear)
            return

        createObject.instanceYear = instanceYear
        addValue("$key/instanceYear", instanceYear)
    }

    fun setInstanceMonth(instanceMonth: Int) {
        if (instanceMonth == createObject.instanceMonth)
            return

        createObject.instanceMonth = instanceMonth
        addValue("$key/instanceMonth", instanceMonth)
    }

    fun setInstanceDay(instanceDay: Int) {
        if (instanceDay == createObject.instanceDay)
            return

        createObject.instanceDay = instanceDay
        addValue("$key/instanceDay", instanceDay)
    }

    fun setOrdinal(ordinal: Double) {
        if (ordinal == createObject.ordinal)
            return

        createObject.ordinal = ordinal
        addValue("$key/ordinal", ordinal)
    }
}
