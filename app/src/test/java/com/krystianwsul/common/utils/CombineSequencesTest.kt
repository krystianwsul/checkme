package com.krystianwsul.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CombineSequencesTest {

    @Test
    fun testSequences() {
        val sequence1 = sequenceOf("a", "n", "r", "u")
        val sequence2 = sequenceOf("i", "q", "z")
        val sequence3 = sequenceOf("d", "e", "f", "g", "s")

        val result = combineSequences(listOf(sequence1, sequence2, sequence3)) {
            val next = it.filterNotNull().minOrNull()!!

            it.indexOf(next)
        }

        assertEquals(
                listOf("a", "d", "e", "f", "g", "i", "n", "q", "r", "s", "u", "z"),
                result.toList()
        )
    }

    @Test
    fun testGrouping() {
        val sequence1 = sequenceOf(1.0 to "a", 2.0 to "b", 3.0 to "c", 4.0 to "d", 5.0 to "e", 6.0 to "f", 7.0 to "g")
        val sequence2 = sequenceOf(2.0 to "a", 4.0 to "b", 6.0 to "c")
        val sequence3 = sequenceOf(3.0 to "a", 6.0 to "b")

        val result = combineSequencesGrouping(listOf(sequence1, sequence2, sequence3)) {
            val next = it.filterNotNull()
                    .minByOrNull { it.first }!!
                    .first

            it.mapIndexed { index, value -> index to value }
                    .filter { next == it.second?.first }
                    .map { it.first }
        }

        assertEquals(
                listOf(
                        listOf(1.0 to "a"),
                        listOf(2.0 to "b", 2.0 to "a"),
                        listOf(3.0 to "c", 3.0 to "a"),
                        listOf(4.0 to "d", 4.0 to "b"),
                        listOf(5.0 to "e"),
                        listOf(6.0 to "f", 6.0 to "c", 6.0 to "b"),
                        listOf(7.0 to "g")
                ),
                result.toList()
        )
    }
}