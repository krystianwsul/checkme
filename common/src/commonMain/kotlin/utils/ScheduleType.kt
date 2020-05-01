package com.krystianwsul.common.utils

enum class ScheduleType {
    SINGLE,
    DAILY,
    WEEKLY,
    MONTHLY_DAY {

        override val isMonthly = true
    },
    MONTHLY_WEEK {

        override val isMonthly = true
    };

    open val isMonthly = false
}
