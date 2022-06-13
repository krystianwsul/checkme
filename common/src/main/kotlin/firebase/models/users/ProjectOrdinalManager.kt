package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.json.users.ProjectOrdinalEntryJson
import com.krystianwsul.common.firebase.json.users.ProjectOrdinalKeyEntryJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Ordinal
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class ProjectOrdinalManager(
    private val callbacks: Callbacks,
    val projectKey: ProjectKey.Shared,
    private val ordinalEntries: MutableList<OrdinalEntry>,
) {

    val allEntries: Collection<OrdinalEntry> = ordinalEntries

    private fun Key.Entry.getHourMinute(project: SharedOwnedProject) =
        project.getTime(instanceTimePair).getHourMinute(instanceDateOrDayOfWeek.dayOfWeek)

    fun setOrdinal(project: SharedOwnedProject, key: Key, ordinal: Ordinal, now: ExactTimeStamp.Local) {
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

        fun match(searchKey: Key): Ordinal? {
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
                ?.map { it.ordinalEntry }
                ?.sortedWith(
                    compareBy(
                        { it.key.parentState is Key.ParentState.Set },
                        { it.key.parentState == searchKey.parentState },
                        { it.value.updated },
                    )
                )
                ?.lastOrNull()
                ?.value
                ?.ordinal
        }
    }

    fun getOrdinal(project: SharedOwnedProject, key: Key): Ordinal {
        check(project.projectKey == projectKey)

        listOf<Matcher<*>>(
            Matcher(false) { entry ->
                if (entry.taskInfo?.scheduleDateTimePair == null && entry.instanceDateOrDayOfWeek.date != null) {
                    null
                } else {
                    entry
                }
            }, // instanceKey and instance dateTimePair
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
                it.taskInfo?.takeIf { it.scheduleDateTimePair != null }
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
                            .map { Key.Entry.fromJson(projectCustomTimeIdAndKeyProvider, it.value) }
                            .toSet(),
                        Key.ParentState.fromJson(projectCustomTimeIdAndKeyProvider, json.parentInstanceKey),
                    ),
                    Value(Ordinal.fromFields(json.ordinal, json.ordinalString)!!, ExactTimeStamp.Local(json.updated))
                )
            }
        }

        fun toJson(): ProjectOrdinalEntryJson {
            val (ordinalDouble, ordinalString) = value.ordinal.toFields()

            return ProjectOrdinalEntryJson(
                key.entries
                    .mapIndexed { index, entry -> "a$index" to entry.toJson() }
                    .toMap(), // ridiculous hack to fix Java vs. JS parsing
                ordinalDouble,
                ordinalString,
                value.updated.long,
                key.parentState.toJson(),
            )
        }
    }

    data class Key(val entries: Set<Entry>, val parentState: ParentState) {

        constructor(instances: Collection<Instance>, parentInstance: Instance?) : this(
            instances.map(::Entry).toSet(),
            ParentState.Set(parentInstance?.instanceKey),
        )

        constructor(entries: Set<Entry>, parentInstanceKey: InstanceKey?) : this(entries, ParentState.Set(parentInstanceKey))

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

            constructor(instance: Instance) : this(
                instance.instanceKey,
                instance.instanceDateTime.toDateTimePair(),
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

        sealed class ParentState {

            companion object {

                fun fromJson(
                    projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
                    json: String?,
                ) = when (json) {
                    Unset.key -> Unset
                    else -> Set.fromJson(projectCustomTimeIdAndKeyProvider, json)
                }
            }

            abstract fun toJson(): String?

            object Unset : ParentState() {

                val key = "unset"

                override fun toJson(): String = "unset"
            }

            data class Set(val parentInstanceKey: InstanceKey?) : ParentState() {

                companion object {

                    fun fromJson(
                        projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
                        json: String?,
                    ) = Set(json?.let { InstanceKey.fromJson(projectCustomTimeIdAndKeyProvider, it) })
                }

                override fun toJson() = parentInstanceKey?.toJson()
            }
        }
    }

    data class Value(val ordinal: Ordinal, val updated: ExactTimeStamp.Local)

    fun interface Callbacks {

        fun addOrdinalEntry(ordinalEntry: OrdinalEntry)
    }

    interface Provider {

        fun getProjectOrdinalManager(project: SharedOwnedProject): ProjectOrdinalManager
    }
}