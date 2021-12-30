package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

class ProjectOrdinalManager(private val project: SharedProject) {

    private var ordinals = mutableMapOf<Key, Value>()

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
        fun DateTimePair.getHourMinute() = project.getTime(timePair).getHourMinute(date.dayOfWeek)

        listOf<(Key.Entry) -> Any?>(
            { it.instanceKey }, // instanceKey (taskKey + customTime/hourMinute)
            { it.instanceDateTimePair }, // instance dateTimePair
            { // instance Timestamp
                it.instanceDateTimePair.run { TimeStamp(date, getHourMinute()) }
            },
            { it.instanceDateTimePair.timePair }, // instance timePair
            { it.instanceDateTimePair.getHourMinute() }, // instance hourMinute
            { it.instanceKey.taskKey }, // taskKey
        ).asSequence()
            .mapNotNull { getMatchByAspect(key, it) }
            .firstOrNull()
            ?.let { return it }

        return project.projectKey.getOrdinal()
    }

    data class Key(val entries: Set<Entry>) {

        data class Entry(val instanceKey: InstanceKey, val instanceDateTimePair: DateTimePair)
    }

    private data class Value(val ordinal: Double, val updated: ExactTimeStamp.Local)

    interface Provider {

        fun getProjectOrdinalManager(project: SharedProject): ProjectOrdinalManager
    }
}