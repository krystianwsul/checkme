package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectKey

// todo I can't think of a good reason why this couldn't be implemented on the task record itself, but whatever
sealed class CopyScheduleHelper {

    companion object {

        private fun Set<String>.toAssociateMap() = associate { it to true }

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
        assignedTo: Set<String>,
        copiedTime: Time,
        projectKey: ProjectKey<*>,
    ): SingleScheduleJson

    abstract fun newWeekly(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        dayOfWeek: Int,
        from: String?,
        until: String?,
        interval: Int,
        assignedTo: Set<String>,
        copiedTime: Time,
        projectKey: ProjectKey<*>,
    ): WeeklyScheduleJson

    abstract fun newMonthlyDay(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        dayOfMonth: Int,
        beginningOfMonth: Boolean,
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        copiedTime: Time,
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
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        copiedTime: Time,
        projectKey: ProjectKey<*>,
    ): MonthlyWeekScheduleJson

    abstract fun newYearly(
        startTime: Long,
        startTimeOffset: Double,
        endTime: Long?,
        endTimeOffset: Double?,
        month: Int,
        day: Int,
        from: String?,
        until: String?,
        assignedTo: Set<String>,
        copiedTime: Time,
        projectKey: ProjectKey<*>,
    ): YearlyScheduleJson

    object Root : CopyScheduleHelper() {

        override fun newSingle(
            startTime: Long,
            startTimeOffset: Double,
            endTime: Long?,
            endTimeOffset: Double?,
            year: Int,
            month: Int,
            day: Int,
            assignedTo: Set<String>,
            copiedTime: Time,
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
                assignedTo.toAssociateMap(),
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
            from: String?,
            until: String?,
            interval: Int,
            assignedTo: Set<String>,
            copiedTime: Time,
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
                assignedTo = assignedTo.toAssociateMap(),
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
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            copiedTime: Time,
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
                assignedTo = assignedTo.toAssociateMap(),
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
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            copiedTime: Time,
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
                assignedTo = assignedTo.toAssociateMap(),
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
            from: String?,
            until: String?,
            assignedTo: Set<String>,
            copiedTime: Time,
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
                assignedTo = assignedTo.toAssociateMap(),
                time = JsonTime.fromTime(copiedTime).toJson(),
                projectId = projectKey.key,
            )
        }
    }
}