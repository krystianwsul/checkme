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

    fun <T> log(key: String, action: () -> T): T {
        val tracker = start(key)

        val ret = action()

        tracker.stop()

        return ret
    }

    class Tracker(val key: String) {

        private val start = DateTimeSoy.nowUnixLong()

        private var stopped = false

        fun stop(extra: String? = null) {
            val diff = DateTimeSoy.nowUnixLong() - start

            check(!stopped)

            stopped = true

            val key = extra?.let { "$key $it" } ?: key

            val oldPair = times[key]

            times[key] = if (oldPair != null) {
                Pair(oldPair.first + 1, oldPair.second + diff)
            } else {
                Pair(1, diff)
            }
        }
    }
}