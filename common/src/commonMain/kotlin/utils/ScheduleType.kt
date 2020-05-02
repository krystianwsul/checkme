package com.krystianwsul.common.utils

enum class ScheduleType {

    SINGLE {

        override val hasDate = true
    },

    WEEKLY,

    MONTHLY_DAY {

        override val isMonthly = true
    },

    MONTHLY_WEEK {

        override val isMonthly = true
    },

    YEARLY {

        override val hasDate = true
    };

    open val isMonthly = false
    open val hasDate = false
}
