package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectOrdinalManagerTest {

    @Test
    fun testDateTimePairHourMinuteSerialization() {
        val inputDateTimePair = DateTimePair(Date(2022, 1, 2), TimePair(HourMinute(20, 55)))
        val dateTimePairJson = inputDateTimePair.toJson()
        val outputDateTimePair = DateTimePair.fromJson(mockk(), dateTimePairJson)

        assertEquals(inputDateTimePair, outputDateTimePair)
    }
}