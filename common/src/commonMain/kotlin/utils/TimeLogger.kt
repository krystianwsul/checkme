package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

object TimeLogger { // todo search remove all calls

    private val times = mutableMapOf<String, Pair<Int, Long>>()

    fun clear() = times.clear()

    fun print() {
        times.entries
                .sortedBy { it.key }
                .forEach { log("time for ${it.key}, called ${it.value.first} times: ${it.value.second}") }
    }

    fun start(key: String) = Tracker(key)

    fun stop(tracker: Tracker) {
        val oldPair = times[tracker.key] ?: Pair(0, 0L)


        times[tracker.key] = Pair(
                oldPair.first + 1,
                oldPair.second + (ExactTimeStamp.now.long - tracker.start.long)
        )
    }

    data class Tracker(val key: String, val id: Int = staticId++) {

        companion object {

            private var staticId = 0
        }

        val start = ExactTimeStamp.now
    }
}