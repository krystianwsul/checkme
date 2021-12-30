package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

class ProjectOrdinalManager(private val project: SharedProject) {

    private var ordinals = mutableMapOf<Key, Value>()

    fun setOrdinal(instanceKeys: Set<InstanceKey>, ordinal: Double, now: ExactTimeStamp.Local) {
        ordinals[Key(instanceKeys)] = Value(ordinal, now)
    }

    private fun <T> getMatchesByOverlap(searchKey: Key, selector: (InstanceKey) -> T): Double? {
        fun Key.toMatchElements() = instanceKeys.map(selector).toSet()

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
            ?.groupBy { it.matchElements.size }
            ?.minByOrNull { it.key } // find the least extra match elements
            ?.value
            ?.map { it.entry }
            ?.minByOrNull { it.key.instanceKeys.size } // find the smallest instance key count
            ?.value
            ?.ordinal
    }

    // todo ordinal add info about instanceDateTime
    fun getOrdinal(instanceKeys: Set<InstanceKey>): Double {
        getMatchesByOverlap(Key(instanceKeys)) { it }?.let { return it }
        getMatchesByOverlap(Key(instanceKeys)) { it.taskKey }?.let { return it }

        // match those that contain the most instances with the exact same DateTime

        return project.projectKey.getOrdinal()
    }

    private data class Key(val instanceKeys: Set<InstanceKey>)

    private data class Value(val ordinal: Double, val updated: ExactTimeStamp.Local)
}