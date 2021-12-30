package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

class ProjectOrdinalManager(private val project: SharedProject) {

    private var ordinals = mutableMapOf<Key, Value>()

    fun setOrdinal(instances: Set<Instance>, ordinal: Double, now: ExactTimeStamp.Local) {
        ordinals[Key(instances)] = Value(ordinal, now)
    }

    private fun <T> getMatchByAspect(searchKey: Key, aspectSelector: (Key.Entry) -> T): Double? {
        fun Key.toMatchElements() = entries.map(aspectSelector).toSet()

        val inputMatchElements = searchKey.toMatchElements()

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

    // todo ordinal add info about instanceDateTime
    fun getOrdinal(instances: Set<Instance>): Double {
        getMatchByAspect(Key(instances)) { it.instanceKey }?.let { return it }
        getMatchByAspect(Key(instances)) { it.instanceKey.taskKey }?.let { return it }

        // match those that contain the most instances with the exact same DateTime

        return project.projectKey.getOrdinal()
    }

    private data class Key(val entries: Set<Entry>) {

        companion object {

            operator fun invoke(instances: Set<Instance>) = Key(instances.map(::Entry).toSet())
        }

        data class Entry(val instanceKey: InstanceKey) {

            constructor(instance: Instance) : this(instance.instanceKey)
        }
    }

    private data class Value(val ordinal: Double, val updated: ExactTimeStamp.Local)
}