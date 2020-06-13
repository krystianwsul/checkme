package com.krystianwsul.common.utils

enum class ScheduleType {

    SINGLE,

    WEEKLY,

    MONTHLY_DAY {

        override val isMonthly = true
    },

    MONTHLY_WEEK {

        override val isMonthly = true
    },

    YEARLY;

    open val isMonthly = false
}
