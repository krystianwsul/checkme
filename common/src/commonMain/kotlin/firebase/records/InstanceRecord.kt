package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*
import kotlin.jvm.JvmStatic
import kotlin.properties.Delegates.observable

abstract class InstanceRecord<T : ProjectType>(
        create: Boolean,
        protected val taskRecord: TaskRecord<T>,
        final override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        override val key: String,
        val scheduleCustomTimeId: CustomTimeId<T>?
) : RemoteRecord(create) {

    companion object {

        private val hourMinuteRegex = Regex("^\\d\\d:\\d\\d$")

        private val hourMinuteKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val customTimeKeyRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$")

        private fun Int.pad(padding: Boolean) = toString().run { if (padding) padStart(2, '0') else this }

        @JvmStatic
        fun scheduleKeyToDateString(scheduleKey: ScheduleKey, padding: Boolean) = scheduleKey.scheduleDate.run {
            fun Int.pad() = pad(padding)

            "$year-${month.pad()}-${day.pad()}"
        }

        @JvmStatic
        fun scheduleKeyToTimeString(scheduleKey: ScheduleKey, padding: Boolean) = scheduleKey.scheduleTimePair.run {
            fun Int.pad() = pad(padding)

            customTimeKey?.customTimeId ?: hourMinute!!.run { "${hour.pad()}-${minute.pad()}" }
        }

        fun scheduleKeyToString(scheduleKey: ScheduleKey) = scheduleKey.let {
            scheduleKeyToDateString(it, false) + "-" + scheduleKeyToTimeString(it, false)
        }

        @JvmStatic
        protected fun MatchResult.getInt(position: Int) = groupValues[position].toInt()

        fun <T : ProjectType> stringToScheduleKey(
                projectRecord: ProjectRecord<T>,
                key: String
        ): Pair<ScheduleKey, CustomTimeId<T>?> {
            val hourMinuteMatchResult = hourMinuteKeyRegex.find(key)

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

                val customTimeKey = projectRecord.getCustomTimeKey(customTimeId)

                return Pair(ScheduleKey(Date(year, month, day), TimePair(customTimeKey)), customTimeKey.customTimeId)
            }
        }
    }

    var done by Committer(createObject::done)
    var doneOffset by Committer(createObject::doneOffset)

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
                    JsonTime.Custom(taskRecord.projectRecord.getCustomTimeId(it))
            }

    var instanceJsonTime by observable(getInitialInstanceJsonTime()) { _, _, value ->
        setProperty(createObject::instanceTime, value?.toJson())
    }

    var hidden by Committer(createObject::hidden)

    val instanceKey by lazy { InstanceKey(taskRecord.taskKey, scheduleKey) }

    var parentInstanceKey: InstanceKey? by observable(
            createObject.parentJson?.let {
                InstanceKey(
                        TaskKey(taskRecord.projectRecord.projectKey, it.taskId),
                        stringToScheduleKey(taskRecord.projectRecord, it.scheduleKey).first
                )
            }
    ) { _, _, newValue ->
        setProperty(
                createObject::parentJson,
                newValue?.let { InstanceJson.ParentJson(it.taskKey.taskId, scheduleKeyToString(it.scheduleKey)) }
        )
    }

    var noParent by Committer(createObject::noParent)
}
