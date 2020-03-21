package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey
import kotlin.properties.Delegates.observable

class InstanceRecord<T : ProjectType>(
        create: Boolean,
        private val taskRecord: TaskRecord<T>,
        override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        private val firebaseKey: String,
        val scheduleCustomTimeId: CustomTimeId<T>?
) : RemoteRecord(create) {

    companion object {

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        private val hourMinuteKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val customTimeKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$")

        fun scheduleKeyToString(scheduleKey: ScheduleKey) = scheduleKey.run {
            scheduleDate.run { "$year-$month-$day-" } + scheduleTimePair.run {
                customTimeKey?.customTimeId ?: hourMinute!!.run { "$hour-$minute" }
            }
        }

        fun <T : ProjectType> stringToScheduleKey(
                remoteProjectRecord: RemoteProjectRecord<T>,
                key: String
        ): Pair<ScheduleKey, CustomTimeId<T>?> {
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

    override val key by lazy { taskRecord.key + "/instances/" + firebaseKey }

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

    private fun getInitialInstanceDate() = createObject.instanceDate
                .takeUnless { it.isNullOrEmpty() }
            ?.let { Date.fromJson(it) }

    var instanceDate by observable(getInitialInstanceDate()) { _, _, value ->
        setProperty(createObject::instanceDate, value!!.toJson())
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun getInitialInstanceJsonTime() = createObject.instanceTime
            ?.let {
                if (hourMinuteRegex.find(it) != null)
                    JsonTime.Normal<T>(HourMinute.fromJson(it))
                else
                    JsonTime.Custom(taskRecord.getcustomTimeId(it))
            }

    var instanceJsonTime by observable(getInitialInstanceJsonTime()) { _, _, value ->
        setProperty(createObject::instanceTime, value?.toJson())
    }

    var ordinal by Committer(createObject::ordinal)
    var hidden by Committer(createObject::hidden)

    override fun deleteFromParent() = check(taskRecord.remoteInstanceRecords.remove(scheduleKey) == this)
}
