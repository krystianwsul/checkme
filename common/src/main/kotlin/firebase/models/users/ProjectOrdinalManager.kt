package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.json.users.ProjectOrdinalKeyEntryJson
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class ProjectOrdinalManager(private val timeConverter: TimeConverter, val projectKey: ProjectKey.Shared) {

    private var ordinals = mutableMapOf<Key, Value>()

    val allEntries: Collection<Pair<Key, Value>> = ordinals.entries.map { it.key to it.value }

    private fun Key.Entry.getHourMinute() = timeConverter.getHourMinute(instanceDateOrDayOfWeek.dayOfWeek, instanceTimePair)

    fun setOrdinal(key: Key, ordinal: Double, now: ExactTimeStamp.Local) {
        check(
            key.entries
                .map { TimeStamp(it.instanceDateOrDayOfWeek.date!!, it.getHourMinute()) }
                .distinct()
                .size == 1
        )

        ordinals[key] = Value(ordinal, now)
    }

    private fun <T> getMatchByAspect(searchKey: Key, mostInCommon: Boolean, aspectSelector: (Key.Entry) -> T?): Double? {
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
            .let {
                if (mostInCommon) {
                    it.maxByOrNull { it.key }?.value // find the most match elements in common
                } else {
                    it.flatMap { it.value } // find any match elements in common
                }
            }
            ?.map { it.entry.value }
            ?.maxByOrNull { it.updated }
            ?.ordinal
    }

    fun getOrdinal(key: Key): Double {
        // exact match
        ordinals[key]?.let { return it.ordinal }

        class Matcher<T>(val mostInCommon: Boolean, val aspectSelector: (Key.Entry) -> T?)

        listOf<Matcher<*>>(
            Matcher(false) { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { DateTimePair(it, entry.instanceTimePair) }
            }, // instance dateTimePair
            Matcher(false) { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { TimeStamp(it, entry.getHourMinute()) }
            }, // instance Timestamp
            Matcher(true) {
                it.taskInfo?.let { taskInfo ->
                    taskInfo.scheduleDateTimePair?.let { taskInfo to it }
                }
            }, // instanceKey
            Matcher(true) { it.instanceTimePair }, // instance timePair
            Matcher(true) { it.getHourMinute() }, // instance hourMinute
            Matcher(true) { it.taskInfo?.taskKey }, // taskKey
        ).asSequence()
            .mapNotNull { getMatchByAspect(key, it.mostInCommon, it.aspectSelector) }
            .firstOrNull()
            ?.let { return it }

        // if nothing matches, return the most recently-set ordinal
        ordinals.values
            .maxByOrNull { it.updated }
            ?.let { return it.ordinal }

        return projectKey.getOrdinal()
    }

    data class Key(val entries: Set<Entry>) {

        data class Entry(
            val taskInfo: TaskInfo?,
            val instanceDateOrDayOfWeek: DateOrDayOfWeek,
            val instanceTimePair: TimePair,
        ) {

            companion object {

                fun fromJson(
                    projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
                    entryJson: ProjectOrdinalKeyEntryJson,
                ) = Entry(
                    entryJson.taskInfoJson?.let { TaskInfo.fromJson(projectCustomTimeIdAndKeyProvider, it) },
                    DateOrDayOfWeek.fromJson(entryJson.instanceDateOrDayOfWeek),
                    JsonTime.fromJson(
                        projectCustomTimeIdAndKeyProvider,
                        entryJson.instanceTime,
                    ).toTimePair(projectCustomTimeIdAndKeyProvider),
                )
            }

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

            companion object {

                fun fromJson(
                    projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
                    taskInfoJson: ProjectOrdinalKeyEntryJson.TaskInfoJson,
                ) = TaskInfo(
                    TaskKey.fromShortcut(taskInfoJson.taskKey),
                    taskInfoJson.scheduleDateTimePairJson?.let {
                        DateTimePair.fromJson(projectCustomTimeIdAndKeyProvider, it)
                    },
                )
            }

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

    fun interface TimeConverter {

        fun getHourMinute(dayOfWeek: DayOfWeek, timePair: TimePair): HourMinute
    }

    interface Provider {

        fun getProjectOrdinalManager(project: SharedProject): ProjectOrdinalManager
    }
}