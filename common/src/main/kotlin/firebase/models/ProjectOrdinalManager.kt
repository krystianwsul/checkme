package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

class ProjectOrdinalManager(private val project: SharedProject) {

    private var ordinals = mutableMapOf<Key, Value>()

    fun setOrdinal(instances: Set<Instance>, ordinal: Double, now: ExactTimeStamp.Local) {
        ordinals[Key(instances)] = Value(ordinal, now)
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

    // todo ordinal make list of selectors to avoid repetitive let { return }
    fun getOrdinal(instances: Set<Instance>): Double {
        val key = Key(instances)

        // instanceKey (taskKey + customTime/hourMinute)
        getMatchByAspect(key) { it.instanceKey }?.let { return it }

        // instance dateTimePair
        getMatchByAspect(key) { it.instanceDateTimePair }?.let { return it }

        // instance Timestamp
        getMatchByAspect(key) {
            it.instanceDateTimePair.run { TimeStamp(date, project.getTime(timePair).getHourMinute(date.dayOfWeek)) }
        }?.let { return it }

        // instance timePair
        getMatchByAspect(key) { it.instanceDateTimePair.timePair }?.let { return it }

        // instance hourMinute
        getMatchByAspect(key) {
            it.instanceDateTimePair.run { project.getTime(timePair).getHourMinute(date.dayOfWeek) }
        }?.let { return it }

        // taskKey
        getMatchByAspect(key) { it.instanceKey.taskKey }?.let { return it }

        return project.projectKey.getOrdinal()
    }

    private data class Key(val entries: Set<Entry>) {

        companion object {

            operator fun invoke(instances: Set<Instance>) = Key(instances.map(::Entry).toSet())
        }

        data class Entry(val instanceKey: InstanceKey, val instanceDateTimePair: DateTimePair) {

            constructor(instance: Instance) : this(instance.instanceKey, instance.instanceDateTime.toDateTimePair())
        }
    }

    private data class Value(val ordinal: Double, val updated: ExactTimeStamp.Local)
}