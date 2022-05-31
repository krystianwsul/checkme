package com.krystianwsul.checkme.gui.edit

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
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
import com.krystianwsul.checkme.viewmodels.ObservableDomainViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.*
import com.mindorks.scheduler.Priority
import com.soywiz.klock.Month
import io.reactivex.rxjava3.core.Observable
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

    private val editParametersRelay = BehaviorRelay.create<EditParameters>()

    private var editParameters
        get() = editParametersRelay.value!!
        set(value) {
            editParametersRelay.accept(value)
        }

    private val mainDomainListener = object : DomainListener<MainData>() {

        override val priority = Priority.EDIT_SCREEN

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
    private val parentPickerDomainListener: DomainListener<ParentPickerData> = object : DomainListener<ParentPickerData>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData {
            parentPickerDelegate.parameters.run { it.getCreateTaskParentPickerData(startParameters, searchCriteria) }
        }
    }

    private val parentPickerDelegate =
        ObservableDomainViewModel.Delegate<ParentPickerData, ParentPickerParameters>(parentPickerDomainListener)

    val mainData get() = mainDomainListener.data
    val parentPickerData get() = parentPickerDomainListener.data

    private val editImageStateRelay = BehaviorRelay.create<EditImageState>()
    val editImageStateObservable = editImageStateRelay.hide()
    val editImageState get() = editImageStateRelay.value!!

    val delegateRelay = BehaviorRelay.create<EditDelegate>()

    var delegate
        get() = delegateRelay.value!!
        set(value) {
            delegateRelay.accept(value)
        }

    val hasDelegate get() = delegateRelay.hasValue()

    private var currentParentSource by SavedStateProperty<CurrentParentSource>(savedStateHandle, "currentParentSource")

    val searchRelay = PublishRelay.create<SearchCriteria.Search>()

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
                check(!hasDelegate)

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

        Observable.combineLatest(searchRelay, editParametersRelay) { search, editParameters ->
            ParentPickerParameters(SearchCriteria(search, showDeleted = false), editParameters.startParameters)
        }
            .subscribe(parentPickerDelegate.parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start(editParameters: EditParameters, editActivity: EditActivity) {
        if (currentParentSource == null) currentParentSource = editParameters.currentParentSource

        this.editParameters = editParameters

        mainDomainListener.start()
        parentPickerDelegate.start()

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

        parentPickerDelegate.dispose()
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

        fun getScheduleDialogData(scheduleHint: EditParentHint.Schedule?) =
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
                    ScheduleDialogData.Type.SINGLE,
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

                val type = if (scheduleData.daysOfWeek == DayOfWeek.set && scheduleData.interval == 1)
                    ScheduleDialogData.Type.DAILY
                else
                    ScheduleDialogData.Type.WEEKLY

                return ScheduleDialogData(
                    suggestedDate,
                    scheduleData.daysOfWeek,
                    true,
                    monthDayNumber,
                    dayOfMonthToWeekOfMonth(monthDayNumber),
                    suggestedDate.dayOfWeek,
                    beginningOfMonth,
                    TimePairPersist(timePair),
                    type,
                    scheduleData.from,
                    scheduleData.until,
                    scheduleData.interval,
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
                    ScheduleDialogData.Type.MONTHLY,
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
                    ScheduleDialogData.Type.MONTHLY,
                    scheduleData.from,
                    scheduleData.until,
                    1,
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
                    ScheduleDialogData.Type.YEARLY,
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
        val showJoinAllRemindersDialog: Boolean?,
        val currentParent: ParentScheduleManager.Parent?,
        val parentTaskDescription: String?,
    ) : DomainData()

    data class ParentPickerData(val parentTreeDatas: List<ParentEntryData>) : DomainData()

    data class CustomTimeData(
        val customTimeKey: CustomTimeKey,
        val name: String,
        val hourMinutes: SortedMap<DayOfWeek, HourMinute>,
        val isMine: Boolean,
    )

    data class TaskData(
        val name: String,
        val scheduleDataWrappers: List<ScheduleDataWrapper>?,
        val note: String?,
        val imageState: ImageState?,
        val assignedTo: Set<UserKey>,
    )

    sealed class ParentEntryData : ParentPickerFragment.EntryData {

        abstract override val childEntryDatas: Collection<Task>
        abstract override val entryKey: ParentKey

        protected abstract val projectKey: ProjectKey<*>

        abstract fun toParent(): ParentScheduleManager.Parent

        data class Project(
            override val name: String,
            override val childEntryDatas: List<Task>,
            override val projectKey: ProjectKey<*>,
            private val projectUsers: Map<UserKey, UserData>,
            private val projectOrder: Float,
        ) : ParentEntryData() {

            override val entryKey = ParentKey.Project(projectKey)

            override val details = projectUsers.values.joinToString(", ") { it.name }

            override val note: String? = null

            override val sortKey = SortKey.ProjectSortKey(projectKey, projectOrder)

            override val matchesSearch = false

            override fun toParent() = ParentScheduleManager.Parent.Project(name, entryKey, projectUsers)
        }

        data class Task(
            override val name: String,
            override val childEntryDatas: List<Task>,
            private val taskKey: TaskKey,
            override val details: String?,
            override val note: String?,
            override val sortKey: SortKey.TaskSortKey,
            override val projectKey: ProjectKey<*>,
            val hasMultipleInstances: Boolean?, // only for certain scenarios with Create
            val topLevelTaskIsSingleSchedule: Boolean, // only for join with showJoinAllRemindersDialog == true
            override val matchesSearch: Boolean,
        ) : ParentEntryData() {

            override val entryKey = ParentKey.Task(taskKey)

            override fun toParent(): ParentScheduleManager.Parent = ParentScheduleManager.Parent.Task(
                name,
                entryKey,
                hasMultipleInstances,
                null,
                topLevelTaskIsSingleSchedule,
            )
        }
    }

    sealed class ParentKey : Parcelable {

        @Parcelize
        data class Project(val projectId: ProjectKey<*>) : ParentKey()

        @Parcelize
        data class Task(val taskKey: TaskKey) : ParentKey()
    }

    sealed class SortKey : ParentPickerFragment.SortKey {

        data class ProjectSortKey(private val projectId: ProjectKey<*>, private val projectOrder: Float) : SortKey() {

            override fun compareTo(other: ParentPickerFragment.SortKey): Int {
                if (other is TaskSortKey) return 1

                val projectSortKey = other as ProjectSortKey

                return projectOrder.compareTo(projectSortKey.projectOrder)
                    .takeIf { it != 0 }
                    ?: projectId.compareTo(projectSortKey.projectId)
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

    sealed interface StartParameters {

        val excludedTaskKeys: Set<TaskKey>
        val parentInstanceKey: InstanceKey? get() = null

        fun showAllInstancesDialog(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Boolean? = null

        data class Create(override val parentInstanceKey: InstanceKey?) : StartParameters {

            override val excludedTaskKeys = setOf<TaskKey>()
        }

        data class MigrateDescription(val taskKey: TaskKey) : StartParameters {

            override val excludedTaskKeys = setOf<TaskKey>()
        }

        data class TaskOrInstance(val copySource: EditParameters.Copy.CopySource) : StartParameters {

            override val excludedTaskKeys = setOf(copySource.taskKey)
        }

        data class Join(private val joinables: List<EditParameters.Join.Joinable>) : StartParameters {

            override val excludedTaskKeys = joinables.map { it.taskKey }.toSet()

            // todo join parameters no longer needed
            override fun showAllInstancesDialog(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = true
        }
    }

    sealed class CurrentParentSource : Parcelable {

        @Parcelize
        object None : CurrentParentSource()

        @Parcelize
        data class Set(val parentKey: ParentKey?) : CurrentParentSource()

        @Parcelize
        data class FromTask(val taskKey: TaskKey) : CurrentParentSource()

        @Parcelize
        data class FromInstance(val instanceKey: InstanceKey) : CurrentParentSource()

        @Parcelize
        data class FromTasks(val taskKeys: kotlin.collections.Set<TaskKey>) : CurrentParentSource()
    }

    @Parcelize
    data class UserData(val key: UserKey, val name: String, val photoUrl: String?) : Parcelable

    private data class ParentPickerParameters(
        val searchCriteria: SearchCriteria,
        val startParameters: StartParameters,
    ) : ObservableDomainViewModel.Parameters
}