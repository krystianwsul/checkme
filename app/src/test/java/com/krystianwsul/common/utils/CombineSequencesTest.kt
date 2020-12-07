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
        val sequence1 = sequenceOf(1 to "a", 2 to "b", 3 to "c", 4 to "d", 5 to "e", 6 to "f", 7 to "g")
        val sequence2 = sequenceOf(2 to "a", 4 to "b", 6 to "c")
        val sequence3 = sequenceOf(3 to "a", 6 to "b")

        val result = combineSequencesGrouping(listOf(sequence1, sequence2, sequence3)) {
            val next = it.filterNotNull().minOrNull()

            it.mapIndexed { index, value -> index to value }
                    .filter { next == it.second }
                    .map { it.first }
        }

        assertEquals(
                listOf(
                        1 to listOf("a"),
                        2 to listOf("b", "a"),
                        3 to listOf("c", "a"),
                        4 to listOf("d", "b"),
                        5 to listOf("e"),
                        6 to listOf("f", "c", "b"),
                        7 to listOf("g")
                ),
                result.toList()
        )
    }
}