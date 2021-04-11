package com.krystianwsul.common.time

import com.krystianwsul.common.utils.ProjectType

data class TimeDescriptor(val customTimeDescriptor: String?, val hour: Int?, val minute: Int?) {

    init {
        if (customTimeDescriptor == null) {
            checkNotNull(hour)
            checkNotNull(minute)
        } else {
            check(hour == null)
            check(minute == null)
        }
    }

    companion object {

        fun fromJsonTime(jsonTime: JsonTime): TimeDescriptor {
            return when (jsonTime) {
                is JsonTime.Custom -> TimeDescriptor(jsonTime.toJson(), null, null)
                is JsonTime.Normal -> TimeDescriptor(
                        null,
                        jsonTime.hourMinute.hour,
                        jsonTime.hourMinute.minute,
                )
            }
        }
    }

    fun <T : ProjectType> toJsonTime(projectIdProvider: JsonTime.ProjectIdProvider<T>): JsonTime {
        return if (customTimeDescriptor != null) {
            JsonTime.Custom.fromJson(projectIdProvider, customTimeDescriptor)
        } else {
            JsonTime.Normal(HourMinute(hour!!, minute!!))
        }
    }
}