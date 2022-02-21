package com.krystianwsul.common.firebase.records


import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*
import kotlin.properties.Delegates.observable

class InstanceRecord(
    create: Boolean,
    private val taskRecord: TaskRecord,
    override val createObject: InstanceJson,
    val instanceScheduleKey: InstanceScheduleKey,
    firebaseKey: String,
) : RemoteRecord(create) {

    companion object {

        private val scheduleKeyRegex = Regex("^(\\d\\d\\d\\d-\\d?\\d-\\d?\\d)-(.+)$")

        private fun scheduleKeyToDateString(instanceScheduleKey: InstanceScheduleKey) =
            instanceScheduleKey.scheduleDate.run { "$year-$month-$day" }

        private fun scheduleKeyToTimeString(instanceScheduleKey: InstanceScheduleKey) =
            instanceScheduleKey.scheduleTimePair.run { customTimeKey?.toJson() ?: hourMinute!!.run { "$hour-$minute" } }

        fun scheduleKeyToString(instanceScheduleKey: InstanceScheduleKey) = instanceScheduleKey.let {
            scheduleKeyToDateString(it) + "-" + scheduleKeyToTimeString(it)
        }

        fun stringToScheduleKey(
            projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
            key: String,
        ): InstanceScheduleKey {
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

        private fun dateTimeStringsToScheduleKey(
            projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
            dateString: String,
            timeString: String,
        ): InstanceScheduleKey {
            val date = dateStringToDate(dateString)

            val jsonTime = hourMinuteRegex.find(timeString)?.let { matchResult ->
                val (hour, minute) = (1..2).map { matchResult.groupValues[it].toInt() }

                JsonTime.Normal(HourMinute(hour, minute))
            } ?: JsonTime.fromJson(projectCustomTimeIdAndKeyProvider, timeString)

            return InstanceScheduleKey(date, jsonTime.toTimePair(projectCustomTimeIdAndKeyProvider))
        }
    }

    override val key = taskRecord.key + "/instances/" + firebaseKey

    var done by Committer(createObject::done)
    var doneOffset by Committer(createObject::doneOffset)

    val scheduleYear by lazy { instanceScheduleKey.scheduleDate.year }
    val scheduleMonth by lazy { instanceScheduleKey.scheduleDate.month }
    val scheduleDay by lazy { instanceScheduleKey.scheduleDate.day }

    private fun getInitialInstanceDate() = createObject.instanceDate
        .takeUnless { it.isNullOrEmpty() }
        ?.let { Date.fromJson(it) }

    var instanceDate by observable(getInitialInstanceDate()) { _, _, value ->
        setProperty(createObject::instanceDate, value?.toJson())
    }

    private fun getInitialInstanceJsonTime() =
        createObject.instanceTime?.let { JsonTime.fromJson(taskRecord.projectCustomTimeIdAndKeyProvider, it) }

    var instanceJsonTime: JsonTime? by observable(getInitialInstanceJsonTime()) { _, _, value ->
        setProperty(createObject::instanceTime, value?.toJson())

        instanceCustomTimeKeyProperty.invalidate()
    }

    private val instanceCustomTimeKeyProperty = invalidatableLazy {
        instanceJsonTime?.getCustomTimeKey(taskRecord.projectCustomTimeIdAndKeyProvider)
    }
    val instanceCustomTimeKey by instanceCustomTimeKeyProperty

    var hidden by Committer(createObject::hidden)

    val instanceKey by lazy { InstanceKey(taskRecord.taskKey, instanceScheduleKey) }

    var parentInstanceKey: InstanceKey? by observable(
        createObject.parentJson?.let {
            InstanceKey(
                TaskKey.Root(it.taskId),
                stringToScheduleKey(taskRecord.projectCustomTimeIdAndKeyProvider, it.scheduleKey),
            )
        }
    ) { _, _, newValue ->
        newValue?.let { check(it.taskKey is TaskKey.Root) }

        setProperty(
            createObject::parentJson,
            newValue?.let(InstanceJson::ParentJson)
        )
    }

    var noParent by Committer(createObject::noParent)

    // Ordinal.fromJson isn't called by ref because of a weird JS issue
    var ordinal by observable(createObject.ordinal?.let { Ordinal.fromJson(it) }) { _, _, newValue ->
        setProperty(createObject::ordinal, newValue?.toString())
    }

    var groupByProject by Committer(createObject::groupByProject)

    override fun deleteFromParent() = check(taskRecord.instanceRecords.remove(instanceScheduleKey) == this)
}
