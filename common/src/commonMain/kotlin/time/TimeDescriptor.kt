package com.krystianwsul.common.time

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

    fun toJsonTime(projectCustomTimeIdProvider: JsonTime.ProjectCustomTimeIdProvider): JsonTime {
        return if (customTimeDescriptor != null) {
            JsonTime.Custom.fromJson(projectCustomTimeIdProvider, customTimeDescriptor)
        } else {
            JsonTime.Normal(HourMinute(hour!!, minute!!))
        }
    }
}