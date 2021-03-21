package com.krystianwsul.checkme.viewmodels

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskData
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import com.soywiz.klock.Month
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.*

class EditViewModel(private val savedStateHandle: SavedStateHandle) : DomainViewModel<EditViewModel.Data>() {

    companion object {

        private const val KEY_EDIT_IMAGE_STATE = "editImageState"
    }

    private lateinit var startParameters: StartParameters
    private var parentTaskKeyHint: TaskKey? = null

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getCreateTaskData(
                startParameters,
                parentTaskKeyHint
        )
    }

    val editImageStateRelay = BehaviorRelay.create<EditImageState>()!!
    val editImageState get() = editImageStateRelay.value!!

    init {
        savedStateHandle.setSavedStateProvider(KEY_EDIT_IMAGE_STATE) {
            Bundle().apply {
                editImageStateRelay.value?.let { putSerializable(KEY_EDIT_IMAGE_STATE, it) }
            }
        }
    }

    fun start(
            startParameters: StartParameters = StartParameters.Create,
            parentTaskKeyHint: TaskKey? = null,
    ) {
        this.startParameters = startParameters
        this.parentTaskKeyHint = parentTaskKeyHint

        internalStart()
    }

    fun initializeEditImageState(editDelegate: EditDelegate) {
        if (editImageStateRelay.value == null) {
            val savedEditImageState = savedStateHandle.get<Bundle>(KEY_EDIT_IMAGE_STATE)
                    ?.getSerializable(KEY_EDIT_IMAGE_STATE) as? EditImageState

            editImageStateRelay.accept(editDelegate.getInitialEditImageState(savedEditImageState))
        }
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
                        setOf(scheduleData.date.dayOfWeek),
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

                return ScheduleDialogData(
                        suggestedDate,
                        scheduleData.daysOfWeek,
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
                        setOf(date.dayOfWeek),
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
                        setOf(scheduleData.dayOfWeek),
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
                        setOf(date.dayOfWeek),
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
            val showAllInstancesDialog: Boolean?,
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
            val imageState: ImageState?,
            val assignedTo: Set<UserKey>,
    )

    data class ParentTreeData(
            override val name: String,
            val parentTreeDatas: Map<ParentKey, ParentTreeData>,
            val parentKey: ParentKey,
            override val details: String?,
            override val note: String?,
            override val sortKey: SortKey,
            val projectId: ProjectKey.Shared?,
            val projectUsers: Map<UserKey, UserData>,
    ) : ParentPickerFragment.EntryData {

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override val entryKey = parentKey
        override val childEntryDatas = parentTreeDatas.values

        override fun normalize() {
            normalizedFields
        }
    }

    sealed class ParentKey : Parcelable {

        @Parcelize
        data class Project(val projectId: ProjectKey.Shared) : ParentKey()

        @Parcelize
        data class Task(val taskKey: TaskKey) : ParentKey()
    }

    sealed class SortKey : ParentPickerFragment.SortKey {

        data class ProjectSortKey(private val projectId: ProjectKey.Shared) : SortKey() {

            override fun compareTo(other: ParentPickerFragment.SortKey): Int {
                if (other is TaskSortKey) return 1

                val projectSortKey = other as ProjectSortKey

                return projectId.compareTo(projectSortKey.projectId)
            }
        }

        data class TaskSortKey(private val startExactTimeStamp: ExactTimeStamp.Local) : SortKey() {

            override fun compareTo(other: ParentPickerFragment.SortKey): Int {
                if (other is ProjectSortKey) return -1

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

        class Join(val joinables: List<EditParameters.Join.Joinable>) : StartParameters() {

            override val excludedTaskKeys = joinables.map { it.taskKey }.toSet()
        }
    }

    @Parcelize
    data class UserData(val key: UserKey, val name: String, val photoUrl: String?) : Parcelable
}