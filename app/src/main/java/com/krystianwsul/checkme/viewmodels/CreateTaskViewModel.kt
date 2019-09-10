package com.krystianwsul.checkme.viewmodels

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.models.ImageState
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ScheduleDialogFragment
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.util.*

class CreateTaskViewModel : DomainViewModel<CreateTaskViewModel.Data>() {

    private var taskKey: TaskKey? = null
    private var joinTaskKeys: List<TaskKey>? = null
    private var parentTaskKeyHint: TaskKey? = null

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getCreateTaskData(taskKey, joinTaskKeys, parentTaskKeyHint)
    }

    fun start(taskKey: TaskKey?, joinTaskKeys: List<TaskKey>?, parentTaskKeyHint: TaskKey?) {
        this.taskKey = taskKey
        this.joinTaskKeys = joinTaskKeys
        this.parentTaskKeyHint = parentTaskKeyHint

        internalStart()
    }

    sealed class ScheduleDataWrapper : Serializable {

        abstract val scheduleData: ScheduleData

        val timePair get() = scheduleData.timePair

        abstract fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String

        abstract fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData

        data class Single(override val scheduleData: ScheduleData.Single) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return scheduleData.date.getDisplayText() + ", " + if (timePair.customTimeKey != null) {
                    check(timePair.hourMinute == null)

                    val customTimeData = customTimeDatas.getValue(timePair.customTimeKey!!)

                    customTimeData.name + " (" + customTimeData.hourMinutes[scheduleData.date.dayOfWeek] + ")"
                } else {
                    timePair.hourMinute!!.toString()
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var monthDayNumber = scheduleData.date.day
                var beginningOfMonth = true
                if (monthDayNumber > 28) {
                    monthDayNumber = Utils.getDaysInMonth(scheduleData.date.year, scheduleData.date.month) - monthDayNumber + 1
                    beginningOfMonth = false
                }
                val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

                return ScheduleDialogFragment.ScheduleDialogData(scheduleData.date, hashSetOf(scheduleData.date.dayOfWeek), true, monthDayNumber, monthWeekNumber, scheduleData.date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.SINGLE)
            }
        }

        data class Weekly(override val scheduleData: ScheduleData.Weekly) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return scheduleData.daysOfWeek.prettyPrint() + if (timePair.customTimeKey != null) {
                    check(timePair.hourMinute == null)

                    customTimeDatas.getValue(timePair.customTimeKey!!).name
                } else {
                    timePair.hourMinute!!.toString()
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                val date = scheduleHint?.date ?: today

                var monthDayNumber = date.day
                var beginningOfMonth = true
                if (monthDayNumber > 28) {
                    monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
                    beginningOfMonth = false
                }
                val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

                return ScheduleDialogFragment.ScheduleDialogData(date, scheduleData.daysOfWeek.toHashSet(), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.WEEKLY)
            }
        }

        data class MonthlyDay(override val scheduleData: ScheduleData.MonthlyDay) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                val day = Utils.ordinal(scheduleData.dayOfMonth) + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

                return "$day, " + if (timePair.customTimeKey != null) {
                    check(timePair.hourMinute == null)

                    val customTimeData = customTimeDatas.getValue(timePair.customTimeKey!!)

                    customTimeData.name
                } else {
                    timePair.hourMinute!!.toString()
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var date = scheduleHint?.date ?: today

                date = Utils.getDateInMonth(date.year, date.month, scheduleData.dayOfMonth, scheduleData.beginningOfMonth)

                return ScheduleDialogFragment.ScheduleDialogData(date, hashSetOf(date.dayOfWeek), true, scheduleData.dayOfMonth, (scheduleData.dayOfMonth - 1) / 7 + 1, date.dayOfWeek, scheduleData.beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_DAY)
            }
        }

        data class MonthlyWeek(override val scheduleData: ScheduleData.MonthlyWeek) : ScheduleDataWrapper() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                val day = Utils.ordinal(scheduleData.dayOfMonth) + " " + scheduleData.dayOfWeek + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (scheduleData.beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

                return "$day, " + if (timePair.customTimeKey != null) {
                    check(timePair.hourMinute == null)

                    val customTimeData = customTimeDatas.getValue(timePair.customTimeKey!!)

                    customTimeData.name
                } else {
                    timePair.hourMinute!!.toString()
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var date = scheduleHint?.date ?: today

                date = Utils.getDateInMonth(date.year, date.month, scheduleData.dayOfMonth, scheduleData.dayOfWeek, scheduleData.beginningOfMonth)

                return ScheduleDialogFragment.ScheduleDialogData(date, hashSetOf(scheduleData.dayOfWeek), false, date.day, scheduleData.dayOfMonth, scheduleData.dayOfWeek, scheduleData.beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_WEEK)
            }
        }
    }

    data class Data(
            val taskData: TaskData?,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>,
            val defaultReminder: Boolean) : DomainData()

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey<*>,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>)

    data class TaskData(
            val name: String,
            val parentKey: ParentKey?,
            val scheduleDataWrappers: List<ScheduleDataWrapper>?,
            val note: String?,
            val projectName: String?,
            val imageState: ImageState?)

    data class ParentTreeData(
            val name: String,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val parentKey: ParentKey,
            val scheduleText: String?,
            val note: String?,
            val sortKey: SortKey,
            val projectId: String?) {

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
        data class Project(val projectId: String) : ParentKey()

        @Parcelize
        data class Task(val taskKey: TaskKey) : ParentKey()
    }

    sealed class SortKey : Comparable<SortKey> {

        data class ProjectSortKey(private val projectId: String) : SortKey() {

            init {
                check(!TextUtils.isEmpty(projectId))
            }

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