package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.utils.ProjectType

sealed class CopyScheduleHelper<T : ProjectType> {

    companion object {

        private fun Set<String>.toMap() = associate { it to true }
    }

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
            assignedTo: Set<String>,
    ): SingleScheduleJson<T>

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
            assignedTo: Set<String>,
    ): WeeklyScheduleJson<T>

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
            assignedTo: Set<String>,
    ): MonthlyDayScheduleJson<T>

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
            assignedTo: Set<String>,
    ): MonthlyWeekScheduleJson<T>

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
            assignedTo: Set<String>,
    ): YearlyScheduleJson<T>

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
                assignedTo: Set<String>,
        ): SingleScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

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
                assignedTo: Set<String>,
        ): WeeklyScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

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
                assignedTo: Set<String>,
        ): MonthlyDayScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

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
                assignedTo: Set<String>,
        ): MonthlyWeekScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

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
                assignedTo: Set<String>,
        ): YearlyScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

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
                assignedTo: Set<String>,
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
                assignedTo.toMap()
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
                assignedTo: Set<String>,
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
                    interval,
                    assignedTo = assignedTo.toMap(),
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
                assignedTo: Set<String>,
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
                    until,
                    assignedTo = assignedTo.toMap(),
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
                assignedTo: Set<String>,
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
                    until,
                    assignedTo = assignedTo.toMap(),
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
                assignedTo: Set<String>,
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
                    until,
                    assignedTo = assignedTo.toMap(),
            )
        }
    }
}