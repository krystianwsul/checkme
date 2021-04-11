package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectType

sealed class CopyScheduleHelper<T : ProjectType> {

    companion object {

        private fun Set<String>.toMap() = associate { it to true }

        private fun <T : ProjectType> Time.destructure(): DestructuredTime {
            val jsonTime = JsonTime.fromTime<T>(this)
            val projectCustomTimeId = (jsonTime as? JsonTime.Custom.Project<*>)?.id?.value
            val hourMinute = (jsonTime as? JsonTime.Normal)?.hourMinute

            return DestructuredTime(
                    projectCustomTimeId,
                    hourMinute?.hour,
                    hourMinute?.minute,
                    jsonTime.toJson(),
            )
        }
    }

    private data class DestructuredTime(
            val projectCustomTimeId: String?,
            val hour: Int?,
            val minute: Int?,
            val jsonTime: String,
    )

    abstract fun newSingle(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            copiedTime: Time,
            assignedTo: Set<String>,
    ): SingleScheduleJson<T>

    abstract fun newWeekly(
            startTime: Long,
            startTimeOffset: Double?,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfWeek: Int,
            copiedTime: Time,
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
            copiedTime: Time,
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
            copiedTime: Time,
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
            copiedTime: Time,
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
                copiedTime: Time,
                assignedTo: Set<String>,
        ): SingleScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure<ProjectType.Private>()

            return PrivateSingleScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    year,
                    month,
                    day,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    destructuredTime.jsonTime,
            )
        }

        override fun newWeekly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfWeek: Int,
                copiedTime: Time,
                from: String?,
                until: String?,
                interval: Int,
                assignedTo: Set<String>,
        ): WeeklyScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure<ProjectType.Private>()

            return PrivateWeeklyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfWeek,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    interval,
                    time = destructuredTime.jsonTime,
            )
        }

        override fun newMonthlyDay(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                beginningOfMonth: Boolean,
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): MonthlyDayScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure<ProjectType.Private>()

            return PrivateMonthlyDayScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    beginningOfMonth,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    time = destructuredTime.jsonTime,
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
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): MonthlyWeekScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure<ProjectType.Private>()

            return PrivateMonthlyWeekScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    dayOfWeek,
                    beginningOfMonth,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    time = destructuredTime.jsonTime,
            )
        }

        override fun newYearly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                month: Int,
                day: Int,
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): YearlyScheduleJson<ProjectType.Private> {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure<ProjectType.Private>()

            return PrivateYearlyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    month,
                    day,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    time = destructuredTime.jsonTime,
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
                copiedTime: Time,
                assignedTo: Set<String>,
        ): SharedSingleScheduleJson {
            val destructuredTime = copiedTime.destructure<ProjectType.Shared>()

            return SharedSingleScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    year,
                    month,
                    day,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    assignedTo.toMap(),
                    destructuredTime.jsonTime,
            )
        }

        override fun newWeekly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfWeek: Int,
                copiedTime: Time,
                from: String?,
                until: String?,
                interval: Int,
                assignedTo: Set<String>,
        ): WeeklyScheduleJson<ProjectType.Shared> {
            val destructuredTime = copiedTime.destructure<ProjectType.Shared>()

            return SharedWeeklyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfWeek,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    interval,
                    assignedTo = assignedTo.toMap(),
                    time = destructuredTime.jsonTime,
            )
        }

        override fun newMonthlyDay(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                dayOfMonth: Int,
                beginningOfMonth: Boolean,
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): MonthlyDayScheduleJson<ProjectType.Shared> {
            val destructuredTime = copiedTime.destructure<ProjectType.Shared>()

            return SharedMonthlyDayScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    beginningOfMonth,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    assignedTo = assignedTo.toMap(),
                    time = destructuredTime.jsonTime,
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
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): MonthlyWeekScheduleJson<ProjectType.Shared> {
            val destructuredTime = copiedTime.destructure<ProjectType.Shared>()

            return SharedMonthlyWeekScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    dayOfMonth,
                    dayOfWeek,
                    beginningOfMonth,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    assignedTo = assignedTo.toMap(),
                    time = destructuredTime.jsonTime,
            )
        }

        override fun newYearly(
                startTime: Long,
                startTimeOffset: Double?,
                endTime: Long?,
                endTimeOffset: Double?,
                month: Int,
                day: Int,
                copiedTime: Time,
                from: String?,
                until: String?,
                assignedTo: Set<String>,
        ): YearlyScheduleJson<ProjectType.Shared> {
            val destructuredTime = copiedTime.destructure<ProjectType.Shared>()

            return SharedYearlyScheduleJson(
                    startTime,
                    startTimeOffset,
                    endTime,
                    endTimeOffset,
                    month,
                    day,
                    destructuredTime.projectCustomTimeId,
                    destructuredTime.hour,
                    destructuredTime.minute,
                    from,
                    until,
                    assignedTo = assignedTo.toMap(),
                    time = destructuredTime.jsonTime,
            )
        }
    }
}