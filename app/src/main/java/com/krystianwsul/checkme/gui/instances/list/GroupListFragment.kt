package com.krystianwsul.checkme.gui.instances.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.TooltipManager
import com.krystianwsul.checkme.TooltipManager.subscribeShowBalloon
import com.krystianwsul.checkme.databinding.FragmentGroupListBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.EditInstancesFragment
import com.krystianwsul.checkme.gui.instances.SubtaskDialogFragment
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.utils.*
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.*
import com.skydoves.balloon.ArrowOrientation
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.util.*

class GroupListFragment @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) :
    RelativeLayout(context, attrs, defStyleAttr, defStyleRes),
    FabUser,
    ListItemAddedScroller {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        const val EXPANSION_STATE_KEY = "expansionState"
        private const val LAYOUT_MANAGER_STATE = "layoutManagerState"
        private const val EDIT_INSTANCES_TAG = "editInstances"
        private const val KEY_SHOW_IMAGE = "showImage"
        private const val KEY_SEARCH_DATA = "searchData"

        private fun rangePositionToDate(timeRange: Preferences.TimeRange, position: Int): Date {
            check(position >= 0)

            val calendar = Calendar.getInstance()

            if (position > 0) {
                when (timeRange) {
                    Preferences.TimeRange.DAY -> calendar.add(Calendar.DATE, position)
                    Preferences.TimeRange.WEEK -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, position)
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    }
                    Preferences.TimeRange.MONTH -> {
                        calendar.add(Calendar.MONTH, position)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }

            return Date(calendar.toDateTimeTz())
        }

        private fun nodesToSelectedDatas(treeNodes: List<TreeNode<AbstractHolder>>): Set<GroupListDataWrapper.SelectedData> {
            val instanceDatas = ArrayList<GroupListDataWrapper.SelectedData>()

            treeNodes.map { it.modelNode }.forEach {
                when (it) {
                    is NotDoneNode -> instanceDatas += it.contentDelegate.allInstanceDatas
                    is TaskNode -> instanceDatas += it.taskData
                    else -> throw IllegalArgumentException()
                }
            }

            return instanceDatas.toSet()
        }
    }

    val activity get() = context as AbstractActivity

    lateinit var listener: GroupListListener

    val progressShown by lazy {
        getProgressShownObservable(binding.groupListRecycler) { searchDataManager.treeViewAdapter }
    }

    private val parametersRelay = BehaviorRelay.create<GroupListParameters>()
    val parameters get() = parametersRelay.value!!

    private var state = GroupListState()

    val dragHelper: DragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter(): TreeViewAdapter<AbstractHolder> = searchDataManager.treeViewAdapter

            override val recyclerView get() = binding.groupListRecycler
        }
    }

    var forceSaveStateListener: (() -> Unit)? = null

    val treeViewAdapter: TreeViewAdapter<AbstractHolder> get() = searchDataManager.treeViewAdapter

    val selectionCallback = object : SelectionCallback() {

        override val activity get() = this@GroupListFragment.activity

        override fun getTreeViewAdapter(): TreeViewAdapter<AbstractHolder> = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = getTreeViewAdapter().unselect(placeholder)

        override val bottomBarData by lazy {
            Triple(listener.getBottomBar(), R.menu.menu_edit_groups_bottom, listener::initBottomBar)
        }

        override fun onMenuClick(@IdRes itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            val treeNodes = getTreeViewAdapter().selectedNodes

            val selectedDatas = nodesToSelectedDatas(treeNodes)
            if (selectedDatas.isEmpty()) {
                MyCrashlytics.logException(NoSelectionException("menuItem.id: ${activity.normalizedId(itemId)}, selectedDatas: $selectedDatas, selectedNodes: $treeNodes"))
                return true
            }

            fun setInstancesDone(
                instanceKeys: List<InstanceKey>,
                done: Boolean,
            ) = AndroidDomainUpdater.setInstancesDone(
                DomainListenerManager.NotificationType.First(parameters.dataId),
                instanceKeys,
                done,
            )

            when (itemId) {
                R.id.actionGroupHour -> {
                    check(showHour(selectedDatas))
                    val instanceKeys = selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey }

                    AndroidDomainUpdater.setInstancesAddHourActivity(
                        DomainListenerManager.NotificationType.First(parameters.dataId),
                        instanceKeys,
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMapMaybe { listener.showSnackbarHourMaybe(it.instanceDateTimes.size).map { _ -> it } }
                        .flatMapCompletable {
                            AndroidDomainUpdater.undoInstancesAddHour(
                                DomainListenerManager.NotificationType.First(parameters.dataId),
                                it,
                            )
                        }
                        .subscribe()
                        .addTo(attachedToWindowDisposable)
                }
                R.id.action_group_edit_instance -> {
                    check(selectedDatas.isNotEmpty())

                    EditInstancesFragment.newInstance(
                        selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey },
                        parameters.dataId,
                    )
                        .also { it.listener = this@GroupListFragment::onEditInstances }
                        .show(activity.supportFragmentManager, EDIT_INSTANCES_TAG)

                    return false
                }
                R.id.action_group_share -> Utils.share(activity, getShareData(selectedDatas))
                R.id.action_group_show_task -> {
                    val selectedData = selectedDatas.single()

                    activity.startActivity(ShowTaskActivity.newIntent(selectedData.taskKey))
                }
                R.id.action_group_edit_task -> {
                    val selectedData = selectedDatas.single()
                    check(selectedData.taskCurrent)

                    val editParameters = (selectedData as? GroupListDataWrapper.InstanceData)?.let {
                        EditParameters.Edit(selectedData.instanceKey)
                    } ?: EditParameters.Edit(selectedData.taskKey)

                    activity.startActivity(EditActivity.getParametersIntent(editParameters))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = selectedDatas.map { it.taskKey }
                    check(taskKeys.isNotEmpty())
                    check(selectedDatas.all { it.taskCurrent })

                    listener.deleteTasks(parameters.dataId, taskKeys.toSet())
                }
                R.id.action_group_join -> {
                    val joinables = selectedDatas.map {
                        when (it) {
                            is GroupListDataWrapper.TaskData -> EditParameters.Join.Joinable.Task(it.taskKey)
                            is GroupListDataWrapper.InstanceData ->
                                EditParameters.Join.Joinable.Instance(it.instanceKey)
                            else -> throw IllegalArgumentException()
                        }
                    }

                    val hint = if (parameters is GroupListParameters.InstanceKey) {
                        EditActivity.Hint.Task((parameters as GroupListParameters.InstanceKey).instanceKey.taskKey)
                    } else {
                        selectedDatas.filterIsInstance<GroupListDataWrapper.InstanceData>()
                            .minByOrNull { it.instanceTimeStamp }
                            ?.let {
                                val date = it.instanceTimeStamp.date
                                val timePair = it.createTaskTimePair

                                EditActivity.Hint.Schedule(date, timePair)
                            }
                    }

                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Join(joinables, hint)))
                }
                R.id.action_group_mark_done -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                    check(instanceDatas.all { it.done == null })

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    setInstancesDone(instanceKeys, true).observeOn(AndroidSchedulers.mainThread())
                        .andThen(Maybe.defer { listener.showSnackbarDoneMaybe(instanceKeys.size) })
                        .flatMapCompletable { setInstancesDone(instanceKeys, false) }
                        .subscribe()
                        .addTo(attachedToWindowDisposable)
                }
                R.id.action_group_mark_not_done -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                    check(instanceDatas.all { it.done != null })

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    setInstancesDone(instanceKeys, false).observeOn(AndroidSchedulers.mainThread())
                        .andThen(Maybe.defer { listener.showSnackbarDoneMaybe(instanceKeys.size) })
                        .flatMapCompletable { setInstancesDone(instanceKeys, true) }
                        .subscribe()
                        .addTo(attachedToWindowDisposable)
                }
                R.id.action_group_notify -> {
                    val instanceDatas =
                        selectedDatas.filter { it.canShowNotification() }.map { it as GroupListDataWrapper.InstanceData }

                    check(instanceDatas.isNotEmpty())

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    AndroidDomainUpdater.setInstancesNotNotified(
                        DomainListenerManager.NotificationType.First(parameters.dataId),
                        instanceKeys,
                    )
                        .subscribe()
                        .addTo(attachedToWindowDisposable)
                }
                R.id.actionGroupCopyTask -> activity.startActivity(getCopyTasksIntent(selectedDatas.map { it.taskKey }))
                R.id.actionGroupWebSearch -> activity.startActivity(webSearchIntent(selectedDatas.single().name))
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups_top, actionMode!!.menu)

            listener.onCreateGroupActionMode(actionMode!!, getTreeViewAdapter(), initial)

            super.onFirstAdded(placeholder, initial)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) = listener.onDestroyGroupActionMode()

        private fun showHour(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = selectedDatas.all {
            it is GroupListDataWrapper.InstanceData
                    && it.isRootInstance
                    && it.done == null
                    && it.instanceTimeStamp <= TimeStamp.now
        }

        fun GroupListDataWrapper.SelectedData.canShowNotification(): Boolean {
            return this is GroupListDataWrapper.InstanceData
                    && isRootInstance
                    && done == null
                    && instanceTimeStamp <= TimeStamp.now
                    && !notificationShown
        }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            checkNotNull(actionMode)

            val selectedDatas = nodesToSelectedDatas(getTreeViewAdapter().selectedNodes)
            check(selectedDatas.isNotEmpty())

            val itemVisibilities = mutableListOf(
                R.id.action_group_notify to selectedDatas.any { it.canShowNotification() },
                R.id.actionGroupHour to showHour(selectedDatas),
                R.id.action_group_edit_instance to selectedDatas.all {
                    it is GroupListDataWrapper.InstanceData && it.done == null
                },
                R.id.action_group_mark_done to selectedDatas.all {
                    it is GroupListDataWrapper.InstanceData && it.done == null
                },
                R.id.action_group_mark_not_done to selectedDatas.all {
                    it is GroupListDataWrapper.InstanceData && it.done != null
                },
                R.id.actionGroupCopyTask to selectedDatas.all { it.taskCurrent },
            )

            if (selectedDatas.size == 1) {
                val selectedData = selectedDatas.single()

                itemVisibilities += listOf(
                    R.id.action_group_show_task to true,
                    R.id.action_group_edit_task to selectedData.taskCurrent,
                    R.id.action_group_join to false,
                    R.id.action_group_delete_task to selectedData.taskCurrent,
                    R.id.actionGroupWebSearch to true
                )
            } else {
                check(selectedDatas.size > 1)

                itemVisibilities += listOf(
                    R.id.action_group_show_task to false,
                    R.id.action_group_edit_task to false,
                    R.id.actionGroupWebSearch to false,
                )

                val allCurrent = selectedDatas.all { it.taskCurrent }

                itemVisibilities += listOf(
                    R.id.action_group_join to allCurrent,
                    R.id.action_group_delete_task to allCurrent
                )
            }

            return itemVisibilities
        }

        override fun getTitleCount() = nodesToSelectedDatas(getTreeViewAdapter().selectedNodes).size

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)

            forceSaveStateListener?.invoke()
        }
    }

    private val floatingActionButtonRelay = BehaviorRelay.createDefault(NullableWrapper<FloatingActionButton>())

    private var floatingActionButton: FloatingActionButton?
        get() = floatingActionButtonRelay.value!!.value
        set(value) {
            floatingActionButtonRelay.accept(NullableWrapper(value))
        }

    val shareData: String
        get() {
            val instanceDatas = parameters.groupListDataWrapper
                .instanceDatas
                .sorted()

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas) printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    val attachedToWindowDisposable = CompositeDisposable()

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            setGroupMenuItemVisibility()
            updateFabVisibility()
        }
    }

    private var showImage = false
    private var imageViewerData: Pair<ImageState, StfalconImageViewer<ImageState>>? = null

    override var scrollToTaskKey: TaskKey? = null

    override val listItemAddedListener get() = listener
    override val recyclerView: RecyclerView get() = binding.groupListRecycler

    val unscheduledFirst get() = parameters.unscheduledFirst

    private val binding = FragmentGroupListBinding.inflate(LayoutInflater.from(context), this)

    val emptyTextLayout get() = binding.groupListEmptyTextInclude.emptyTextLayout

    override val scrollDisposable = attachedToWindowDisposable

    private fun getShareData(selectedDatas: Collection<GroupListDataWrapper.SelectedData>): String {
        check(selectedDatas.isNotEmpty())

        val instanceDatas = selectedDatas.mapNotNull { it as? GroupListDataWrapper.InstanceData }
        val taskDatas = selectedDatas.mapNotNull { it as? GroupListDataWrapper.TaskData }

        val instanceMap = LinkedHashMap<InstanceKey, GroupListDataWrapper.InstanceData>()
        val taskList = mutableListOf<GroupListDataWrapper.TaskData>()

        instanceDatas.filterNot { inTree(instanceMap, it) }.forEach { instanceMap[it.instanceKey] = it }
        taskDatas.filterNot { inTree(taskList, it) }.forEach { taskList.add(it) }

        val lines = mutableListOf<String>()

        for (selectedData in instanceMap.values + taskList) printTree(lines, 0, selectedData)

        return lines.joinToString("\n")
    }

    private fun inTree(
        shareTree: Map<InstanceKey, GroupListDataWrapper.InstanceData>,
        instanceData: GroupListDataWrapper.InstanceData,
    ): Boolean = if (shareTree.containsKey(instanceData.instanceKey))
        true
    else
        shareTree.values.any { inTree(it.children, instanceData) }

    private fun inTree(
        shareTree: List<GroupListDataWrapper.TaskData>,
        childTaskData: GroupListDataWrapper.TaskData,
    ): Boolean = when {
        shareTree.isEmpty() -> false
        shareTree.contains(childTaskData) -> true
        else -> shareTree.any { inTree(it.children, childTaskData) }
    }

    private fun printTree(
        lines: MutableList<String>,
        indentation: Int,
        selectedData: GroupListDataWrapper.SelectedData,
    ) {
        lines.add("-".repeat(indentation) + selectedData.name)
        selectedData.note?.let { lines.add("-".repeat(indentation + 1) + it) }

        selectedData.childSelectedDatas.forEach { printTree(lines, indentation + 1, it) }
    }

    init {
        binding.groupListRecycler.layoutManager = LinearLayoutManager(context)
    }

    private fun newSearchDataManager() = object : SearchDataManager<GroupListParameters, GroupAdapter>(
        activity.started,
        parametersRelay
    ) {

        override val recyclerView get() = binding.groupListRecycler
        override val progressView get() = binding.groupListProgress
        override val emptyTextBinding get() = binding.groupListEmptyTextInclude

        override val emptyTextResId
            get() = when (val parameters = parameters) {
                is GroupListParameters.All -> R.string.instances_empty_root
                is GroupListParameters.InstanceKey -> if (parameters.groupListDataWrapper.taskEditable!!) {
                    R.string.empty_child
                } else {
                    R.string.empty_disabled
                }
                is GroupListParameters.Search -> R.string.noResults
                else -> null
            }

        override val compositeDisposable = attachedToWindowDisposable

        override val filterCriteriaObservable = parametersRelay.switchMap {
            if (it.filterCriteria != null)
                Observable.never()
            else
                listener.instanceSearch
        }

        override fun dataIsImmediate(data: GroupListParameters) = data.immediate

        override fun getFilterCriteriaFromData(data: GroupListParameters) = data.filterCriteria

        override fun filterDataChangeRequiresReinitializingModelAdapter(
            oldFilterCriteria: FilterCriteria,
            newFilterCriteria: FilterCriteria,
        ) = false

        override fun instantiateAdapters(filterCriteria: FilterCriteria) =
            GroupAdapter(this@GroupListFragment, filterCriteria).let { it to it.treeViewAdapter }

        override fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>) {
            binding.groupListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.groupListRecycler)
        }

        override fun initializeModelAdapter(
            modelAdapter: GroupAdapter,
            data: GroupListParameters,
            filterCriteria: FilterCriteria,
        ) {
            if (treeViewAdapterInitialized) state = modelAdapter.groupListState

            modelAdapter.initialize(
                data.dataId,
                data.groupListDataWrapper.customTimeDatas,
                data.groupingMode,
                data.groupListDataWrapper.instanceDatas,
                state,
                data.groupListDataWrapper.taskDatas,
                data.groupListDataWrapper.note,
                data.groupListDataWrapper.imageData,
                data.showProgress,
                data.useDoneNode,
                data.groupListDataWrapper.projectInfo,
            )
        }

        override fun updateTreeViewAdapterAfterModelAdapterInitialization(
            treeViewAdapter: TreeViewAdapter<AbstractHolder>,
            data: GroupListParameters,
            initial: Boolean,
            placeholder: TreeViewAdapter.Placeholder,
        ) = selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, placeholder, initial)

        override fun onDataChanged() {
            setGroupMenuItemVisibility()
            updateFabVisibility()

            tryScroll()
        }

        override fun onFilterCriteriaChanged() = Unit
    }

    private var searchDataManager = newSearchDataManager()

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            state.apply {
                classLoader = GroupListState::class.java.classLoader
                if (containsKey(EXPANSION_STATE_KEY))
                    this@GroupListFragment.state = getParcelable(EXPANSION_STATE_KEY)!!

                binding.groupListRecycler
                    .layoutManager!!
                    .onRestoreInstanceState(state.getParcelable(LAYOUT_MANAGER_STATE))

                showImage = getBoolean(KEY_SHOW_IMAGE)

                searchDataManager = newSearchDataManager()
                searchDataManager.setInitialFilterCriteria(getParcelable(KEY_SEARCH_DATA)!!)
            }

            super.onRestoreInstanceState(state.getParcelable(SUPER_STATE_KEY))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        searchDataManager.subscribe()

        activity.startTicks(receiver)

        activity.tryGetFragment<EditInstancesFragment>(EDIT_INSTANCES_TAG)?.listener = this::onEditInstances

        searchDataManager.treeViewAdapterSingle
            .flatMapObservable { it.updates }
            .subscribe {
                setGroupMenuItemVisibility()
                updateFabVisibility()
            }
            .addTo(attachedToWindowDisposable)

        floatingActionButtonRelay
            .filter { it.value != null }
            .switchMap { listener.subtaskDialogResult }
            .subscribe {
                val hint = when (it) {
                    is SubtaskDialogFragment.Result.SameTime -> it.resultData
                        .run { listOf(Triple(instanceDate, createTaskTimePair, null)) }
                        .getHint()
                    is SubtaskDialogFragment.Result.Subtask -> EditActivity.Hint.Task(it.resultData.taskKey)
                }

                startEditActivity(hint, true)
            }
            .addTo(attachedToWindowDisposable)

        parametersRelay.switchMap { parameters -> TooltipManager.fiveSecondDelay().map { parameters } }
            .filter {
                it.draggable &&
                        it.groupListDataWrapper.taskEditable != false &&
                        searchDataManager.treeViewAdapter
                            .displayedNodes
                            .none { it.isExpanded || it.isSelected } &&
                        it.groupListDataWrapper.instanceDatas.size > 1
            }
            .mapNotNull {
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                recyclerView.findViewHolderForAdapterPosition(position)?.itemView
            }
            .subscribeShowBalloon(
                context,
                TooltipManager.Type.PRESS_DRAG,
                {
                    setTextResource(R.string.tooltip_press_drag)
                    setArrowOrientation(ArrowOrientation.TOP)
                    setArrowPosition(0.1f)
                },
                { showAlignBottom(it) },
            )
            .addTo(attachedToWindowDisposable)
    }

    override fun onDetachedFromWindow() {
        attachedToWindowDisposable.clear()

        activity.unregisterReceiver(receiver)

        super.onDetachedFromWindow()
    }

    fun setAll(
        timeRange: Preferences.TimeRange,
        position: Int,
        dataId: DataId,
        immediate: Boolean,
        groupListDataWrapper: GroupListDataWrapper,
    ) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? GroupListParameters.All)?.let {
            it.timeRange != timeRange || it.position != position
        } ?: false

        setParameters(GroupListParameters.All(dataId, immediate, groupListDataWrapper, position, timeRange, differentPage))
    }

    fun setInstanceKey(
        instanceKey: InstanceKey,
        dataId: DataId,
        immediate: Boolean,
        groupListDataWrapper: GroupListDataWrapper,
    ) = setParameters(GroupListParameters.InstanceKey(dataId, immediate, groupListDataWrapper, instanceKey))

    fun setInstanceKeys(
        dataId: DataId,
        immediate: Boolean,
        groupListDataWrapper: GroupListDataWrapper,
    ) = setParameters(GroupListParameters.InstanceKeys(dataId, immediate, groupListDataWrapper))

    fun setParameters(parameters: GroupListParameters) = parametersRelay.accept(parameters)

    public override fun onSaveInstanceState() = Bundle().apply {
        putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

        if (searchDataManager.treeViewAdapterInitialized)
            putParcelable(EXPANSION_STATE_KEY, searchDataManager.modelAdapter!!.groupListState)

        putParcelable(
            LAYOUT_MANAGER_STATE,
            binding.groupListRecycler
                .layoutManager!!
                .onSaveInstanceState()
        )

        putBoolean(KEY_SHOW_IMAGE, imageViewerData != null)

        putParcelable(KEY_SEARCH_DATA, searchDataManager.filterCriteria)
    }

    private fun setGroupMenuItemVisibility() {
        listener.apply {
            val position = (parametersRelay.value as? GroupListParameters.All)?.position

            if (searchDataManager.treeViewAdapterInitialized)
                setGroupMenuItemVisibility(
                    position,
                    searchDataManager.treeViewAdapter.displayedNodes.any { it.modelNode.isSelectable }
                )
            else
                setGroupMenuItemVisibility(position, false)
        }
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        this.floatingActionButton = floatingActionButton

        updateFabVisibility()
    }

    private fun getStartEditActivityFabState(hint: EditActivity.Hint, closeActionMode: Boolean = false) =
        FabState.Visible { startEditActivity(hint, closeActionMode) }

    private fun startEditActivity(hint: EditActivity.Hint, closeActionMode: Boolean) {
        if (closeActionMode) selectionCallback.actionMode!!.finish()

        activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))
    }

    private fun List<Triple<Date, TimePair, ProjectKey.Shared?>>.getHint(): EditActivity.Hint.Schedule {
        val projectKey = map { it.third }.distinct().singleOrNull()
        val (date, timePair) = firstOrNull { it.second.customTimeKey != null } ?: first()

        return EditActivity.Hint.Schedule(date, timePair, projectKey)
    }

    private fun getFabState(): FabState {
        if (!parametersRelay.hasValue()) return FabState.Hidden

        fun List<GroupListDataWrapper.InstanceData>.getHint() = map {
            Triple(it.instanceDateTime.date, it.createTaskTimePair, it.projectKey)
        }.getHint()

        return if (selectionCallback.hasActionMode) {
            if (parameters.fabActionMode != GroupListParameters.FabActionMode.NONE) {
                val selectedNodes = searchDataManager.treeViewAdapter.selectedNodes
                val selectedDatas = nodesToSelectedDatas(selectedNodes)

                val singleSelectedData = selectedDatas.singleOrNull()

                if (singleSelectedData != null) {
                    val instanceData = singleSelectedData as? GroupListDataWrapper.InstanceData

                    val canAddSubtask = parameters.fabActionMode.showSubtask && singleSelectedData.canAddSubtask

                    val canAddToTime = parameters.fabActionMode.showTime
                            && instanceData?.run { isRootInstance && instanceTimeStamp > TimeStamp.now } == true

                    when {
                        canAddToTime && canAddSubtask -> FabState.Visible {
                            listener.showSubtaskDialog(instanceData!!.run {
                                SubtaskDialogFragment.ResultData(
                                    taskKey,
                                    instanceDateTime.date,
                                    createTaskTimePair,
                                )
                            })
                        }
                        canAddSubtask -> getStartEditActivityFabState(
                            EditActivity.Hint.Task(singleSelectedData.taskKey),
                            true,
                        )
                        canAddToTime -> getStartEditActivityFabState(
                            listOf(instanceData!!).getHint(),
                            true,
                        )
                        else -> FabState.Hidden
                    }
                } else {
                    val notDoneNode = selectedNodes.singleOrNull()?.modelNode as? NotDoneNode
                    val groupContentDelegate = notDoneNode?.contentDelegate as? NotDoneNode.ContentDelegate.Group
                    val isProjectNode = groupContentDelegate?.groupType is GroupType.Project

                    val showTime = parameters.fabActionMode.showTime || isProjectNode

                    if (showTime && selectedDatas.all { it is GroupListDataWrapper.InstanceData }) {
                        val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                        if (instanceDatas.asSequence()
                                .filter { it.isRootInstance }
                                .map { it.instanceTimeStamp }
                                .distinct()
                                .singleOrNull()
                                ?.takeIf { it > TimeStamp.now } != null
                        ) {
                            getStartEditActivityFabState(instanceDatas.getHint(), true)
                        } else {
                            FabState.Hidden
                        }
                    } else {
                        FabState.Hidden
                    }
                }
            } else {
                FabState.Hidden
            }
        } else {
            when (val parameters = parameters) {
                is GroupListParameters.All -> getStartEditActivityFabState(
                    EditActivity.Hint.Schedule(
                        rangePositionToDate(
                            parameters.timeRange,
                            parameters.position
                        )
                    )
                )
                is GroupListParameters.TimeStamp -> if (parameters.timeStamp > TimeStamp.now) {
                    val hint = parameters.groupListDataWrapper
                        .instanceDatas
                        .let {
                            if (it.isNotEmpty()) {
                                it.getHint()
                            } else {
                                EditActivity.Hint.Schedule(
                                    parameters.timeStamp.date,
                                    TimePair(parameters.timeStamp.hourMinute),
                                    parameters.projectKey,
                                )
                            }
                        }

                    getStartEditActivityFabState(hint)
                } else {
                    FabState.Hidden
                }
                is GroupListParameters.InstanceKey -> if (parameters.groupListDataWrapper.taskEditable!!)
                    getStartEditActivityFabState(EditActivity.Hint.Task(parameters.instanceKey.taskKey))
                else
                    FabState.Hidden
                is GroupListParameters.Parent -> parameters.projectKey
                    ?.let { getStartEditActivityFabState(EditActivity.Hint.Project(it)) }
                    ?: FabState.Hidden
                else -> FabState.Hidden
            }
        }
    }

    private sealed class FabState {

        data class Visible(val listener: () -> Unit) : FabState()

        object Hidden : FabState()
    }

    private fun updateFabVisibility() {
        floatingActionButton?.apply {
            when (val fabState = getFabState()) {
                is FabState.Visible -> {
                    show()

                    setOnClickListener { fabState.listener() }
                }
                FabState.Hidden -> hide()
            }
        }
    }

    override fun clearFab() {
        floatingActionButton = null
    }

    private fun getAllInstanceDatas(instanceData: GroupListDataWrapper.InstanceData): List<GroupListDataWrapper.InstanceData> {
        return listOf(
            listOf(listOf(instanceData)),
            instanceData.children.values.map { getAllInstanceDatas(it) }
        ).flatten().flatten()
    }

    private val GroupListDataWrapper.InstanceData.allTaskKeys
        get() = getAllInstanceDatas(this).map { it.taskKey }

    override fun findItem(): Int? {
        if (!searchDataManager.treeViewAdapterInitialized) return null

        return searchDataManager.treeViewAdapter
            .displayedNodes
            .firstOrNull {
                when (val modelNode = it.modelNode) {
                    is NotDoneGroupNode -> {
                        if (it.isExpanded) {
                            if (modelNode.contentDelegate is NotDoneNode.ContentDelegate.Instance) {
                                modelNode.contentDelegate.instanceData.taskKey == scrollToTaskKey
                            } else {
                                false
                            }
                        } else {
                            modelNode.contentDelegate
                                .directInstanceDatas
                                .map { it.allTaskKeys }
                                .flatten()
                                .contains(scrollToTaskKey)
                        }
                    }
                    is NotDoneInstanceNode -> {
                        if (it.isExpanded) {
                            modelNode.instanceData.taskKey == scrollToTaskKey
                        } else {
                            modelNode.instanceData
                                .allTaskKeys
                                .contains(scrollToTaskKey)
                        }
                    }
                    else -> false
                }
            }
            ?.let {
                searchDataManager.treeViewAdapter
                    .getTreeNodeCollection()
                    .getPosition(it)
            }
    }

    private fun onEditInstances(undoData: UndoData, count: Int) {
        selectionCallback.actionMode!!.finish()

        listener.showSnackbarHourMaybe(count)
            .flatMapCompletable {
                AndroidDomainUpdater.undo(DomainListenerManager.NotificationType.First(parameters.dataId), undoData)
            }
            .subscribe()
            .addTo(attachedToWindowDisposable)
    }

    fun clearExpansionStates() = searchDataManager.treeViewAdapterNullable?.clearExpansionStates()

    class GroupAdapter(val groupListFragment: GroupListFragment, filterCriteria: FilterCriteria) :
        BaseAdapter(),
        NodeCollectionParent,
        ActionModeCallback by groupListFragment.selectionCallback {

        val treeViewAdapter = TreeViewAdapter(
            this,
            TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
            filterCriteria
        )

        public override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        private lateinit var nodeCollection: NodeCollection

        val groupListState: GroupListState
            get() {
                val doneExpanded = nodeCollection.doneExpansionState
                val unscheduledExpansionState = nodeCollection.unscheduledExpansionState
                val taskExpansionStates = nodeCollection.taskExpansionStates

                val selectedNodes = treeViewAdapter.selectedNodes
                val selectedDatas = nodesToSelectedDatas(selectedNodes)
                val selectedTasks = selectedDatas.filterIsInstance<GroupListDataWrapper.TaskData>().map { it.taskKey }

                return GroupListState(
                    doneExpanded,
                    nodeCollection.contentDelegateStates,
                    unscheduledExpansionState,
                    taskExpansionStates,
                    selectedTasks,
                )
            }

        lateinit var dataId: DataId
            private set

        lateinit var customTimeDatas: List<GroupListDataWrapper.CustomTimeData>
            private set

        fun initialize(
            dataId: DataId,
            customTimeDatas: List<GroupListDataWrapper.CustomTimeData>,
            groupingMode: NodeCollection.GroupingMode,
            instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
            groupListState: GroupListState,
            taskDatas: List<GroupListDataWrapper.TaskData>,
            note: String?,
            imageState: ImageState?,
            showProgress: Boolean,
            useDoneNode: Boolean,
            projectInfo: DetailsNode.ProjectInfo?,
        ) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            nodeCollection = NodeCollection(
                0,
                this,
                groupingMode,
                treeNodeCollection,
                note,
                null,
                projectInfo,
                useDoneNode,
            )

            treeNodeCollection.nodes = nodeCollection.initialize(
                instanceDatas,
                groupListState.contentDelegateStates,
                groupListState.doneExpansionState,
                taskDatas,
                groupListState.unscheduledExpansionState,
                groupListState.taskExpansionStates,
                groupListState.selectedTaskKeys,
                imageState?.let {
                    ImageNode.ImageData(
                        it,
                        { viewer ->
                            check(groupListFragment.imageViewerData == null)

                            groupListFragment.imageViewerData = Pair(it, viewer)
                        },
                        {
                            checkNotNull(groupListFragment.imageViewerData)

                            groupListFragment.imageViewerData = null
                        },
                        groupListFragment.showImage
                    )
                }
            )

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)
            treeViewAdapter.showProgress = showProgress

            groupListFragment.showImage = false
        }

        override val groupAdapter = this

        override fun scrollToTop() = groupListFragment.scrollToTop()
    }

    private class NoSelectionException(message: String) : Exception(message)
}