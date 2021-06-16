package com.krystianwsul.checkme.gui.edit

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskData
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskParentPickerData
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.checkme.gui.utils.SavedStateProperty
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import com.soywiz.klock.Month
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import java.util.*

class EditViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {

        private const val KEY_EDIT_IMAGE_STATE = "editImageState"
        private const val KEY_DELEGATE_STATE = "delegateState"
    }

    private val clearedDisposable = CompositeDisposable()

    private lateinit var editParameters: EditParameters

    private val mainDomainListener = object : DomainListener<MainData>() {

        override val domainResultFetcher = object : DomainResultFetcher<MainData> {

            override fun getDomainResult(userScope: UserScope) =
                userScope.getCreateTaskData(editParameters.startParameters, currentParentSource!!)
        }
    }

    /**
     * todo: This should be either completely moved into a ViewModel dedicated to ParentPickerFragment, but I'm avoiding
     * that because of the complexity of the Delegate that's shared with EditInstancesFragment.
     *
     * If it stays here, then it would probably make sense to start/stop it when the ParentPickerFragment is visible,
     * but I don't care right now.  The objective I achieved was being able to display the main screen, without waiting
     * for this stupid list to be generated.
     */
    private val parentPickerDomainListener = object : DomainListener<ParentPickerData>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            it.getCreateTaskParentPickerData(editParameters.startParameters)
        }
    }

    val mainData get() = mainDomainListener.data
    val parentPickerData get() = parentPickerDomainListener.data

    private val editImageStateRelay = BehaviorRelay.create<EditImageState>()!!
    val editImageStateObservable = editImageStateRelay.hide()!!
    val editImageState get() = editImageStateRelay.value!!

    lateinit var delegate: EditDelegate
        private set

    val hasDelegate get() = this::delegate.isInitialized

    private var currentParentSource by
    SavedStateProperty<CurrentParentSource>(savedStateHandle, "currentParentSource")

    init {
        savedStateHandle.setSavedStateProvider(KEY_EDIT_IMAGE_STATE) {
            Bundle().apply {
                editImageStateRelay.value?.let { putSerializable(KEY_EDIT_IMAGE_STATE, it) }
            }
        }

        savedStateHandle.setSavedStateProvider(KEY_DELEGATE_STATE) {
            if (hasDelegate) delegate.saveState() else Bundle()
        }

        mainData.firstOrError()
                .subscribeBy {
                    check(!this::delegate.isInitialized)

                    delegate = EditDelegate.fromParameters(
                            editParameters,
                            it,
                            savedStateHandle.get(KEY_DELEGATE_STATE),
                            clearedDisposable,
                    ) { parentKey, refresh ->
                        currentParentSource = CurrentParentSource.Set(parentKey)

                        if (refresh) mainDomainListener.start(true)
                    }
                }
                .addTo(clearedDisposable)

        mainData.skip(1)
                .subscribe { delegate.newData(it) }
                .addTo(clearedDisposable)
    }

    fun start(editParameters: EditParameters, editActivity: EditActivity) {
        if (currentParentSource == null) currentParentSource = editParameters.currentParentSource

        this.editParameters = editParameters

        mainDomainListener.start()
        parentPickerDomainListener.start()

        if (editImageStateRelay.value != null) return

        val savedEditImageState = savedStateHandle.get<Bundle>(KEY_EDIT_IMAGE_STATE)
                ?.getSerializable(KEY_EDIT_IMAGE_STATE) as? EditImageState

        editParameters.getInitialEditImageStateSingle(
                savedEditImageState,
                mainData.firstOrError().map { NullableWrapper(it.taskData) },
                editActivity,
        )
                .doOnSuccess { check(editImageStateRelay.value == null) }
                .subscribe(editImageStateRelay)
                .addTo(clearedDisposable)
    }

    fun setEditImageState(editImageState: EditImageState) {
        checkNotNull(editImageStateRelay.value)

        editImageStateRelay.accept(editImageState)
    }

    fun stop() {
        mainDomainListener.stop()
        parentPickerDomainListener.stop()
    }

    override fun onCleared() {
        stop()

        clearedDisposable.dispose()
    }

    sealed class ScheduleDataWrapper : Parcelable {

        companion object {

            fun fromScheduleData(scheduleData: ScheduleData) = when (scheduleData) {
                is ScheduleData.Single -> Single(scheduleData)
                is ScheduleData.Weekly -> Weekly(scheduleData)
                is ScheduleData.MonthlyDay -> MonthlyDay(scheduleData)
                is ScheduleData.MonthlyWeek -> MonthlyWeek(scheduleData)
                is ScheduleData.Yearly -> Yearly(scheduleData)
                else -> throw UnsupportedOperationException()
            }

            private fun timePairCallback(
                    timePair: TimePair,
                    customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                    dayOfWeek: DayOfWeek? = null,
            ): String {
                return timePair.customTimeKey
                        ?.let {
                            customTimeDatas.getValue(it).let {
                                it.name + (dayOfWeek?.let { _ -> " (" + it.hourMinutes[dayOfWeek] + ")" } ?: "")
                            }
                        }
                        ?: timePair.hourMinute!!.toString()
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

        abstract fun getText(customTimeDatas: Map<CustomTimeKey, CustomTimeData>, context: Context): String

        fun getScheduleDialogData(scheduleHint: EditActivity.Hint.Schedule?) =
                getScheduleDialogDataHelper(scheduleHint?.date ?: Date.today())

        protected abstract fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData

        @Parcelize
        data class Single(override val scheduleData: ScheduleData.Single) : ScheduleDataWrapper() {

            override fun getText(
                customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                context: Context,
            ): String {
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
                    1,
                )
            }
        }

        @Parcelize
        data class Weekly(override val scheduleData: ScheduleData.Weekly) : ScheduleDataWrapper() {

            override fun getText(
                customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                context: Context,
            ): String {
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

        @Parcelize
        data class MonthlyDay(override val scheduleData: ScheduleData.MonthlyDay) : ScheduleDataWrapper() {

            override fun getText(
                customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                context: Context,
            ): String {
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
                    1,
                )
            }
        }

        @Parcelize
        data class MonthlyWeek(override val scheduleData: ScheduleData.MonthlyWeek) : ScheduleDataWrapper() {

            override fun getText(
                customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                context: Context,
            ): String {
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

        @Parcelize
        data class Yearly(override val scheduleData: ScheduleData.Yearly) : ScheduleDataWrapper() {

            override fun getText(
                customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
                context: Context,
            ): String {
                return ScheduleText.Yearly.getScheduleText(scheduleData) {
                    timePairCallback(it, customTimeDatas)
                }
            }

            override fun getScheduleDialogDataHelper(suggestedDate: Date): ScheduleDialogData {
                val date = getDateInMonth(
                    suggestedDate.year,
                    scheduleData.month,
                    scheduleData.day,
                    true,
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
                    1,
                )
            }
        }
    }

    data class MainData(
            val taskData: TaskData?,
            val customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
            val showAllInstancesDialog: Boolean?,
            val currentParent: ParentScheduleManager.Parent?,
    ) : DomainData()

    data class ParentPickerData(val parentTreeDatas: List<ParentTreeData>) : DomainData()

    data class CustomTimeData(
            val customTimeKey: CustomTimeKey,
            val name: String,
            val hourMinutes: SortedMap<DayOfWeek, HourMinute>,
            val isMine: Boolean,
    )

    data class TaskData(
            val name: String,
            val parentKey: ParentKey?,
            val scheduleDataWrappers: List<ScheduleDataWrapper>?,
            val note: String?,
            val imageState: ImageState?,
            val assignedTo: Set<UserKey>,
            val projectKey: ProjectKey<*>,
    )

    data class ParentTreeData(
            override val name: String,
            override val childEntryDatas: List<ParentTreeData>,
            override val entryKey: ParentKey,
            override val details: String?,
            override val note: String?,
            override val sortKey: SortKey,
            val projectUsers: Map<UserKey, UserData>,
            private val projectKey: ProjectKey<*>,
    ) : ParentPickerFragment.EntryData {

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override fun normalize() {
            normalizedFields
        }

        override fun matchesTaskKey(taskKey: TaskKey) = (entryKey as? ParentKey.Task)?.taskKey == taskKey

        fun toParent() = ParentScheduleManager.Parent(name, entryKey, projectUsers, projectKey)
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

    sealed class CurrentParentSource : Parcelable {

        @Parcelize
        object None : CurrentParentSource()

        @Parcelize
        data class Set(val parentKey: ParentKey?) : CurrentParentSource()

        @Parcelize
        data class FromTask(val taskKey: TaskKey) : CurrentParentSource()
    }

    @Parcelize
    data class UserData(val key: UserKey, val name: String, val photoUrl: String?) : Parcelable
}