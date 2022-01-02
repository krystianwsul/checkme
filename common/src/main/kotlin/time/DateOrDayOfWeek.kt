package com.krystianwsul.common.time

sealed class DateOrDayOfWeek {

    companion object {

        fun fromJson(json: String) = if (json.contains('-')) Date.fromJson(json) else DayOfWeek.fromJson(json)
    }

    abstract val date: com.krystianwsul.common.time.Date?
    abstract val dayOfWeek: com.krystianwsul.common.time.DayOfWeek

    abstract fun toJson(): String

    data class Date(override val date: com.krystianwsul.common.time.Date) : DateOrDayOfWeek() {

        companion object {

            fun fromJson(json: String) = Date(com.krystianwsul.common.time.Date.fromJson(json))
        }

        override val dayOfWeek get() = date.dayOfWeek

        override fun toJson() = date.toJson()
    }

    data class DayOfWeek(override val dayOfWeek: com.krystianwsul.common.time.DayOfWeek) : DateOrDayOfWeek() {

        companion object {

            fun fromJson(json: String) = DayOfWeek(com.krystianwsul.common.time.DayOfWeek.fromJson(json))
        }

        override val date: com.krystianwsul.common.time.Date? get() = null

        override fun toJson() = dayOfWeek.toJson()
    }
}