package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectKey

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
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        year: Int,
        month: Int,
        day: Int,
        copiedTime: Time,
        assignedTo: Set<String>,
        projectKey: ProjectKey<*>,
    ): SingleScheduleJson

    abstract fun newWeekly(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        dayOfWeek: Int,
        copiedTime: Time,
        from: String?,
        until: String?,
        interval: Int,
        assignedTo: Set<String>,
        projectKey: ProjectKey<*>,
    ): WeeklyScheduleJson

    abstract fun newMonthlyDay(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        dayOfMonth: Int,
        beginningOfMonth: Boolean,
        copiedTime: Time,
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        projectKey: ProjectKey<*>,
    ): MonthlyDayScheduleJson

    abstract fun newMonthlyWeek(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        dayOfMonth: Int,
        dayOfWeek: Int,
        beginningOfMonth: Boolean,
        copiedTime: Time,
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        projectKey: ProjectKey<*>,
    ): MonthlyWeekScheduleJson

    abstract fun newYearly(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        month: Int,
        day: Int,
        copiedTime: Time,
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        projectKey: ProjectKey<*>,
    ): YearlyScheduleJson

    object Private : CopyScheduleHelper() {

        override fun newSingle(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            copiedTime: Time,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfWeek: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            interval: Int,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            dayOfWeek: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            month: Int,
            day: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            copiedTime: Time,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfWeek: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            interval: Int,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            dayOfWeek: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            month: Int,
            day: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
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
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            copiedTime: Time,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
        ): RootSingleScheduleJson {
            return RootSingleScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                year,
                month,
                day,
                assignedTo.toMap(),
                JsonTime.fromTime(copiedTime).toJson(),
                projectKey.key,
            )
        }

        override fun newWeekly(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfWeek: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            interval: Int,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
        ): WeeklyScheduleJson {
            return RootWeeklyScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                dayOfWeek,
                from,
                until,
                interval,
                assignedTo = assignedTo.toMap(),
                time = JsonTime.fromTime(copiedTime).toJson(),
                projectId = projectKey.key,
            )
        }

        override fun newMonthlyDay(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
        ): MonthlyDayScheduleJson {
            return RootMonthlyDayScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                dayOfMonth,
                beginningOfMonth,
                from,
                until,
                assignedTo = assignedTo.toMap(),
                time = JsonTime.fromTime(copiedTime).toJson(),
                projectId = projectKey.key,
            )
        }

        override fun newMonthlyWeek(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            dayOfMonth: Int,
            dayOfWeek: Int,
            beginningOfMonth: Boolean,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
        ): MonthlyWeekScheduleJson {
            return RootMonthlyWeekScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                dayOfMonth,
                dayOfWeek,
                beginningOfMonth,
                from,
                until,
                assignedTo = assignedTo.toMap(),
                time = JsonTime.fromTime(copiedTime).toJson(),
                projectId = projectKey.key,
            )
        }

        override fun newYearly(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            month: Int,
            day: Int,
            copiedTime: Time,
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            projectKey: ProjectKey<*>,
        ): YearlyScheduleJson {
            return RootYearlyScheduleJson(
                startTime,
                startTimeOffset,
                endTime,
                endTimeOffset,
                month,
                day,
                from,
                until,
                assignedTo = assignedTo.toMap(),
                time = JsonTime.fromTime(copiedTime).toJson(),
                projectId = projectKey.key,
            )
        }
    }
}