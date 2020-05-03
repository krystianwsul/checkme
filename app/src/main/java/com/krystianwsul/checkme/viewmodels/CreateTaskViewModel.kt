package com.krystianwsul.checkme.viewmodels

import android.content.Context
import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.tasks.ScheduleDialogFragment
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskActivity
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import com.soywiz.klock.Month
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.util.*

class CreateTaskViewModel : DomainViewModel<CreateTaskViewModel.Data>() {

    private var taskKey: TaskKey? = null
    private var joinTaskKeys: List<TaskKey>? = null
    private var parentTaskKeyHint: TaskKey? = null

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getCreateTaskData(
                taskKey,
                joinTaskKeys,
                parentTaskKeyHint
        )
    }

    fun start(taskKey: TaskKey?, joinTaskKeys: List<TaskKey>?, parentTaskKeyHint: TaskKey?) {
        this.taskKey = taskKey
        this.joinTaskKeys = joinTaskKeys
        this.parentTaskKeyHint = parentTaskKeyHint

        internalStart()
    }

    sealed class ScheduleDataWrapper : Serializable {

        companion object {

            fun fromScheduleData(scheduleData: ScheduleData) = when (scheduleData) {
                is ScheduleData.Single -> Single(scheduleData)
                is ScheduleData.Weekly -> Weekly(scheduleData)
                is ScheduleData.MonthlyDay -> MonthlyDay(scheduleData)
                is ScheduleData.MonthlyWeek -> MonthlyWeek(scheduleData)
                is ScheduleData.Yearly -> Yearly(scheduleData)
            }

            private fun timePairCallback(
                    timePair: TimePair,
                    customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
                    dayOfWeek: DayOfWeek? = null
            ): String {
                return timePair.customTimeKey?.let {
                    val customTimeData = customTimeDatas.getValue(timePair.customTimeKey!!)

                    customTimeData.name + (dayOfWeek?.let { " (" + customTimeData.hourMinutes[it] + ")" }
                            ?: "")
                } ?: timePair.hourMinute!!.toString()
            }
        }

        abstract val scheduleData: ScheduleData

        val timePair get() = scheduleData.timePair

        abstract fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String

        abstract fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData

        data class Single(override val scheduleData: ScheduleData.Single) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Single.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas, scheduleData.date.dayOfWeek)
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var monthDayNumber = scheduleData.date.day
                var beginningOfMonth = true
                if (monthDayNumber > 28) {
                    monthDayNumber = Month(scheduleData.date.month).days(scheduleData.date.year) - monthDayNumber + 1
                    beginningOfMonth = false
                }
                val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogFragment.ScheduleDialogData(
                        scheduleData.date,
                        mutableSetOf(scheduleData.date.dayOfWeek),
                        true,
                        true,
                        monthDayNumber,
                        monthWeekNumber,
                        scheduleData.date.dayOfWeek,
                        beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.SINGLE,
                        null,
                        null
                )
            }
        }

        data class Weekly(override val scheduleData: ScheduleData.Weekly) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Weekly.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                val date = scheduleHint?.date ?: today

                var monthDayNumber = date.day
                var beginningOfMonth = true
                if (monthDayNumber > 28) {
                    monthDayNumber = Month(date.month).days(date.year) - monthDayNumber + 1
                    beginningOfMonth = false
                }
                val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

                val days = scheduleData.daysOfWeek.toMutableSet()

                return ScheduleDialogFragment.ScheduleDialogData(
                        date,
                        days,
                        days == DayOfWeek.set,
                        true,
                        monthDayNumber,
                        monthWeekNumber,
                        date.dayOfWeek,
                        beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.WEEKLY,
                        scheduleData.from,
                        scheduleData.until
                )
            }
        }

        data class MonthlyDay(override val scheduleData: ScheduleData.MonthlyDay) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.MonthlyDay.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var date = scheduleHint?.date ?: today

                date = getDateInMonth(date.year, date.month, scheduleData.dayOfMonth, scheduleData.beginningOfMonth)

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogFragment.ScheduleDialogData(
                        date,
                        mutableSetOf(date.dayOfWeek),
                        true,
                        true,
                        scheduleData.dayOfMonth,
                        (scheduleData.dayOfMonth - 1) / 7 + 1,
                        date.dayOfWeek,
                        scheduleData.beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.MONTHLY_DAY,
                        scheduleData.from,
                        scheduleData.until
                )
            }
        }

        data class MonthlyWeek(override val scheduleData: ScheduleData.MonthlyWeek) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.MonthlyWeek.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var date = scheduleHint?.date ?: today

                date = getDateInMonth(date.year, date.month, scheduleData.dayOfMonth, scheduleData.dayOfWeek, scheduleData.beginningOfMonth)

                val dayNumber = if (scheduleData.beginningOfMonth)
                    date.day
                else
                    Month(date.month).days(date.year) - date.day + 1

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogFragment.ScheduleDialogData(
                        date,
                        mutableSetOf(scheduleData.dayOfWeek),
                        true,
                        false,
                        listOf(dayNumber, ScheduleDialogFragment.ScheduleDialogData.MAX_MONTH_DAY).min()!!,
                        scheduleData.dayOfMonth,
                        scheduleData.dayOfWeek,
                        scheduleData.beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.MONTHLY_WEEK,
                        scheduleData.from,
                        scheduleData.until
                )
            }
        }

        data class Yearly(override val scheduleData: ScheduleData.Yearly) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Yearly.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var date = scheduleHint?.date ?: today

                date = getDateInMonth(date.year, scheduleData.month, scheduleData.day, true)

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogFragment.ScheduleDialogData(
                        date,
                        mutableSetOf(date.dayOfWeek),
                        true,
                        true,
                        scheduleData.day,
                        (scheduleData.day - 1) / 7 + 1,
                        date.dayOfWeek,
                        true,
                        TimePairPersist(timePair),
                        ScheduleType.YEARLY,
                        scheduleData.from,
                        scheduleData.until
                )
            }
        }
    }

    data class Data(
            val taskData: TaskData?,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
            val defaultReminder: Boolean
    ) : DomainData()

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey<*>,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>
    )

    data class TaskData(
            val name: String,
            val parentKey: ParentKey?,
            val scheduleDataWrappers: List<ScheduleDataWrapper>?,
            val note: String?,
            val projectName: String?,
            val imageState: ImageState?
    )

    data class ParentTreeData(
            val name: String,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val parentKey: ParentKey,
            val scheduleText: String?,
            val note: String?,
            val sortKey: SortKey,
            val projectId: ProjectKey.Shared?
    ) {

        fun matchesSearch(query: String?): Boolean {
            if (query.isNullOrEmpty())
                return true

            if (name.toLowerCase(Locale.getDefault()).contains(query))
                return true

            if (note?.toLowerCase(Locale.getDefault())?.contains(query) == true)
                return true

            return parentTreeDatas.values.any { it.matchesSearch(query) }
        }
    }

    sealed class ParentKey : Parcelable {

        @Parcelize
        data class Project(val projectId: ProjectKey.Shared) : ParentKey()

        @Parcelize
        data class Task(val taskKey: TaskKey) : ParentKey()
    }

    sealed class SortKey : Comparable<SortKey> {

        data class ProjectSortKey(private val projectId: ProjectKey.Shared) : SortKey() {

            override fun compareTo(other: SortKey): Int {
                if (other is TaskSortKey)
                    return 1

                val projectSortKey = other as ProjectSortKey

                return projectId.compareTo(projectSortKey.projectId)
            }
        }

        data class TaskSortKey(private val startExactTimeStamp: ExactTimeStamp) : SortKey() {

            override fun compareTo(other: SortKey): Int {
                if (other is ProjectSortKey)
                    return -1

                val taskSortKey = other as TaskSortKey

                return startExactTimeStamp.compareTo(taskSortKey.startExactTimeStamp)
            }
        }
    }
}