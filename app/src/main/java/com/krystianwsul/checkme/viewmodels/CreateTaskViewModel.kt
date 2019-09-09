package com.krystianwsul.checkme.viewmodels

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ScheduleDialogFragment
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.prettyPrint
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeKey
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

    sealed class ScheduleData : Serializable {

        abstract val timePair: TimePair

        abstract fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String

        abstract fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData

        data class Single(val date: Date, override val timePair: TimePair) : ScheduleData() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return date.getDisplayText() + ", " + if (timePair.customTimeKey != null) {
                    check(timePair.hourMinute == null)

                    val customTimeData = customTimeDatas.getValue(timePair.customTimeKey!!)

                    customTimeData.name + " (" + customTimeData.hourMinutes[date.dayOfWeek] + ")"
                } else {
                    timePair.hourMinute!!.toString()
                }
            }

            override fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData {
                var monthDayNumber = date.day
                var beginningOfMonth = true
                if (monthDayNumber > 28) {
                    monthDayNumber = Utils.getDaysInMonth(date.year, date.month) - monthDayNumber + 1
                    beginningOfMonth = false
                }
                val monthWeekNumber = (monthDayNumber - 1) / 7 + 1

                return ScheduleDialogFragment.ScheduleDialogData(date, hashSetOf(date.dayOfWeek), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.SINGLE)
            }
        }

        data class Weekly(val daysOfWeek: Set<DayOfWeek>, override val timePair: TimePair) : ScheduleData() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                return daysOfWeek.prettyPrint() + if (timePair.customTimeKey != null) {
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

                return ScheduleDialogFragment.ScheduleDialogData(date, daysOfWeek.toHashSet(), true, monthDayNumber, monthWeekNumber, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.WEEKLY)
            }
        }

        data class MonthlyDay(
                val dayOfMonth: Int,
                val beginningOfMonth: Boolean,
                override val timePair: TimePair) : ScheduleData() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                val day = Utils.ordinal(dayOfMonth) + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

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

                date = Utils.getDateInMonth(date.year, date.month, dayOfMonth, beginningOfMonth)

                return ScheduleDialogFragment.ScheduleDialogData(date, hashSetOf(date.dayOfWeek), true, dayOfMonth, (dayOfMonth - 1) / 7 + 1, date.dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_DAY)
            }
        }

        data class MonthlyWeek(
                val dayOfMonth: Int,
                val dayOfWeek: DayOfWeek,
                val beginningOfMonth: Boolean,
                override val timePair: TimePair) : ScheduleData() {

            override fun getText(customTimeDatas: Map<CustomTimeKey<*>, CustomTimeData>, context: Context): String {
                val day = Utils.ordinal(dayOfMonth) + " " + dayOfWeek + " " + context.getString(R.string.monthDayStart) + " " + context.resources.getStringArray(R.array.month)[if (beginningOfMonth) 0 else 1] + " " + context.getString(R.string.monthDayEnd)

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

                date = Utils.getDateInMonth(date.year, date.month, dayOfMonth, dayOfWeek, beginningOfMonth)

                return ScheduleDialogFragment.ScheduleDialogData(date, hashSetOf(dayOfWeek), false, date.day, dayOfMonth, dayOfWeek, beginningOfMonth, TimePairPersist(timePair), ScheduleType.MONTHLY_WEEK)
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
            val hourMinutes: TreeMap<DayOfWeek, HourMinute>)

    data class TaskData(
            val name: String,
            val parentKey: ParentKey?,
            val scheduleDatas: List<ScheduleData>?,
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