package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskKey
import kotlin.jvm.JvmStatic
import kotlin.properties.Delegates.observable

class InstanceRecord(
        create: Boolean,
        private val taskRecord: TaskRecord<*>,
        override val createObject: InstanceJson,
        val scheduleKey: ScheduleKey,
        firebaseKey: String,
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

            customTimeKey?.toJson() ?: hourMinute!!.run { "${hour.pad()}-${minute.pad()}" }
        }

        fun scheduleKeyToString(scheduleKey: ScheduleKey) = scheduleKey.let {
            scheduleKeyToDateString(it, false) + "-" + scheduleKeyToTimeString(it, false)
        }

        fun <T : ProjectType> stringToScheduleKey(
                projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider<T>,
                key: String,
        ): ScheduleKey {
            val matchResult = scheduleKeyRegex.find(key)!!

            val dateString = matchResult.groupValues[1]
            val timeString = matchResult.groupValues[2]

            return dateTimeStringsToScheduleKey(projectCustomTimeIdAndKeyProvider, dateString, timeString)
        }

        private val dateRegex = Regex("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)$")
        private val hourMinuteRegex = Regex("^(\\d?\\d)-(\\d?\\d)$")

        private fun MatchResult.getInt(position: Int) = groupValues[position].toInt()

        private fun dateStringToDate(dateString: String): Date {
            val matchResult = dateRegex.find(dateString)!!

            val year = matchResult.getInt(1)
            val month = matchResult.getInt(2)
            val day = matchResult.getInt(3)

            return Date(year, month, day)
        }

        private fun <T : ProjectType> dateTimeStringsToScheduleKey(
                projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider<T>,
                dateString: String,
                timeString: String,
        ): ScheduleKey {
            val date = dateStringToDate(dateString)

            val jsonTime = hourMinuteRegex.find(timeString)?.let { matchResult ->
                val (hour, minute) = (1..2).map { matchResult.groupValues[it].toInt() }

                JsonTime.Normal(HourMinute(hour, minute))
            } ?: JsonTime.fromJson(projectCustomTimeIdAndKeyProvider, timeString)

            return ScheduleKey(date, jsonTime.toTimePair(projectCustomTimeIdAndKeyProvider))
        }
    }

    override val key = taskRecord.key + "/instances/" + firebaseKey

    var done by Committer(createObject::done)
    var doneOffset by Committer(createObject::doneOffset)

    val scheduleYear by lazy { scheduleKey.scheduleDate.year }
    val scheduleMonth by lazy { scheduleKey.scheduleDate.month }
    val scheduleDay by lazy { scheduleKey.scheduleDate.day }

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

    override fun deleteFromParent() = check(taskRecord.instanceRecords.remove(scheduleKey) == this)
}
