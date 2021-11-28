package com.krystianwsul.common

import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateSequenceTest {

    @Test
    fun testInitialValueImmediate() {
        var lastGeneratedValue = -1

        fun generateInitial(): Int {
            lastGeneratedValue = 0
            return 0
        }

        val sequence = generateSequence(generateInitial()) {
            val ret = it + 1

            lastGeneratedValue = ret

            ret
        }
        assertEquals(0, lastGeneratedValue)

        assertEquals(listOf(0, 1), sequence.take(2).toList())
        assertEquals(1, lastGeneratedValue)
    }

    @Test
    fun testInitialValueLazy() {
        var lastGeneratedValue = -1

        fun generateInitial(): Int {
            lastGeneratedValue = 0
            return 0
        }

        val sequence = generateSequence(::generateInitial) {
            val ret = it + 1

            lastGeneratedValue = ret

            ret
        }
        assertEquals(-1, lastGeneratedValue)

        assertEquals(listOf(0, 1), sequence.take(2).toList())
        assertEquals(1, lastGeneratedValue)
    }
}