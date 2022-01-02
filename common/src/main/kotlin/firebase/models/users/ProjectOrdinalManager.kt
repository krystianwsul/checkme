package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.json.users.ProjectOrdinalKeyEntryJson
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey

class ProjectOrdinalManager(val project: SharedProject) {

    private var ordinals = mutableMapOf<Key, Value>()

    val allEntries: Collection<Pair<Key, Value>> = ordinals.entries.map { it.key to it.value }

    fun setOrdinal(key: Key, ordinal: Double, now: ExactTimeStamp.Local) {
        ordinals[key] = Value(ordinal, now)
    }

    private fun <T> getMatchByAspect(searchKey: Key, aspectSelector: (Key.Entry) -> T?): Double? {
        fun Key.toMatchElements() = entries.mapNotNull(aspectSelector).toSet()

        val inputMatchElements = searchKey.toMatchElements()
            .takeIf { it.isNotEmpty() }
            ?: return null

        data class MatchData(val entry: Map.Entry<Key, Value>, val matchElements: Set<T>) {

            constructor(entry: Map.Entry<Key, Value>) : this(entry, entry.key.toMatchElements())
        }

        return ordinals.entries
            .map(::MatchData)
            .groupBy { it.matchElements.intersect(inputMatchElements).size }
            .filter { it.key > 0 }
            .maxByOrNull { it.key } // find the most match elements in common
            ?.value
            ?.map { it.entry.value }
            ?.maxByOrNull { it.updated }
            ?.ordinal
    }

    fun getOrdinal(key: Key): Double {
        fun Key.Entry.getHourMinute() = project.getTime(instanceTimePair).getHourMinute(instanceDateOrDayOfWeek.dayOfWeek)

        listOf<(Key.Entry) -> Any?>(
            {
                it.taskInfo?.let { taskInfo ->
                    taskInfo.scheduleDateTimePair?.let { taskInfo to it }
                }
            }, // instanceKey
            { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { DateTimePair(it, entry.instanceTimePair) }
            }, // instance dateTimePair
            { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { TimeStamp(it, entry.getHourMinute()) }
            }, // instance Timestamp
            { it.instanceTimePair }, // instance timePair
            { it.getHourMinute() }, // instance hourMinute
            { it.taskInfo?.taskKey }, // taskKey
        ).asSequence()
            .mapNotNull { getMatchByAspect(key, it) }
            .firstOrNull()
            ?.let { return it }

        ordinals.values
            .maxByOrNull { it.updated }
            ?.let { return it.ordinal }

        return project.projectKey.getOrdinal()
    }

    data class Key(val entries: Set<Entry>) {

        data class Entry(
            val taskInfo: TaskInfo?,
            val instanceDateOrDayOfWeek: DateOrDayOfWeek,
            val instanceTimePair: TimePair,
        ) {

            constructor(instanceKey: InstanceKey, instanceDateTimePair: DateTimePair) : this(
                TaskInfo(instanceKey),
                DateOrDayOfWeek.Date(instanceDateTimePair.date),
                instanceDateTimePair.timePair,
            )

            fun toJson() = ProjectOrdinalKeyEntryJson(
                taskInfo?.toJson(),
                instanceDateOrDayOfWeek.toJson(),
                instanceTimePair.toJsonTime().toJson(),
            )
        }

        data class TaskInfo(val taskKey: TaskKey, val scheduleDateTimePair: DateTimePair?) {

            constructor(instanceKey: InstanceKey) : this(
                instanceKey.taskKey,
                instanceKey.instanceScheduleKey.run { DateTimePair(scheduleDate, scheduleTimePair) },
            )

            fun toJson() = ProjectOrdinalKeyEntryJson.TaskInfoJson(
                taskKey.toShortcut(),
                scheduleDateTimePair?.toJson(),
            )
        }
    }

    data class Value(val ordinal: Double, val updated: ExactTimeStamp.Local)

    interface Provider {

        fun getProjectOrdinalManager(project: SharedProject): ProjectOrdinalManager
    }
}