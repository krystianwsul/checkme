package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

object TimeLogger {

    private val times = mutableMapOf<String, Pair<Int, Long>>()

    fun clear() = times.clear()

    fun print() {
        times.entries
                .sortedBy { it.key }
                .forEach { log("magic called ${it.value.first} times: ${it.key}, ${it.value.second} ms") }
    }

    fun start(key: String) = Tracker(key)

    data class Tracker(val key: String, val id: Int = staticId++) {

        companion object {

            private var staticId = 0
        }

        private val start = ExactTimeStamp.Local.now

        private var stopped = false

        fun stop(extra: String? = null) {
            check(!stopped)

            val key = extra?.let { "$key $it" } ?: key

            stopped = true

            val oldPair = times[key] ?: Pair(0, 0L)

            times[key] = Pair(
                    oldPair.first + 1,
                    oldPair.second + (ExactTimeStamp.Local.now.long - start.long)
            )
        }
    }
}