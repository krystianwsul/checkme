package com.krystianwsul.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OrdinalTest {

    @Test
    fun testToString() {
        val ordinal1 = Ordinal(1)
        val ordinal4 = Ordinal(4)
        val ordinal0dot25 = ordinal1 / ordinal4

        val ordinalString = ordinal0dot25.toString()
        assertEquals(ordinal0dot25, Ordinal.fromJson(ordinalString))
    }

    @Test
    fun testStringLengthFraction() {
        val ordinal0dot25 = Ordinal(1) / Ordinal(4)

        assertEquals("0.25", ordinal0dot25.toString())
    }

    @Test
    fun testStringLength3() {
        val ordinal = Ordinal(3)

        assertEquals("3", ordinal.toString())
    }

    @Test
    fun testStringLength30() {
        val ordinal = Ordinal(30)

        assertEquals("30", ordinal.toString())
    }
}