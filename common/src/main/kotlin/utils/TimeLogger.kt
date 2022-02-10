package com.krystianwsul.common.utils

import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.time.DateTimeSoy

object TimeLogger {

    private val times = mutableMapOf<String, Pair<Int, Long>>()

    fun clear() = times.clear()

    fun printToString() = times.entries
        .sortedByDescending { it.value.first }
        .joinToString("\n") { "called ${it.value.first} times: ${it.key}, ${it.value.second} ms" }

    fun sumExcluding(key: String) = times.filterKeys { it != key }
        .values
        .sumOf { it.second }

    fun print() {
        times.entries
            .sortedByDescending { it.value.first }
            .forEach { log("magic called ${it.value.first} times: ${it.key}, ${it.value.second} ms") }
    }

    fun start(key: String) = Tracker(key)

    fun startIfLogDone(key: String) = if (FeatureFlagManager.logDone) start(key) else null

    data class Tracker(val key: String, val id: Int = staticId++) {

        companion object {

            private var staticId = 0
        }

        private val start = DateTimeSoy.nowUnixLong()

        private var stopped = false

        fun stop(extra: String? = null) {
            check(!stopped)

            val key = extra?.let { "$key $it" } ?: key

            stopped = true

            val oldPair = times[key] ?: Pair(0, 0L)

            times[key] = Pair(
                    oldPair.first + 1,
                oldPair.second + (DateTimeSoy.nowUnixLong() - start)
            )
        }
    }
}