package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.json.users.ProjectOrdinalEntryJson
import com.krystianwsul.common.firebase.json.users.ProjectOrdinalKeyEntryJson
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class ProjectOrdinalManager(
    private val callbacks: Callbacks,
    val projectKey: ProjectKey.Shared,
    private val ordinalEntries: MutableList<OrdinalEntry>,
) {

    val allEntries: Collection<OrdinalEntry> = ordinalEntries

    private fun Key.Entry.getHourMinute(project: SharedProject) =
        project.getTime(instanceTimePair).getHourMinute(instanceDateOrDayOfWeek.dayOfWeek)

    fun setOrdinal(project: SharedProject, key: Key, ordinal: Double, now: ExactTimeStamp.Local) {
        check(project.projectKey == projectKey)

        check(
            key.entries
                .map { TimeStamp(it.instanceDateOrDayOfWeek.date!!, it.getHourMinute(project)) }
                .distinct()
                .size == 1
        )

        OrdinalEntry(key, Value(ordinal, now)).let {
            ordinalEntries += it
            callbacks.addOrdinalEntry(it)
        }
    }

    private inner class Matcher<T>(private val mostInCommon: Boolean, private val aspectSelector: (Key.Entry) -> T?) {

        fun match(searchKey: Key): Double? {
            fun Key.toMatchElements() = entries.mapNotNull(aspectSelector).toSet()

            val inputMatchElements = searchKey.toMatchElements()
                .takeIf { it.isNotEmpty() }
                ?: return null

            data class MatchData(val ordinalEntry: OrdinalEntry, val matchElements: Set<T>) {

                constructor(ordinalEntry: OrdinalEntry) : this(ordinalEntry, ordinalEntry.key.toMatchElements())
            }

            return ordinalEntries.map(::MatchData)
                .groupBy { it.matchElements.intersect(inputMatchElements).size }
                .filter { it.key > 0 }
                .let {
                    if (mostInCommon) {
                        it.maxByOrNull { it.key }?.value // find the most match elements in common
                    } else {
                        it.flatMap { it.value } // find any match elements in common
                    }
                }
                ?.map { it.ordinalEntry.value }
                ?.maxByOrNull { it.updated }
                ?.ordinal
        }
    }

    fun getOrdinal(project: SharedProject, key: Key): Double {
        check(project.projectKey == projectKey)

        listOf<Matcher<*>>(
            Matcher(false) { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { DateTimePair(it, entry.instanceTimePair) }
            }, // instance dateTimePair
            Matcher(false) { entry ->
                entry.instanceDateOrDayOfWeek
                    .date
                    ?.let { TimeStamp(it, entry.getHourMinute(project)) }
            }, // instance Timestamp
            Matcher(true) {
                it.taskInfo?.let { taskInfo ->
                    taskInfo.scheduleDateTimePair?.let { taskInfo to it }
                }
            }, // instanceKey
            Matcher(true) { it.instanceTimePair }, // instance timePair
            Matcher(true) { it.getHourMinute(project) }, // instance hourMinute
            Matcher(true) { it.taskInfo?.taskKey }, // taskKey
        ).asSequence()
            .mapNotNull { it.match(key) }
            .firstOrNull()
            ?.let { return it }

        // if nothing matches, return the most recently-set ordinal
        ordinalEntries.maxByOrNull { it.value.updated }?.let { return it.value.ordinal }

        return projectKey.getOrdinal()
    }

    data class OrdinalEntry(val key: Key, val value: Value) {

        companion object {

            fun fromJson(
                projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
                json: ProjectOrdinalEntryJson,
            ): OrdinalEntry {
                return OrdinalEntry(
                    Key(
                        json.keyEntries
                            .map { Key.Entry.fromJson(projectCustomTimeIdAndKeyProvider, it) }
                            .toSet()
                    ),
                    Value(json.ordinal, ExactTimeStamp.Local(json.updated))
                )
            }
        }

        fun toJson(): ProjectOrdinalEntryJson {
            return ProjectOrdinalEntryJson(
                key.entries.map { it.toJson() },
                value.ordinal,
                value.updated.long,
            )
        }
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

    fun interface Callbacks {

        fun addOrdinalEntry(ordinalEntry: OrdinalEntry)
    }

    interface Provider {

        fun getProjectOrdinalManager(project: SharedProject): ProjectOrdinalManager
    }
}