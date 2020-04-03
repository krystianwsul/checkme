package com.krystianwsul.common.utils

import com.krystianwsul.common.time.ExactTimeStamp

interface Current {

    val startExactTimeStamp: ExactTimeStamp
    val endExactTimeStamp: ExactTimeStamp?

    fun notDeleted(exactTimeStamp: ExactTimeStamp) = endExactTimeStamp?.let { it > exactTimeStamp } != false

    fun current(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp && notDeleted(exactTimeStamp)
}