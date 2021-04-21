package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time

// todo I can't think of a good reason why this couldn't be implemented on the task record itself, but whatever
sealed class CopyScheduleHelper {

    companion object {

        private fun Set<String>.toMap() = associate { it to true }

        private fun Time.destructure(): DestructuredTime {
            val jsonTime = JsonTime.fromTime(this)
            val projectCustomTimeId = (jsonTime as? JsonTime.Custom.Project)?.id?.value
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
    ): SingleScheduleJson

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
    ): WeeklyScheduleJson

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
    ): MonthlyDayScheduleJson

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
    ): MonthlyWeekScheduleJson

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
    ): YearlyScheduleJson

    object Private : CopyScheduleHelper() {

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
        ): SingleScheduleJson {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure()

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
        ): WeeklyScheduleJson {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure()

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
        ): MonthlyDayScheduleJson {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure()

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
        ): MonthlyWeekScheduleJson {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure()

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
        ): YearlyScheduleJson {
            check(assignedTo.isEmpty())

            val destructuredTime = copiedTime.destructure()

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

    object Shared : CopyScheduleHelper() {

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
            val destructuredTime = copiedTime.destructure()

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
        ): WeeklyScheduleJson {
            val destructuredTime = copiedTime.destructure()

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
        ): MonthlyDayScheduleJson {
            val destructuredTime = copiedTime.destructure()

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
        ): MonthlyWeekScheduleJson {
            val destructuredTime = copiedTime.destructure()

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
        ): YearlyScheduleJson {
            val destructuredTime = copiedTime.destructure()

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

    object Root : CopyScheduleHelper() {

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
        ): RootSingleScheduleJson {
            val destructuredTime = copiedTime.destructure()

            return RootSingleScheduleJson(
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
        ): WeeklyScheduleJson {
            val destructuredTime = copiedTime.destructure()

            return RootWeeklyScheduleJson(
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
        ): MonthlyDayScheduleJson {
            val destructuredTime = copiedTime.destructure()

            return RootMonthlyDayScheduleJson(
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
        ): MonthlyWeekScheduleJson {
            val destructuredTime = copiedTime.destructure()

            return RootMonthlyWeekScheduleJson(
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
        ): YearlyScheduleJson {
            val destructuredTime = copiedTime.destructure()

            return RootYearlyScheduleJson(
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