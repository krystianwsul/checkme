package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.ErrorLogger

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ScheduleKey
import kotlin.properties.Delegates.observable

class InstanceRecord<T : CustomTimeId>(
        create: Boolean,
        private val remoteTaskRecord: RemoteTaskRecord<T, *>,
        override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        private val firebaseKey: String,
        val scheduleCustomTimeId: T?
) : RemoteRecord(create) {

    companion object {

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        private val hourMinuteKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val customTimeKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$")

        fun scheduleKeyToString(scheduleKey: ScheduleKey): String {
            var key = scheduleKey.scheduleDate.year.toString() + "-" + scheduleKey.scheduleDate.month + "-" + scheduleKey.scheduleDate.day + "-"
            key += scheduleKey.scheduleTimePair.let {
                if (it.customTimeKey != null) {
                    check(it.hourMinute == null)

                    it.customTimeKey
                            .customTimeId
                            .value
                } else {
                    it.hourMinute!!.run { "$hour-$minute" }
                }
            }

            return key
        }

        fun <T : CustomTimeId> stringToScheduleKey(
                remoteProjectRecord: RemoteProjectRecord<T, *>,
                key: String
        ): Pair<ScheduleKey, T?> {
            val hourMinuteMatchResult = hourMinuteKeyRegex.find(key)

            fun MatchResult.getInt(position: Int) = groupValues[position].toInt()

            if (hourMinuteMatchResult != null) {
                val year = hourMinuteMatchResult.getInt(1)
                val month = hourMinuteMatchResult.getInt(2)
                val day = hourMinuteMatchResult.getInt(3)
                val hour = hourMinuteMatchResult.getInt(4)
                val minute = hourMinuteMatchResult.getInt(5)

                return Pair(ScheduleKey(Date(year, month, day), TimePair(HourMinute(hour, minute))), null)
            } else {
                val customTimeMatchResult = customTimeKeyRegex.find(key)
                checkNotNull(customTimeMatchResult)

                val year = customTimeMatchResult.getInt(1)
                val month = customTimeMatchResult.getInt(2)
                val day = customTimeMatchResult.getInt(3)

                val customTimeId = customTimeMatchResult.groupValues[4]
                check(customTimeId.isNotEmpty())

                val customTimeKey = remoteProjectRecord.getRemoteCustomTimeKey(customTimeId)

                return Pair(ScheduleKey(Date(year, month, day), TimePair(customTimeKey)), customTimeKey.customTimeId)
            }
        }
    }

    override val key by lazy { remoteTaskRecord.key + "/instances/" + firebaseKey }

    val taskId by lazy { remoteTaskRecord.id }

    var done by Committer(createObject::done)

    val scheduleYear by lazy { scheduleKey.scheduleDate.year }
    val scheduleMonth by lazy { scheduleKey.scheduleDate.month }
    val scheduleDay by lazy { scheduleKey.scheduleDate.day }

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

    private fun getInitialInstanceDate(): Date? {
        createObject.instanceDate
                .takeUnless { it.isNullOrEmpty() }
                ?.let { return Date.fromJson(it) }

        val instanceYear = createObject.instanceYear
        val instanceMonth = createObject.instanceMonth
        val instanceDay = createObject.instanceDay

        if (((instanceYear != null) != (instanceMonth != null)) || (instanceYear != null) != (instanceDay != null))
            ErrorLogger.instance.logException(InconsistentInstanceException("instance: " + remoteTaskRecord.key + " " + key + ", instanceYear: $instanceYear, instanceMonth: $instanceMonth, instanceDay: $instanceDay"))

        return if (instanceYear != null && instanceMonth != null && instanceDay != null)
            Date(instanceYear, instanceMonth, instanceDay)
        else
            null
    }

    var instanceDate by observable(getInitialInstanceDate()) { _, _, value ->
        setProperty(createObject::instanceYear, value!!.year)
        setProperty(createObject::instanceMonth, value.month)
        setProperty(createObject::instanceDay, value.day)
        setProperty(createObject::instanceDate, value.toJson())
    }

    private val initialInstanceJsonTime: JsonTime<T>?

    init {
        initialInstanceJsonTime = createObject.instanceTime
                ?.let {
                    val matchResult = hourMinuteRegex.find(it)
                    if (matchResult != null)
                        JsonTime.Normal<T>(HourMinute.fromJson(it))
                    else
                        JsonTime.Custom(remoteTaskRecord.getcustomTimeId(it))
                }
                ?: createObject.instanceCustomTimeId?.let { JsonTime.Custom(remoteTaskRecord.getcustomTimeId(it)) }
                        ?: createObject.instanceHour?.let { hour -> createObject.instanceMinute?.let { JsonTime.Normal<T>(HourMinute(hour, it)) } }
    }

    var instanceJsonTime by observable(initialInstanceJsonTime) { _, _, value ->
        var customTimeId: T? = null
        var hourMinute: HourMinute? = null

        when (value) {
            is JsonTime.Custom -> customTimeId = value.id
            is JsonTime.Normal -> hourMinute = value.hourMinute
        }

        setProperty(createObject::instanceCustomTimeId, customTimeId?.value)
        setProperty(createObject::instanceHour, hourMinute?.hour)
        setProperty(createObject::instanceMinute, hourMinute?.minute)
        setProperty(createObject::instanceTime, value?.toJson())
    }

    var ordinal by Committer(createObject::ordinal)
    var hidden by Committer(createObject::hidden)

    override fun deleteFromParent() = check(remoteTaskRecord.remoteInstanceRecords.remove(scheduleKey) == this)

    class InconsistentInstanceException(message: String) : Exception(message)
}
