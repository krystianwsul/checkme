package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.utils.ProjectType

sealed class CopyScheduleHelper<T : ProjectType> {

    abstract fun newSingle(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            customTimeId: String?,
            hour: Int?,
            minute: Int?,
            group: Boolean,
    ): SingleScheduleJson<T> // todo assign

    abstract fun newWeekly(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfWeek: Int,
            customTimeId: String?,
            hour: Int?,
            minute: Int?,
            from: String?,
            until: String?,
            interval: Int,
    ): WeeklyScheduleJson<T> // todo assign

    abstract fun newMonthlyDay(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            beginningOfMonth: Boolean,
            customTimeId: String?,
            hour: Int?,
            minute: Int?,
            from: String?,
            until: String?,
    ): MonthlyDayScheduleJson<T> // todo assign

    abstract fun newMonthlyWeek(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            dayOfWeek: Int,
            beginningOfMonth: Boolean,
            customTimeId: String?,
            hour: Int?,
            minute: Int?,
            from: String?,
            until: String?,
    ): MonthlyWeekScheduleJson<T> // todo assign

    abstract fun newYearly(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            month: Int,
            day: Int,
            customTimeId: String?,
            hour: Int?,
            minute: Int?,
            from: String?,
            until: String?,
    ): YearlyScheduleJson<T> // todo assign

    object Private : CopyScheduleHelper<ProjectType.Private>() {

        override fun newSingle(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                year: Int,
                month: Int,
                day: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                group: Boolean,
        ): SingleScheduleJson<ProjectType.Private> {
            return PrivateSingleScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    year,
                    month,
                    day,
                    customTimeId,
                    hour,
                    minute,
                    group
            )
        }

        override fun newWeekly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfWeek: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
                interval: Int,
        ): WeeklyScheduleJson<ProjectType.Private> {
            return PrivateWeeklyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfWeek,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until,
                    interval
            )
        }

        override fun newMonthlyDay(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                beginningOfMonth: Boolean,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): MonthlyDayScheduleJson<ProjectType.Private> {
            return PrivateMonthlyDayScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    beginningOfMonth,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }

        override fun newMonthlyWeek(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                dayOfWeek: Int,
                beginningOfMonth: Boolean,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): MonthlyWeekScheduleJson<ProjectType.Private> {
            return PrivateMonthlyWeekScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    dayOfWeek,
                    beginningOfMonth,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }

        override fun newYearly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                month: Int,
                day: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): YearlyScheduleJson<ProjectType.Private> {
            return PrivateYearlyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    month,
                    day,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }
    }

    object Shared : CopyScheduleHelper<ProjectType.Shared>() {

        override fun newSingle(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                year: Int,
                month: Int,
                day: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                group: Boolean,
        ) = SharedSingleScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                year,
                month,
                day,
                customTimeId,
                hour,
                minute,
                group
        )

        override fun newWeekly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfWeek: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
                interval: Int,
        ): WeeklyScheduleJson<ProjectType.Shared> {
            return SharedWeeklyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfWeek,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until,
                    interval
            )
        }

        override fun newMonthlyDay(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                beginningOfMonth: Boolean,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): MonthlyDayScheduleJson<ProjectType.Shared> {
            return SharedMonthlyDayScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    beginningOfMonth,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }

        override fun newMonthlyWeek(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                dayOfWeek: Int,
                beginningOfMonth: Boolean,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): MonthlyWeekScheduleJson<ProjectType.Shared> {
            return SharedMonthlyWeekScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    dayOfWeek,
                    beginningOfMonth,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }

        override fun newYearly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                month: Int,
                day: Int,
                customTimeId: String?,
                hour: Int?,
                minute: Int?,
                from: String?,
                until: String?,
        ): YearlyScheduleJson<ProjectType.Shared> {
            return SharedYearlyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    month,
                    day,
                    customTimeId,
                    hour,
                    minute,
                    from,
                    until
            )
        }
    }
}