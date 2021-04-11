package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*
import kotlin.jvm.JvmStatic
import kotlin.properties.Delegates.observable

abstract class InstanceRecord<T : ProjectType>(
        create: Boolean,
        protected val taskRecord: TaskRecord<T>,
        final override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        override val key: String,
        val scheduleCustomTimeId: CustomTimeId.Project<T>?,
) : RemoteRecord(create) {

    companion object {

        private val scheduleKeyRegex = Regex("^(\\d\\d\\d\\d-\\d?\\d-\\d?\\d)-(.+)$")

        private fun Int.pad(padding: Boolean) = toString().run { if (padding) padStart(2, '0') else this }

        @JvmStatic
        fun scheduleKeyToDateString(scheduleKey: ScheduleKey, padding: Boolean) = scheduleKey.scheduleDate.run {
            fun Int.pad() = pad(padding)

            "$year-${month.pad()}-${day.pad()}"
        }

        @JvmStatic
        fun scheduleKeyToTimeString(scheduleKey: ScheduleKey, padding: Boolean) = scheduleKey.scheduleTimePair.run {
            fun Int.pad() = pad(padding)

            customTimeKey?.customTimeId?.value ?: hourMinute!!.run { "${hour.pad()}-${minute.pad()}" }
        }

        fun scheduleKeyToString(scheduleKey: ScheduleKey) = scheduleKey.let {
            scheduleKeyToDateString(it, false) + "-" + scheduleKeyToTimeString(it, false)
        }

        // todo customtime use jsontime here we can just change the second type to jsontime.  Or, just use schedulekey
        fun <T : ProjectType> stringToScheduleKey(
                projectRecord: ProjectRecord<T>,
                key: String,
        ): ScheduleKey {
            val matchResult = scheduleKeyRegex.find(key)!!

            val dateString = matchResult.groupValues[0]
            val timeString = matchResult.groupValues[1]

            return dateTimeStringsToScheduleKey(projectRecord, dateString, timeString)
        }

        // todo customtime cleanup (presumably will be using JsonTime eventually)
        fun <T : ProjectType> dateTimeStringsToScheduleKey(
                projectRecord: ProjectRecord<T>,
                dateString: String,
                timeString: String,
        ): ScheduleKey {
            val jsonTime = JsonTime.fromJson(projectRecord, timeString)

            return ScheduleKey(Date.fromJson(dateString), jsonTime.toTimePair(projectRecord))
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
        setProperty(createObject::instanceDate, value?.toJson())
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun getInitialInstanceJsonTime() = createObject.instanceTime
            ?.let { JsonTime.fromJson(taskRecord.projectRecord, it) }

    var instanceJsonTime by observable(getInitialInstanceJsonTime()) { _, _, value ->
        setProperty(createObject::instanceTime, value?.toJson())
    }

    var hidden by Committer(createObject::hidden)

    val instanceKey by lazy { InstanceKey(taskRecord.taskKey, scheduleKey) }

    var parentInstanceKey: InstanceKey? by observable(
            createObject.parentJson?.let {
                InstanceKey(
                        TaskKey(taskRecord.projectRecord.projectKey, it.taskId),
                        stringToScheduleKey(taskRecord.projectRecord, it.scheduleKey),
                )
            }
    ) { _, _, newValue ->
        setProperty(
                createObject::parentJson,
                newValue?.let(InstanceJson::ParentJson)
        )
    }

    var noParent by Committer(createObject::noParent)
}
