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
import arrow.core.Tuple4
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.TooltipManager
import com.krystianwsul.checkme.TooltipManager.subscribeShowBalloon
import com.krystianwsul.checkme.databinding.FragmentGroupListBinding
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.MixedInstanceDataCollection
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditParentHint
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.edit.SnackbarEditInstancesHostDelegate
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.main.SubtaskMenuDelegate
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.utils.*
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.ActionModeCallback
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import com.skydoves.balloon.ArrowOrientation
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
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
        private const val KEY_SHOW_IMAGE = "showImage"

        private fun rangePositionToDate(position: Int): Date {
            check(position >= 0)

            return Calendar.getInstance()
                .apply { add(Calendar.DATE, position) }
                .toDateTimeTz()
                .let(::Date)
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

        fun getHint(triples: List<Tuple4<Date, TimePair, ProjectKey.Shared?, InstanceKey?>>): EditParentHint {
            val parentInstanceKey = triples.map { it.fourth }
                .distinct()
                .singleOrNull()

            val projectKey = triples.map { it.third }
                .distinct()
                .singleOrNull()

            return if (parentInstanceKey != null) {
                EditParentHint.Instance(parentInstanceKey, projectKey)
            } else {
                val (date, timePair) = triples.firstOrNull { it.second.customTimeKey != null } ?: triples.first()

                EditParentHint.Schedule(date, timePair, projectKey)
            }
        }
    }

    val activity get() = context as AbstractActivity

    lateinit var listener: GroupListListener

    val progressShown by lazy {
        getProgressShownObservable(binding.groupListRecycler, searchDataManager.treeViewAdapterSingle.cast())
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

    val treeViewAdapterNullable get() = searchDataManager.treeViewAdapterNullable
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

            when (itemId) {
                R.id.actionGroupHour ->
                    GroupMenuUtils.onHour(selectedDatas, parameters.dataId, listener).addTo(attachedToWindowDisposable)
                R.id.action_group_edit_instance -> {
                    GroupMenuUtils.onEdit(selectedDatas, editInstancesHostDelegate)

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
                        EditParentHint.Instance((parameters as GroupListParameters.InstanceKey).instanceKey, null)
                    } else {
                        val instanceDatas = selectedDatas.filterIsInstance<GroupListDataWrapper.InstanceData>()

                        val projectKey = instanceDatas.map { it.projectKey }
                            .distinct()
                            .singleOrNull()

                        val parentInstanceKey = instanceDatas.map { it.parentInstanceKey }
                            .distinct()
                            .singleOrNull()

                        if (parentInstanceKey != null) {
                            EditParentHint.Instance(parentInstanceKey, projectKey)
                        } else {
                            instanceDatas.minByOrNull { it.instanceTimeStamp }?.let {
                                val date = it.instanceTimeStamp.date
                                val timePair = it.createTaskTimePair

                                EditParentHint.Schedule(date, timePair, projectKey)
                            }
                        }
                    }

                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Join(joinables, hint)))
                }
                R.id.action_group_mark_done ->
                    GroupMenuUtils.onCheck(selectedDatas, parameters.dataId, listener).addTo(attachedToWindowDisposable)
                R.id.action_group_mark_not_done ->
                    GroupMenuUtils.onUncheck(selectedDatas, parameters.dataId, listener).addTo(attachedToWindowDisposable)
                R.id.action_group_notify ->
                    GroupMenuUtils.onNotify(selectedDatas, parameters.dataId).addTo(attachedToWindowDisposable)
                R.id.actionGroupCopyTask -> {
                    if (selectedDatas.size > 1) {
                        activity.startActivity(ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Copy(selectedDatas.map { it.taskKey })))
                    } else {
                        val selectedData = selectedDatas.single()

                        if (selectedData is GroupListDataWrapper.InstanceData) {
                            listener.groupListViewModel.copy(selectedData.instanceKey)
                        } else {
                            activity.startActivity(EditActivity.getParametersIntent(EditParameters.Copy(selectedData.taskKey)))
                        }
                    }
                }
                R.id.actionGroupWebSearch -> activity.startActivity(webSearchIntent(selectedDatas.single().name))
                R.id.actionGroupMigrateDescription -> activity.startActivity(
                    EditActivity.getParametersIntent(EditParameters.MigrateDescription(selectedDatas.single().taskKey))
                )
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

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            checkNotNull(actionMode)

            val selectedDatas = nodesToSelectedDatas(getTreeViewAdapter().selectedNodes)
            check(selectedDatas.isNotEmpty())

            val itemVisibilities = mutableListOf(
                R.id.action_group_notify to GroupMenuUtils.showNotification(selectedDatas),
                R.id.actionGroupHour to GroupMenuUtils.showHour(selectedDatas),
                R.id.action_group_edit_instance to GroupMenuUtils.showEdit(selectedDatas),
                R.id.action_group_mark_done to GroupMenuUtils.showCheck(selectedDatas),
                R.id.action_group_mark_not_done to GroupMenuUtils.showUncheck(selectedDatas),
                R.id.actionGroupCopyTask to selectedDatas.all { it.taskCurrent },
            )

            if (selectedDatas.size == 1) {
                val selectedData = selectedDatas.single()

                itemVisibilities += listOf(
                    R.id.action_group_show_task to true,
                    R.id.action_group_edit_task to selectedData.taskCurrent,
                    R.id.action_group_join to false,
                    R.id.action_group_delete_task to selectedData.taskCurrent,
                    R.id.actionGroupWebSearch to true,
                    R.id.actionGroupMigrateDescription to selectedData.canMigrateDescription,
                )
            } else {
                check(selectedDatas.size > 1)

                itemVisibilities += listOf(
                    R.id.action_group_show_task to false,
                    R.id.action_group_edit_task to false,
                    R.id.actionGroupWebSearch to false,
                    R.id.actionGroupMigrateDescription to false,
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

    private val fabDelegateRelay = BehaviorRelay.createDefault(NullableWrapper<BottomFabMenuDelegate.FabDelegate>())

    private var fabDelegate: BottomFabMenuDelegate.FabDelegate?
        get() = fabDelegateRelay.value!!.value
        set(value) {
            fabDelegateRelay.accept(NullableWrapper(value))
        }

    val shareData: String
        get() {
            val instanceDatas = parameters.groupListDataWrapper
                .allInstanceDatas
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

    var scrollTargetMatcher: ListItemAddedScroller.ScrollTargetMatcher? = null

    override fun setTaskScrollTargetMatcher(scrollTargetMatcher: ListItemAddedScroller.ScrollTargetMatcher.Task?) {
        this.scrollTargetMatcher = scrollTargetMatcher
    }

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
        shareTree.values.any { inTree(it.allChildren.associateBy { it.instanceKey }, instanceData) }

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

        override fun dataIsImmediate(data: GroupListParameters) = data.immediate

        override fun getSearchCriteriaFromData(data: GroupListParameters) = data.groupListDataWrapper.searchCriteria

        override fun instantiateAdapters() = GroupAdapter(
            this@GroupListFragment,
            (parameters as? GroupListParameters.Parent)?.projectKey,
        ).let { it to it.treeViewAdapter }

        override fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>) {
            binding.groupListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.groupListRecycler)
        }

        override fun initializeModelAdapter(modelAdapter: GroupAdapter, data: GroupListParameters) {
            if (treeViewAdapterInitialized) state = modelAdapter.groupListState

            modelAdapter.initialize(
                data.dataId,
                data.groupListDataWrapper.customTimeDatas,
                data.groupListDataWrapper.mixedInstanceDataCollection,
                data.groupListDataWrapper.doneSingleBridges,
                state,
                data.groupListDataWrapper.taskDatas,
                data.groupListDataWrapper.note,
                data.groupListDataWrapper.imageData,
                data.showProgress,
                data.groupListDataWrapper.projectInfo,
                data.groupListDataWrapper.dropParent,
                data.doneBeforeNotDone,
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
    }

    private var searchDataManager = newSearchDataManager()

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            state.apply {
                classLoader = GroupListState::class.java.classLoader
                if (containsKey(EXPANSION_STATE_KEY))
                    this@GroupListFragment.state = getParcelable(EXPANSION_STATE_KEY)!!

                binding.groupListRecycler
                    .layoutManager
                    .onRestoreInstanceState(state.getParcelable(LAYOUT_MANAGER_STATE))

                showImage = getBoolean(KEY_SHOW_IMAGE)

                searchDataManager = newSearchDataManager()
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

        searchDataManager.treeViewAdapterSingle
            .flatMapObservable { it.updates }
            .subscribe {
                setGroupMenuItemVisibility()
                updateFabVisibility()
            }
            .addTo(attachedToWindowDisposable)

        searchDataManager.treeViewAdapterSingle
            .flatMapObservable { parametersRelay }
            .switchMap { TooltipManager.fiveSecondDelay().map { _ -> it } }
            .filter {
                it.draggable &&
                        it.groupListDataWrapper.taskEditable != false &&
                        searchDataManager.treeViewAdapter
                            .displayedNodes
                            .none { it.isExpanded || it.isSelected } &&
                        it.groupListDataWrapper.allInstanceDatas.size > 1
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
        position: Int,
        dataId: DataId,
        immediate: Boolean,
        groupListDataWrapper: GroupListDataWrapper,
    ) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? GroupListParameters.All)?.let { it.position != position } ?: false

        setParameters(GroupListParameters.All(dataId, immediate, groupListDataWrapper, position, differentPage))
    }

    fun setInstanceKey(
        instanceKey: InstanceKey,
        dataId: DataId,
        immediate: Boolean,
        groupListDataWrapper: GroupListDataWrapper,
    ) = setParameters(GroupListParameters.InstanceKey(dataId, immediate, groupListDataWrapper, instanceKey))

    fun setParameters(parameters: GroupListParameters) = parametersRelay.accept(parameters)

    public override fun onSaveInstanceState() = Bundle().apply {
        putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

        if (searchDataManager.treeViewAdapterInitialized)
            putParcelable(EXPANSION_STATE_KEY, searchDataManager.modelAdapter!!.groupListState)

        putParcelable(
            LAYOUT_MANAGER_STATE,
            binding.groupListRecycler
                .layoutManager
                .onSaveInstanceState(),
        )

        putBoolean(KEY_SHOW_IMAGE, imageViewerData != null)
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

    fun setVisible(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        setFab(fabDelegate)

        editInstancesHostDelegate.onCreate()
    }

    // only call internally
    override fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        this.fabDelegate = fabDelegate

        updateFabVisibility()
    }

    private fun getStartEditActivityFabState(hint: EditParentHint, closeActionMode: Boolean = false) =
        FabState.Visible {
            if (closeActionMode) selectionCallback.actionMode!!.finish()

            activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))
        }

    private fun getFabMenuFabState(hint: EditParentHint) =
        FabState.Visible {
            selectionCallback.actionMode?.finish()

            listener.showFabMenu(ReminderOrNoteMenuDelegate(hint))
        }

    private fun getFabState(): FabState {
        if (!parametersRelay.hasValue()) return FabState.Hidden

        fun List<GroupListDataWrapper.InstanceData>.getHint() =
            getHint(map { Tuple4(it.instanceDate, it.createTaskTimePair, it.projectKey, it.parentInstanceKey) })

        return if (selectionCallback.hasActionMode) {
            if (parameters.fabActionMode != GroupListParameters.FabActionMode.NONE) {
                val selectedNodes = searchDataManager.treeViewAdapter.selectedNodes
                val selectedDatas = nodesToSelectedDatas(selectedNodes)

                val singleSelectedData = selectedDatas.singleOrNull()

                if (singleSelectedData != null) {
                    val instanceData = singleSelectedData as? GroupListDataWrapper.InstanceData

                    val canAddSubtask = parameters.fabActionMode.showSubtask && singleSelectedData.canAddSubtask

                    val isTopLevel = instanceData?.isRootInstance == true

                    val canAddToTime = parameters.fabActionMode.showTime && isTopLevel

                    val canAddToProject = isTopLevel && instanceData?.projectKey != null

                    val multipleValid = listOf(canAddToTime, canAddSubtask, canAddToProject).filter { it }.size > 1

                    when {
                        multipleValid -> FabState.Visible {
                            selectionCallback.actionMode!!.finish()

                            listener.showFabMenu(instanceData!!.run {
                                SubtaskMenuDelegate(
                                    instanceKey.takeIf { canAddSubtask },
                                    instanceDate,
                                    createTaskTimePair,
                                    instanceData.projectKey,
                                    canAddToTime,
                                )
                            })
                        }
                        canAddSubtask -> getStartEditActivityFabState(
                            instanceData?.let { EditParentHint.Instance(it.instanceKey, null) }
                                ?: EditParentHint.Task(singleSelectedData.taskKey),
                            true,
                        )
                        canAddToTime -> getStartEditActivityFabState(listOf(instanceData!!).getHint(), true)
                        canAddToProject -> getStartEditActivityFabState(
                            EditParentHint.Project(instanceData!!.projectKey!!),
                            true,
                        )
                        else -> FabState.Hidden
                    }
                } else {
                    val notDoneNode = selectedNodes.singleOrNull()?.modelNode as? NotDoneNode
                    val groupContentDelegate = notDoneNode?.contentDelegate as? NotDoneNode.ContentDelegate.Group
                    val isProjectNode = groupContentDelegate?.bridge is GroupTypeFactory.ProjectBridge

                    val showTime = parameters.fabActionMode.showTime || isProjectNode

                    if (showTime && selectedDatas.all { it is GroupListDataWrapper.InstanceData }) {
                        val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                        fun canAddToInstances(): Boolean {
                            val rootInstances = instanceDatas.filter { it.isRootInstance }

                            if (rootInstances.isEmpty()) return false

                            return true
                        }

                        if (canAddToInstances()) {
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
                is GroupListParameters.All -> FabState.Visible {
                    listener.showFabMenu(
                        ReminderOrNoteMenuDelegate(
                            EditParentHint.Schedule(rangePositionToDate(parameters.position)),
                        )
                    )
                }
                is GroupListParameters.TimeStamp -> {
                    val hint = parameters.groupListDataWrapper
                        .allInstanceDatas
                        .let {
                            if (it.isNotEmpty()) {
                                it.getHint()
                            } else {
                                parameters.fabData.toEditParentHint()
                            }
                        }

                    getStartEditActivityFabState(hint)
                }
                is GroupListParameters.InstanceKey -> if (parameters.groupListDataWrapper.taskEditable!!)
                    getStartEditActivityFabState(EditParentHint.Instance(parameters.instanceKey, null))
                else
                    FabState.Hidden
                is GroupListParameters.Parent -> parameters.projectKey
                    ?.let { getFabMenuFabState(EditParentHint.Project(it)) }
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
        fabDelegate?.apply {
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
        fabDelegate = null
    }

    private fun getAllInstanceDatas(instanceData: GroupListDataWrapper.InstanceData): List<GroupListDataWrapper.InstanceData> {
        return listOf(
            listOf(listOf(instanceData)),
            instanceData.allChildren.map(::getAllInstanceDatas)
        ).flatten().flatten()
    }

    override fun findItem(): Int? {
        if (!searchDataManager.treeViewAdapterInitialized) return null

        val scrollTargetMatcher = this.scrollTargetMatcher ?: return null

        fun ListItemAddedScroller.ScrollTargetMatcher.matchesInstanceData(instanceData: GroupListDataWrapper.InstanceData) =
            when (this) {
                is ListItemAddedScroller.ScrollTargetMatcher.Task -> instanceData.taskKey == taskKey
                is ListItemAddedScroller.ScrollTargetMatcher.Instance -> instanceData.instanceKey == instanceKey
            }

        return searchDataManager.treeViewAdapter
            .displayedNodes
            .firstOrNull {
                when (val modelNode = it.modelNode) {
                    is NotDoneGroupNode -> {
                        if (it.isExpanded) {
                            if (modelNode.contentDelegate is NotDoneNode.ContentDelegate.Instance) {
                                scrollTargetMatcher.matchesInstanceData(modelNode.contentDelegate.instanceData)
                            } else {
                                false
                            }
                        } else {
                            modelNode.contentDelegate
                                .directInstanceDatas
                                .any { getAllInstanceDatas(it).any(scrollTargetMatcher::matchesInstanceData) }
                        }
                    }
                    is NotDoneInstanceNode -> {
                        if (it.isExpanded) {
                            scrollTargetMatcher.matchesInstanceData(modelNode.instanceData)
                        } else {
                            getAllInstanceDatas(modelNode.instanceData).any(scrollTargetMatcher::matchesInstanceData)
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

    private inner class EditInstancesSnackbarHostDelegate :
        SnackbarEditInstancesHostDelegate(attachedToWindowDisposable) {

        override val tag = "groupListEditInstances"

        override val dataId get() = parameters.dataId

        override val activity get() = this@GroupListFragment.activity

        override val snackbarListener get() = listener

        override fun beforeEditInstances(instanceKeys: Set<InstanceKey>) = selectionCallback.actionMode!!.finish()
    }

    private val editInstancesHostDelegate by lazy { EditInstancesSnackbarHostDelegate() }

    fun clearExpansionStates() = searchDataManager.treeViewAdapterNullable?.clearExpansionStates()

    class GroupAdapter(val groupListFragment: GroupListFragment, private val unscheduledNodeProjectKey: ProjectKey.Shared?) :
        BaseAdapter(),
        NodeCollectionParent,
        ActionModeCallback by groupListFragment.selectionCallback {

        val treeViewAdapter = TreeViewAdapter(
            this,
            TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
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

        lateinit var dropParent: DropParent
            private set

        fun initialize(
            dataId: DataId,
            customTimeDatas: List<GroupListDataWrapper.CustomTimeData>,
            mixedInstanceDataCollection: MixedInstanceDataCollection,
            doneSingleBridges: List<GroupTypeFactory.SingleBridge>,
            groupListState: GroupListState,
            taskDatas: List<GroupListDataWrapper.TaskData>,
            note: String?,
            imageState: ImageState?,
            showProgress: Boolean,
            projectInfo: DetailsNode.ProjectInfo?,
            dropParent: DropParent,
            doneBeforeNotDone: Boolean,
        ) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas
            this.dropParent = dropParent

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            nodeCollection = NodeCollection(
                0,
                this,
                treeNodeCollection,
                note,
                null,
                projectInfo,
                unscheduledNodeProjectKey,
                doneBeforeNotDone,
            )

            treeNodeCollection.nodes = nodeCollection.initialize(
                mixedInstanceDataCollection,
                doneSingleBridges,
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

        override fun mutateIds(oldIds: List<Any>, newIds: List<Any>): Pair<List<Any>, List<Any>> {
            val mutatedOldIds = oldIds.toMutableList()
            val mutatedNewIds = newIds.toMutableList()

            listOf(
                ::matchGroupsByInstanceKeys,
                ::matchGroupsByInstanceKeyIntersect,
                ::matchInstances,
                ::matchInstancesByTaskKey,
            ).forEach {
                it(mutatedOldIds, mutatedNewIds)
                it(mutatedNewIds, mutatedOldIds)
            }

            return mutatedOldIds to mutatedNewIds
        }

        private fun List<Any>.filterInstanceId() = filterIsInstance<NotDoneNode.ContentDelegate.Instance.Id>()
        private fun List<Any>.filterGroupId() = filterIsInstance<NotDoneNode.ContentDelegate.Group.Id>()
        private fun List<Any>.filterTimeId() = filterIsInstance<NotDoneNode.ContentDelegate.Group.Id.Time>()
        private fun List<Any>.filterProjectId() = filterIsInstance<NotDoneNode.ContentDelegate.Group.Id.Project>()

        // this covers rescheduling a whole group of instances
        private fun matchGroupsByInstanceKeys(referenceList: List<Any>, mutableList: MutableList<Any>) {
            referenceList.filterGroupId()
                .filter { it !in mutableList }
                .forEach { currId ->
                    mutableList.filterGroupId()
                        .singleOrNull { it.instanceKeys == currId.instanceKeys }
                        ?.takeIf { it !in referenceList }
                        ?.let { mutableList[mutableList.indexOf(it)] = currId }
                }
        }

        private fun matchGroupsByInstanceKeyIntersect(referenceList: List<Any>, mutableList: MutableList<Any>) {
            referenceList.filterGroupId()
                .filter { it !in mutableList }
                .forEach { currId ->
                    mutableList.filterGroupId()
                        .singleOrNull { it.instanceKeys.intersect(currId.instanceKeys).isNotEmpty() }
                        ?.takeIf { it !in referenceList }
                        ?.let { mutableList[mutableList.indexOf(it)] = currId }
                }
        }

        // this covers a instance <-> group transformation
        private fun matchInstances(referenceList: List<Any>, mutableList: MutableList<Any>) {
            referenceList.filterIsInstance<NotDoneNode.ContentDelegate.Instance.Id>()
                .filter { it !in mutableList }
                .forEach { instanceId ->
                    mutableList.filterGroupId()
                        .singleOrNull { it.instanceKeys.contains(instanceId.instanceKey) }
                        ?.takeIf { it !in referenceList }
                        ?.let { mutableList[mutableList.indexOf(it)] = instanceId }
                }
        }

        // this covers a schedule change.  Doesn't cover multiple matches
        private fun matchInstancesByTaskKey(referenceList: List<Any>, mutableList: MutableList<Any>) {
            referenceList.filterIsInstance<NotDoneNode.ContentDelegate.Instance.Id>()
                .filter { it !in mutableList }
                .forEach { instanceId ->
                    mutableList.filterInstanceId()
                        .filter { it !in referenceList } // order is important, search only unmatched
                        .singleOrNull { it.instanceKey.taskKey == instanceId.instanceKey.taskKey }
                        ?.let { mutableList[mutableList.indexOf(it)] = instanceId }
                }
        }
    }

    private class NoSelectionException(message: String) : Exception(message)
}