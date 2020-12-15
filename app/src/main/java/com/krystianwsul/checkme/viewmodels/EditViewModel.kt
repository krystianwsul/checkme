package com.krystianwsul.checkme.viewmodels

import android.content.Context
import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskData
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.common.criteria.QueryMatch
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import com.soywiz.klock.Month
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.*

class EditViewModel : DomainViewModel<EditViewModel.Data>() {

    private lateinit var startParameters: StartParameters
    private var parentTaskKeyHint: TaskKey? = null

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getCreateTaskData(
                startParameters,
                parentTaskKeyHint
        )
    }

    fun start(
            startParameters: StartParameters = StartParameters.Create,
            parentTaskKeyHint: TaskKey? = null
    ) {
        this.startParameters = startParameters
        this.parentTaskKeyHint = parentTaskKeyHint

        internalStart()
    }

    fun start(taskKey: TaskKey) = start(StartParameters.Task(taskKey))

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

            fun dayFromEndOfMonth(date: Date) = Month(date.month).days(date.year) - date.day + 1

            fun dateToDayFromBeginningOrEnd(date: Date): Pair<Int, Boolean> {
                return if (date.day > ScheduleDialogData.MAX_MONTH_DAY) {
                    dayFromEndOfMonth(date) to false
                } else {
                    date.day to true
                }
            }

            fun dayOfMonthToWeekOfMonth(day: Int) = (day - 1) / 7 + 1
        }

        abstract val scheduleData: ScheduleData

        val timePair get() = scheduleData.timePair

        abstract fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String

        fun getScheduleDialogData(scheduleHint: EditActivity.Hint.Schedule?) =
                getScheduleDialogDataHelper(scheduleHint?.date ?: Date.today())

        protected abstract fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData

        data class Single(override val scheduleData: ScheduleData.Single) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Single.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas, scheduleData.date.dayOfWeek)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(scheduleData.date)

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogData(
                        scheduleData.date,
                        mutableSetOf(scheduleData.date.dayOfWeek),
                        true,
                        monthDayNumber,
                        dayOfMonthToWeekOfMonth(monthDayNumber),
                        scheduleData.date.dayOfWeek,
                        beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.SINGLE,
                        null,
                        null,
                        1
                )
            }
        }

        data class Weekly(override val scheduleData: ScheduleData.Weekly) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Weekly.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(suggestedDate)

                val days = scheduleData.daysOfWeek.toMutableSet()

                return ScheduleDialogData(
                        suggestedDate,
                        days,
                        true,
                        monthDayNumber,
                        dayOfMonthToWeekOfMonth(monthDayNumber),
                        suggestedDate.dayOfWeek,
                        beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.WEEKLY,
                        scheduleData.from,
                        scheduleData.until,
                        scheduleData.interval
                )
            }
        }

        data class MonthlyDay(override val scheduleData: ScheduleData.MonthlyDay) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.MonthlyDay.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val date = getDateInMonth(
                        suggestedDate.year,
                        suggestedDate.month,
                        scheduleData.dayOfMonth,
                        scheduleData.beginningOfMonth
                )

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogData(
                        date,
                        mutableSetOf(date.dayOfWeek),
                        true,
                        scheduleData.dayOfMonth,
                        dayOfMonthToWeekOfMonth(scheduleData.dayOfMonth),
                        date.dayOfWeek,
                        scheduleData.beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.MONTHLY_DAY,
                        scheduleData.from,
                        scheduleData.until,
                        1
                )
            }
        }

        data class MonthlyWeek(override val scheduleData: ScheduleData.MonthlyWeek) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.MonthlyWeek.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val date = getDateInMonth(
                        suggestedDate.year,
                        suggestedDate.month,
                        scheduleData.weekOfMonth,
                        scheduleData.dayOfWeek,
                        scheduleData.beginningOfMonth
                )

                val dayNumber = if (scheduleData.beginningOfMonth)
                    date.day
                else
                    dayFromEndOfMonth(date)

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogData(
                        date,
                        mutableSetOf(scheduleData.dayOfWeek),
                        false,
                        listOf(dayNumber, ScheduleDialogData.MAX_MONTH_DAY).minOrNull()!!,
                        scheduleData.weekOfMonth,
                        scheduleData.dayOfWeek,
                        scheduleData.beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.MONTHLY_WEEK,
                        scheduleData.from,
                        scheduleData.until,
                        1
                )
            }
        }

        data class Yearly(override val scheduleData: ScheduleData.Yearly) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return ScheduleText.Yearly.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val date = getDateInMonth(
                        suggestedDate.year,
                        scheduleData.month,
                        scheduleData.day,
                        true
                )

                val (monthDayNumber, beginningOfMonth) = dateToDayFromBeginningOrEnd(date)

                @Suppress("BooleanLiteralArgument")
                return ScheduleDialogData(
                        date,
                        mutableSetOf(date.dayOfWeek),
                        true,
                        monthDayNumber,
                        dayOfMonthToWeekOfMonth(monthDayNumber),
                        date.dayOfWeek,
                        beginningOfMonth,
                        TimePairPersist(timePair),
                        ScheduleType.YEARLY,
                        scheduleData.from,
                        scheduleData.until,
                        1
                )
            }
        }
    }

    data class Data(
            val taskData: TaskData?,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
            val defaultReminder: Boolean,
            val showAllInstancesDialog: Boolean
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
            val imageState: ImageState?,
            val assignedTo: Set<UserKey>,
    )

    data class ParentTreeData(
            val name: String,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val parentKey: ParentKey,
            val scheduleText: String?,
            val note: String?,
            val sortKey: SortKey,
            val projectId: ProjectKey.Shared?,
            val isRootTaskGroup: Boolean,
            val projectUsers: Map<UserKey, UserData>,
    ) : QueryMatch {

        override val normalizedName by lazy { name.normalized() }
        override val normalizedNote by lazy { note?.normalized() }

        fun normalize() {
            normalizedName
            normalizedNote
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

        data class TaskSortKey(private val startExactTimeStamp: ExactTimeStamp.Local) : SortKey() {

            override fun compareTo(other: SortKey): Int {
                if (other is ProjectSortKey)
                    return -1

                val taskSortKey = other as TaskSortKey

                return startExactTimeStamp.compareTo(taskSortKey.startExactTimeStamp)
            }
        }
    }

    sealed class StartParameters {

        abstract val excludedTaskKeys: Set<TaskKey>

        object Create : StartParameters() {

            override val excludedTaskKeys = setOf<TaskKey>()
        }

        class Task(val taskKey: TaskKey) : StartParameters() {

            override val excludedTaskKeys = setOf(taskKey)
        }

        class Join(val joinTaskKeys: List<TaskKey>) : StartParameters() {

            override val excludedTaskKeys = joinTaskKeys.toSet()
        }
    }

    @Parcelize
    data class UserData(val key: UserKey, val name: String, val photoUrl: String?) : Parcelable
}