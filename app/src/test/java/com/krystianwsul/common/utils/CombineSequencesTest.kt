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
}