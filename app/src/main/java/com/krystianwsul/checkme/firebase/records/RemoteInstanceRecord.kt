package com.krystianwsul.checkme.firebase.records


import android.text.TextUtils
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.InstanceRecord
import com.krystianwsul.checkme.firebase.json.InstanceJson
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceData
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.JsonTime
import com.krystianwsul.checkme.utils.time.TimePair
import java.util.regex.Pattern
import kotlin.properties.Delegates

class RemoteInstanceRecord<T : RemoteCustomTimeId>(
        create: Boolean,
        private val remoteTaskRecord: RemoteTaskRecord<T>,
        override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        private val firebaseKey: String,
        override val scheduleCustomTimeId: T?) : RemoteRecord(create), InstanceRecord<T> {

    companion object {

        private val hourMinutePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val customTimePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$")

        fun <T : RemoteCustomTimeId> scheduleKeyToString(scheduleKey: ScheduleKey, remoteCustomTimeId: T?): String {
            var key = scheduleKey.scheduleDate.year.toString() + "-" + scheduleKey.scheduleDate.month + "-" + scheduleKey.scheduleDate.day + "-"
            key += scheduleKey.scheduleTimePair.let {
                if (it.customTimeKey != null) {
                    check(it.hourMinute == null)

                    remoteCustomTimeId?.value
                } else {
                    it.hourMinute!!.hour.toString() + "-" + it.hourMinute.minute
                }
            }

            return key
        }

        fun scheduleKeyToString(domainFactory: DomainFactory, projectId: String, scheduleKey: ScheduleKey): String {
            var key = scheduleKey.scheduleDate.year.toString() + "-" + scheduleKey.scheduleDate.month + "-" + scheduleKey.scheduleDate.day + "-"
            key += scheduleKey.scheduleTimePair.let {
                if (it.customTimeKey != null) {
                    check(it.hourMinute == null)

                    domainFactory.getRemoteCustomTimeId(projectId, it.customTimeKey)
                } else {
                    it.hourMinute!!.hour.toString() + "-" + it.hourMinute.minute
                }
            }

            return key
        }

        fun <T : RemoteCustomTimeId> stringToScheduleKey(remoteProjectRecord: RemoteProjectRecord<T>, key: String): Pair<ScheduleKey, T?> {
            val hourMinuteMatcher = hourMinutePattern.matcher(key)

            if (hourMinuteMatcher.matches()) {
                val year = Integer.valueOf(hourMinuteMatcher.group(1))
                val month = Integer.valueOf(hourMinuteMatcher.group(2))
                val day = Integer.valueOf(hourMinuteMatcher.group(3))
                val hour = Integer.valueOf(hourMinuteMatcher.group(4))
                val minute = Integer.valueOf(hourMinuteMatcher.group(5))

                return Pair(ScheduleKey(Date(year, month, day), TimePair(HourMinute(hour, minute))), null)
            } else {
                val customTimeMatcher = customTimePattern.matcher(key)
                check(customTimeMatcher.matches())

                val year = Integer.valueOf(customTimeMatcher.group(1))
                val month = Integer.valueOf(customTimeMatcher.group(2))
                val day = Integer.valueOf(customTimeMatcher.group(3))

                val customTimeId = customTimeMatcher.group(4)
                check(!TextUtils.isEmpty(customTimeId))

                val remoteCustomTimeId = remoteProjectRecord.getRemoteCustomTimeId(customTimeId)

                val customTimeRecord = remoteProjectRecord.getCustomTimeRecord(customTimeId)

                val customTimeKey = CustomTimeKey.RemoteCustomTimeKey(customTimeRecord.projectId, customTimeRecord.id)

                return Pair(ScheduleKey(Date(year, month, day), TimePair(customTimeKey)), remoteCustomTimeId)
            }
        }
    }

    override val key by lazy { remoteTaskRecord.key + "/instances/" + firebaseKey }

    val taskId by lazy { remoteTaskRecord.id }

    override var done: Long?
        get() = createObject.done
        set(done) {
            if (done == createObject.done)
                return

            createObject.done = done
            addValue("$key/done", done)
        }

    override val scheduleYear by lazy { scheduleKey.scheduleDate.year }
    override val scheduleMonth by lazy { scheduleKey.scheduleDate.month }
    override val scheduleDay by lazy { scheduleKey.scheduleDate.day }

    override val scheduleHour by lazy {
        scheduleKey.scheduleTimePair
                .hourMinute
                ?.hour
    }
    override val scheduleMinute by lazy {
        scheduleKey.scheduleTimePair
                .hourMinute
                ?.minute
    }

    private fun getInitialInstanceDate(): Date? {
        createObject.instanceDate
                .takeUnless { it.isNullOrEmpty() }
                ?.let { return Date.fromJson(it) }

        val instanceYear = createObject.instanceYear
        val instanceMonth = createObject.instanceMonth
        val instanceDay = createObject.instanceDay

        if (((instanceYear != null) != (instanceMonth != null)) || (instanceYear != null) != (instanceDay != null))
            MyCrashlytics.logException(InstanceData.InconsistentInstanceException("instance: " + remoteTaskRecord.key + " " + key + ", instanceYear: $instanceYear, instanceMonth: $instanceMonth, instanceDay: $instanceDay"))

        return if (instanceYear != null && instanceMonth != null && instanceDay != null)
            Date(instanceYear, instanceMonth, instanceDay)
        else
            null
    }

    override var instanceDate by Delegates.observable(getInitialInstanceDate()) { _, _, value ->
        val instanceYear = value!!.year
        val instanceMonth = value.month
        val instanceDay = value.day

        if (instanceYear != createObject.instanceYear) {
            createObject.instanceYear = instanceYear
            addValue("$key/instanceYear", instanceYear)
        }

        if (instanceMonth != createObject.instanceMonth) {
            createObject.instanceMonth = instanceMonth
            addValue("$key/instanceMonth", instanceMonth)
        }

        if (instanceDay != createObject.instanceDay) {
            createObject.instanceDay = instanceDay
            addValue("$key/instanceDay", instanceDay)
        }

        val json = value.toJson()

        if (json != createObject.instanceDate) {
            createObject.instanceDate = json
            addValue("$key/instanceDate", json)
        }
    }

    private val initialInstanceJsonTime: JsonTime<T>?

    init {
        initialInstanceJsonTime = createObject.instanceCustomTimeId
                ?.let { JsonTime.Custom(remoteTaskRecord.getRemoteCustomTimeId(it)) }
                ?: createObject.instanceHour
                        ?.let { hour ->
                            createObject.instanceMinute?.let { JsonTime.Normal<T>(HourMinute(hour, it)) }
                        }
    }

    override var instanceJsonTime by Delegates.observable(initialInstanceJsonTime) { _, _, value ->
        var customTimeId: T? = null
        var hourMinute: HourMinute? = null
        when (value) {
            is JsonTime.Custom -> customTimeId = value.id
            is JsonTime.Normal -> hourMinute = value.hourMinute
        }

        if (customTimeId != createObject.instanceCustomTimeId?.let { remoteTaskRecord.getRemoteCustomTimeId(it) }) {
            createObject.instanceCustomTimeId = customTimeId?.value
            addValue("$key/instanceCustomTimeId", customTimeId?.value)
        }

        if (hourMinute?.hour != createObject.instanceHour) {
            createObject.instanceHour = hourMinute?.hour
            addValue("$key/instanceHour", hourMinute?.hour)
        }

        if (hourMinute?.minute != createObject.instanceMinute) {
            createObject.instanceMinute = hourMinute?.minute
            addValue("$key/instanceMinute", hourMinute?.minute)
        }
    }

    override var ordinal
        get() = createObject.ordinal
        set(ordinal) {
            if (ordinal == createObject.ordinal)
                return

            createObject.ordinal = ordinal
            addValue("$key/ordinal", ordinal)
        }

    override fun deleteFromParent() = check(remoteTaskRecord.remoteInstanceRecords.remove(scheduleKey) == this)
}
