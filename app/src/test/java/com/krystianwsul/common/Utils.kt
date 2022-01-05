package com.krystianwsul.common

import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair

operator fun TimePair.Companion.invoke(hour: Int, minute: Int) = TimePair(HourMinute(hour, minute))