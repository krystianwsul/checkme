package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.UserScope
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskData
import com.krystianwsul.checkme.domainmodel.extensions.getCreateTaskParentPickerData
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentPickerFragment
import com.krystianwsul.checkme.gui.utils.SavedStateProperty
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainListener
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ObservableDomainViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.*
import com.mindorks.scheduler.Priority
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

    data class MainData(
        val taskData: TaskData?,
        val customTimeDatas: Map<CustomTimeKey, CustomTimeData>,
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