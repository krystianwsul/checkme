package com.krystianwsul.checkme.gui.instances.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.EditInstancesFragment
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.empty_text.view.*
import kotlinx.android.synthetic.main.fragment_group_list.view.*
import java.util.*

class GroupListFragment @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), FabUser, ListItemAddedScroller {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        const val EXPANSION_STATE_KEY = "expansionState"
        private const val LAYOUT_MANAGER_STATE = "layoutManagerState"
        private const val EDIT_INSTANCES_TAG = "editInstances"
        private const val KEY_SHOW_IMAGE = "showImage"

        private fun rangePositionToDate(timeRange: MainActivity.TimeRange, position: Int): Date {
            check(position >= 0)

            val calendar = Calendar.getInstance()

            if (position > 0) {
                when (timeRange) {
                    MainActivity.TimeRange.DAY -> calendar.add(Calendar.DATE, position)
                    MainActivity.TimeRange.WEEK -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, position)
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    }
                    MainActivity.TimeRange.MONTH -> {
                        calendar.add(Calendar.MONTH, position)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }

            return Date(calendar.toDateTimeTz())
        }

        private fun nodesToSelectedDatas(treeNodes: List<TreeNode<NodeHolder>>, includeGroups: Boolean): Set<GroupListDataWrapper.SelectedData> {
            val instanceDatas = ArrayList<GroupListDataWrapper.SelectedData>()
            treeNodes.map { it.modelNode }.forEach {
                when (it) {
                    is NotDoneGroupNode -> {
                        if (includeGroups || it.singleInstance())
                            instanceDatas.addAll(it.instanceDatas)
                    }
                    is NotDoneGroupNode.NotDoneInstanceNode -> instanceDatas.add(it.instanceData)
                    is DoneInstanceNode -> instanceDatas.add(it.instanceData)
                    is TaskNode -> instanceDatas.add(it.taskData)
                    else -> throw IllegalArgumentException()
                }
            }

            return instanceDatas.toSet()
        }

        fun recursiveExists(instanceData: GroupListDataWrapper.InstanceData) {
            instanceData.exists = true

            if (instanceData.instanceDataParent is GroupListDataWrapper.InstanceData) {
                recursiveExists(instanceData.instanceDataParent as GroupListDataWrapper.InstanceData)
            } else {
                check(instanceData.instanceDataParent is GroupListDataWrapper)
            }
        }
    }

    val activity get() = context as AbstractActivity
    val listener get() = context as GroupListListener

    lateinit var treeViewAdapter: TreeViewAdapter<NodeHolder>
        private set

    private val parametersRelay = BehaviorRelay.create<GroupListParameters>()
    val parameters get() = parametersRelay.value!!

    private var state = GroupListState()

    val dragHelper: DragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter() = treeViewAdapter

            override fun onSetNewItemPosition() = selectionCallback.actionMode!!.finish()
        }
    }

    var forceSaveStateListener: (() -> Unit)? = null

    val selectionCallback = object : SelectionCallback() {

        override val activity get() = this@GroupListFragment.activity

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_edit_groups_bottom, listener::initBottomBar) }

        override fun onMenuClick(@IdRes itemId: Int, x: TreeViewAdapter.Placeholder): Boolean {
            val treeNodes = treeViewAdapter.selectedNodes

            val selectedDatas = nodesToSelectedDatas(treeNodes, true)
            if (selectedDatas.isEmpty()) {
                MyCrashlytics.logException(NoSelectionException("menuItem.id: ${activity.normalizedId(itemId)}, selectedDatas: $selectedDatas, selectedNodes: $treeNodes"))
                return true
            }

            when (itemId) {
                R.id.actionGroupHour -> {
                    check(showHour(selectedDatas))
                    val instanceKeys = selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey }

                    addHour(instanceKeys)
                }
                R.id.action_group_edit_instance -> {
                    check(selectedDatas.isNotEmpty())

                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }
                    check(instanceDatas.isNotEmpty())
                    check(instanceDatas.all { it.isRootInstance })

                    val instanceKeys = ArrayList(instanceDatas.map { it.instanceKey })

                    EditInstancesFragment.newInstance(instanceKeys)
                            .also { it.listener = this@GroupListFragment::onEditInstances }
                            .show(activity.supportFragmentManager, EDIT_INSTANCES_TAG)

                    return false
                }
                R.id.action_group_share -> Utils.share(activity, getShareData(selectedDatas))
                R.id.action_group_show_task -> {
                    val instanceData = selectedDatas.single()

                    activity.startActivity(ShowTaskActivity.newIntent(instanceData.taskKey))
                }
                R.id.action_group_edit_task -> {
                    val instanceData = selectedDatas.single()
                    check(instanceData.taskCurrent)

                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Edit(instanceData.taskKey)))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = selectedDatas.map { it.taskKey }
                    check(taskKeys.isNotEmpty())
                    check(selectedDatas.all { it.taskCurrent })

                    listener.deleteTasks(taskKeys.toSet())
                }
                R.id.action_group_add_task -> {
                    val instanceData = selectedDatas.single()
                    check(instanceData.taskCurrent)

                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(EditActivity.Hint.Task(instanceData.taskKey))))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(selectedDatas.map { it.taskKey })
                    check(taskKeys.size > 1)

                    if (parameters is GroupListParameters.InstanceKey) {
                        activity.startActivity(EditActivity.getParametersIntent(EditParameters.Join(taskKeys, EditActivity.Hint.Task((parameters as GroupListParameters.InstanceKey).instanceKey.taskKey))))
                    } else {
                        val instanceDatas = selectedDatas.filterIsInstance<GroupListDataWrapper.InstanceData>()

                        val scheduleHint = instanceDatas.minBy { it.instanceTimeStamp }?.let {
                            val date = it.instanceTimeStamp.date
                            val timePair = it.createTaskTimePair

                            EditActivity.Hint.Schedule(date, timePair)
                        }

                        val removeInstanceKeys = instanceDatas.map { it.instanceKey }

                        activity.startActivity(EditActivity.getParametersIntent(EditParameters.Join(taskKeys, scheduleHint, removeInstanceKeys)))
                    }
                }
                R.id.action_group_mark_done -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                    check(instanceDatas.all { it.done == null })

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    val done = DomainFactory.instance.setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, true)

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) { treeNode ->
                        treeNode.modelNode.let {
                            if (it is NotDoneGroupNode) {
                                val nodeCollection = it.nodeCollection

                                nodeCollection.notDoneGroupCollection.remove(it, x)

                                if (!it.expanded()) {
                                    it.instanceDatas.forEach {
                                        it.done = done

                                        recursiveExists(it)

                                        nodeCollection.dividerNode.add(it, x)
                                    }
                                } else {
                                    check(it.treeNode.allChildren.all { it.isSelected })
                                }

                                decrementSelected(x)
                            } else {
                                val instanceData = (it as NotDoneGroupNode.NotDoneInstanceNode).instanceData
                                instanceData.done = done

                                recursiveExists(instanceData)

                                it.removeFromParent(x)

                                it.parentNodeCollection.dividerNode.add(instanceData, x)
                            }
                        }
                    }

                    listener.showSnackbarDone(instanceKeys.size) {
                        DomainFactory.instance.setInstancesDone(0, SaveService.Source.GUI, instanceKeys, false)
                    }
                }
                R.id.action_group_mark_not_done -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                    check(instanceDatas.all { it.done != null })

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    DomainFactory.instance.setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, false)

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) { treeNode ->
                        treeNode.modelNode.let {
                            val instanceData = (it as DoneInstanceNode).instanceData
                            instanceData.done = null

                            recursiveExists(instanceData)

                            it.removeFromParent(x)

                            it.dividerNode
                                    .nodeCollection
                                    .notDoneGroupCollection
                                    .add(instanceData, x)
                        }
                    }

                    listener.showSnackbarDone(instanceKeys.size) {
                        DomainFactory.instance.setInstancesDone(0, SaveService.Source.GUI, instanceKeys, true)
                    }
                }
                R.id.action_group_notify -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

                    instanceDatas.all { it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown }

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    DomainFactory.instance.setInstancesNotNotified(parameters.dataId, SaveService.Source.GUI, instanceKeys)

                    instanceDatas.forEach { it.notificationShown = true }
                }
                R.id.actionGroupCopyTask -> {
                    val instanceData = selectedDatas.single()

                    activity.startActivity(EditActivity.getParametersIntent(EditParameters.Copy(instanceData.taskKey)))
                }
                R.id.actionGroupRemoveFromParent -> {
                    val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }
                    check(instanceDatas.all { it.isRecurringGroupChild })

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    DomainFactory.instance.removeFromParent(SaveService.Source.GUI, instanceKeys)
                }
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups_top, actionMode!!.menu)

            listener.onCreateGroupActionMode(actionMode!!, treeViewAdapter)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) = listener.onDestroyGroupActionMode()

        private fun showHour(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            checkNotNull(actionMode)

            val selectedDatas = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true)
            check(selectedDatas.isNotEmpty())

            val itemVisibilities = mutableListOf(
                    R.id.action_group_notify to selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown },
                    R.id.actionGroupHour to showHour(selectedDatas),
                    R.id.action_group_edit_instance to selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.isRootInstance && it.done == null },
                    R.id.action_group_mark_done to selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.done == null },
                    R.id.action_group_mark_not_done to selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.done != null },
                    R.id.actionGroupRemoveFromParent to selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.isRecurringGroupChild }
            )

            if (selectedDatas.size == 1) {
                val instanceData = selectedDatas.single()

                itemVisibilities += listOf(
                        R.id.action_group_show_task to true,
                        R.id.action_group_edit_task to instanceData.taskCurrent,
                        R.id.action_group_join to false,
                        R.id.action_group_delete_task to instanceData.taskCurrent,
                        R.id.action_group_add_task to instanceData.taskCurrent,
                        R.id.actionGroupCopyTask to instanceData.taskCurrent
                )
            } else {
                check(selectedDatas.size > 1)

                itemVisibilities += listOf(
                        R.id.action_group_show_task to false,
                        R.id.action_group_edit_task to false,
                        R.id.action_group_add_task to false,
                        R.id.actionGroupCopyTask to false
                )

                val allCurrent = selectedDatas.all { it.taskCurrent }

                itemVisibilities += listOf(
                        R.id.action_group_join to allCurrent,
                        R.id.action_group_delete_task to allCurrent
                )
            }

            return itemVisibilities
        }

        override fun getTitleCount() = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true).size

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)

            forceSaveStateListener?.invoke()
        }
    }

    private var floatingActionButton: FloatingActionButton? = null

    val shareData: String?
        get() {
            val instanceDatas = parameters.groupListDataWrapper
                    .instanceDatas
                    .sorted()

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas)
                printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    private val compositeDisposable = CompositeDisposable()

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
    override val recyclerView: RecyclerView get() = groupListRecycler

    private fun getShareData(selectedDatas: Collection<GroupListDataWrapper.SelectedData>): String {
        check(selectedDatas.isNotEmpty())

        val instanceDatas = selectedDatas.mapNotNull { it as? GroupListDataWrapper.InstanceData }
        val taskDatas = selectedDatas.mapNotNull { it as? GroupListDataWrapper.TaskData }

        val instanceMap = LinkedHashMap<InstanceKey, GroupListDataWrapper.InstanceData>()
        val taskList = mutableListOf<GroupListDataWrapper.TaskData>()

        instanceDatas.filterNot { inTree(instanceMap, it) }.forEach { instanceMap[it.instanceKey] = it }
        taskDatas.filterNot { inTree(taskList, it) }.forEach { taskList.add(it) }

        val lines = mutableListOf<String>()

        for (selectedData in instanceMap.values + taskList)
            printTree(lines, 0, selectedData)

        return lines.joinToString("\n")
    }

    private fun inTree(shareTree: Map<InstanceKey, GroupListDataWrapper.InstanceData>, instanceData: GroupListDataWrapper.InstanceData): Boolean = if (shareTree.containsKey(instanceData.instanceKey)) true else shareTree.values.any { inTree(it.children, instanceData) }

    private fun inTree(shareTree: List<GroupListDataWrapper.TaskData>, childTaskData: GroupListDataWrapper.TaskData): Boolean {
        if (shareTree.isEmpty())
            return false

        return if (shareTree.contains(childTaskData)) true else shareTree.any { inTree(it.children, childTaskData) }
    }

    private fun printTree(lines: MutableList<String>, indentation: Int, selectedData: GroupListDataWrapper.SelectedData) {
        lines.add("-".repeat(indentation) + selectedData.name)
        selectedData.note?.let { lines.add("-".repeat(indentation + 1) + it) }

        selectedData.childSelectedDatas.forEach { printTree(lines, indentation + 1, it) }
    }

    init {
        check(context is GroupListListener)

        inflate(context, R.layout.fragment_group_list, this)

        groupListRecycler.layoutManager = LinearLayoutManager(context)
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            state.apply {
                classLoader = GroupListState::class.java.classLoader
                if (containsKey(EXPANSION_STATE_KEY))
                    this@GroupListFragment.state = getParcelable(EXPANSION_STATE_KEY)!!

                groupListRecycler.layoutManager!!.onRestoreInstanceState(state.getParcelable(LAYOUT_MANAGER_STATE))

                showImage = getBoolean(KEY_SHOW_IMAGE)
            }

            super.onRestoreInstanceState(state.getParcelable(SUPER_STATE_KEY))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val observable = activity.started.switchMap {
            if (it)
                parametersRelay
            else
                Observable.never<GroupListParameters>()
        }

        compositeDisposable += observable.subscribe { initialize() }

        activity.startTicks(receiver)

        (activity.supportFragmentManager.findFragmentByTag(EDIT_INSTANCES_TAG) as? EditInstancesFragment)?.listener = this::onEditInstances
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        activity.unregisterReceiver(receiver)

        super.onDetachedFromWindow()
    }

    fun setAll(
            timeRange: MainActivity.TimeRange,
            position: Int,
            dataId: Int,
            immediate: Boolean,
            groupListDataWrapper: GroupListDataWrapper
    ) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? GroupListParameters.All)?.let {
            it.timeRange != timeRange || it.position != position
        } ?: false

        parametersRelay.accept(GroupListParameters.All(dataId, immediate, groupListDataWrapper, position, timeRange, differentPage))
    }

    fun setTimeStamp(
            timeStamp: TimeStamp,
            dataId: Int,
            immediate: Boolean,
            groupListDataWrapper: GroupListDataWrapper
    ) = parametersRelay.accept(GroupListParameters.TimeStamp(dataId, immediate, groupListDataWrapper, timeStamp))

    fun setInstanceKey(
            instanceKey: InstanceKey,
            dataId: Int,
            immediate: Boolean,
            groupListDataWrapper: GroupListDataWrapper
    ) = parametersRelay.accept(GroupListParameters.InstanceKey(dataId, immediate, groupListDataWrapper, instanceKey))

    fun setInstanceKeys(
            dataId: Int,
            immediate: Boolean,
            groupListDataWrapper: GroupListDataWrapper
    ) = parametersRelay.accept(GroupListParameters.InstanceKeys(dataId, immediate, groupListDataWrapper))

    fun setTaskKey(
            taskKey: TaskKey,
            dataId: Int,
            immediate: Boolean,
            groupListDataWrapper: GroupListDataWrapper,
            showLoader: Boolean
    ) = parametersRelay.accept(GroupListParameters.TaskKey(dataId, immediate, groupListDataWrapper, taskKey, showLoader))

    private fun useGroups() = parameters is GroupListParameters.All

    public override fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

            if (this@GroupListFragment::treeViewAdapter.isInitialized)
                putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter.treeModelAdapter as GroupAdapter).groupListState)

            putParcelable(LAYOUT_MANAGER_STATE, groupListRecycler.layoutManager!!.onSaveInstanceState())

            putBoolean(KEY_SHOW_IMAGE, imageViewerData != null)
        }
    }

    private fun initialize() {
        if (this::treeViewAdapter.isInitialized && (parameters as? GroupListParameters.All)?.differentPage != true) {
            state = (treeViewAdapter.treeModelAdapter as GroupAdapter).groupListState

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as GroupAdapter).initialize(
                        parameters.dataId,
                        parameters.groupListDataWrapper.customTimeDatas,
                        useGroups(),
                        parameters.groupListDataWrapper.instanceDatas,
                        state,
                        parameters.groupListDataWrapper.taskDatas,
                        parameters.groupListDataWrapper.note,
                        parameters.groupListDataWrapper.imageData,
                        parameters.showProgress,
                        parameters.useDoneNode
                )

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        } else {
            val groupAdapter = GroupAdapter(this)

            groupAdapter.initialize(
                    parameters.dataId,
                    parameters.groupListDataWrapper.customTimeDatas,
                    useGroups(),
                    parameters.groupListDataWrapper.instanceDatas,
                    state,
                    parameters.groupListDataWrapper.taskDatas,
                    parameters.groupListDataWrapper.note,
                    parameters.groupListDataWrapper.imageData,
                    parameters.showProgress,
                    parameters.useDoneNode
            )

            treeViewAdapter = groupAdapter.treeViewAdapter

            groupListRecycler.adapter = treeViewAdapter
            groupListRecycler.itemAnimator = CustomItemAnimator()

            treeViewAdapter.updates
                    .subscribe {
                        setGroupMenuItemVisibility()
                        updateFabVisibility()
                    }
                    .addTo(compositeDisposable)

            dragHelper.attachToRecyclerView(groupListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        }

        val emptyTextId = when (val parameters = parameters) {
            is GroupListParameters.All -> R.string.instances_empty_root
            is GroupListParameters.InstanceKey -> if (parameters.groupListDataWrapper.taskEditable!!) {
                R.string.empty_child
            } else {
                R.string.empty_disabled
            }
            else -> null
        }

        val hide = mutableListOf<View>(groupListProgress)
        val show = mutableListOf<View>()

        if (treeViewAdapter.displayedNodes.isEmpty()) {
            hide.add(groupListRecycler)

            if (emptyTextId != null) {
                show.add(emptyTextLayout)
                emptyText.setText(emptyTextId)
            } else {
                hide.add(emptyTextLayout)
            }
        } else {
            show.add(groupListRecycler)
            hide.add(emptyTextLayout)
        }

        animateVisibility(show, hide, immediate = parameters.immediate)

        setGroupMenuItemVisibility()
        updateFabVisibility()

        tryScroll()
    }

    private fun setGroupMenuItemVisibility() {
        listener.apply {
            val position = (parametersRelay.value as? GroupListParameters.All)?.position

            if (this@GroupListFragment::treeViewAdapter.isInitialized)
                setGroupMenuItemVisibility(
                        position,
                        treeViewAdapter.displayedNodes.any { it.modelNode.isSelectable })
            else
                setGroupMenuItemVisibility(position, false)
        }
    }

    private fun addHour(instanceKeys: Collection<InstanceKey>) {
        val hourUndoData = DomainFactory.instance.setInstancesAddHourActivity(0, SaveService.Source.GUI, instanceKeys)

        listener.showSnackbarHour(hourUndoData.instanceDateTimes.size) {
            DomainFactory.instance.undoInstancesAddHour(0, SaveService.Source.GUI, hourUndoData)
        }
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        this.floatingActionButton = floatingActionButton

        floatingActionButton.setOnClickListener {
            check(showFab())

            fun List<GroupListDataWrapper.InstanceData>.getHint() = (firstOrNull { it.createTaskTimePair.customTimeKey != null }
                    ?: first()).let {
                EditActivity.Hint.Schedule(it.instanceTimeStamp.date, it.createTaskTimePair)
            }

            val hint = when (val parameters = parameters) {
                is GroupListParameters.All -> {
                    val actionMode = selectionCallback.actionMode

                    if (actionMode != null) {
                        val hint = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true).map { it as GroupListDataWrapper.InstanceData }.getHint()

                        actionMode.finish()

                        hint
                    } else {
                        EditActivity.Hint.Schedule(rangePositionToDate(parameters.timeRange, parameters.position))
                    }
                }
                is GroupListParameters.TimeStamp -> parameters.groupListDataWrapper
                        .instanceDatas
                        .getHint()
                is GroupListParameters.InstanceKey -> EditActivity.Hint.Task(parameters.instanceKey.taskKey)
                else -> throw IllegalStateException()
            }

            activity.startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))
        }

        updateFabVisibility()
    }

    private fun showFab() = when (val parameters = parameters) {
        is GroupListParameters.All -> {
            if (selectionCallback.hasActionMode) {
                val selectedDatas = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true)
                if (selectedDatas.all { it is GroupListDataWrapper.InstanceData }) {
                    selectedDatas.asSequence()
                            .map { it as GroupListDataWrapper.InstanceData }
                            .filter { it.isRootInstance }
                            .map { it.instanceTimeStamp }
                            .distinct()
                            .singleOrNull()
                            ?.takeIf { it > TimeStamp.now } != null
                } else {
                    false
                }
            } else {
                true
            }
        }
        is GroupListParameters.TimeStamp -> (parameters.timeStamp > TimeStamp.now) && !selectionCallback.hasActionMode
        is GroupListParameters.InstanceKey -> parameters.groupListDataWrapper.taskEditable!! && !selectionCallback.hasActionMode
        else -> false
    }

    private fun updateFabVisibility() {
        floatingActionButton?.apply {
            if (parametersRelay.hasValue() && showFab()) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        floatingActionButton = null
    }

    private fun getAllInstanceDatas(instanceData: GroupListDataWrapper.InstanceData): List<GroupListDataWrapper.InstanceData> {
        return listOf(listOf(listOf(instanceData)), instanceData.children.values.map { getAllInstanceDatas(it) }).flatten().flatten()
    }

    private val GroupListDataWrapper.InstanceData.allTaskKeys get() = getAllInstanceDatas(this).map { it.taskKey }

    override fun findItem(): Int? {
        if (!this::treeViewAdapter.isInitialized)
            return null

        return treeViewAdapter.displayedNodes
                .firstOrNull {
                    when (val modelNode = it.modelNode) {
                        is NotDoneGroupNode -> {
                            if (it.isExpanded) {
                                if (modelNode.singleInstance()) {
                                    modelNode.singleInstanceData.taskKey == scrollToTaskKey
                                } else {
                                    false
                                }
                            } else {
                                modelNode.instanceDatas
                                        .map { it.allTaskKeys }
                                        .flatten()
                                        .contains(scrollToTaskKey)
                            }
                        }
                        is NotDoneGroupNode.NotDoneInstanceNode -> {
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
                }?.let { treeViewAdapter.getTreeNodeCollection().getPosition(it) }
    }

    private fun onEditInstances() {
        selectionCallback.actionMode!!.finish()
    }

    class GroupAdapter(val groupListFragment: GroupListFragment) : GroupHolderAdapter(), NodeCollectionParent {

        companion object {

            const val TYPE_GROUP = 0
            const val TYPE_IMAGE = 1
        }

        val treeViewAdapter = TreeViewAdapter(this, Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress))

        public override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
            private set

        private lateinit var nodeCollection: NodeCollection

        val groupListState: GroupListState
            get() {
                val expandedGroups = nodeCollection.expandedGroups

                val expandedInstances = mutableMapOf<InstanceKey, Boolean>()
                nodeCollection.addExpandedInstances(expandedInstances)

                val doneExpanded = nodeCollection.doneExpanded
                val unscheduledExpanded = nodeCollection.unscheduledExpanded
                val expandedTaskKeys = nodeCollection.expandedTaskKeys

                val selectedNodes = treeViewAdapter.selectedNodes
                val selectedDatas = nodesToSelectedDatas(selectedNodes, false)
                val selectedInstances = selectedDatas.filterIsInstance<GroupListDataWrapper.InstanceData>().map { it.instanceKey }
                val selectedTasks = selectedDatas.filterIsInstance<GroupListDataWrapper.TaskData>().map { it.taskKey }

                val selectedGroups = selectedNodes.map { it.modelNode }
                        .filterIsInstance<NotDoneGroupNode>().filterNot { it.singleInstance() }
                        .map { it.exactTimeStamp.long }

                return GroupListState(
                        doneExpanded,
                        expandedGroups,
                        expandedInstances,
                        unscheduledExpanded,
                        expandedTaskKeys,
                        selectedInstances,
                        selectedGroups,
                        selectedTasks
                )
            }

        var dataId = -1
            private set

        lateinit var customTimeDatas: List<GroupListDataWrapper.CustomTimeData>
            private set

        fun initialize(
                dataId: Int,
                customTimeDatas: List<GroupListDataWrapper.CustomTimeData>,
                useGroups: Boolean,
                instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
                groupListState: GroupListState,
                taskDatas: List<GroupListDataWrapper.TaskData>,
                note: String?,
                imageState: ImageState?,
                showProgress: Boolean,
                useDoneNode: Boolean
        ) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            nodeCollection = NodeCollection(
                    0,
                    this,
                    useGroups,
                    treeNodeCollection,
                    note,
                    useDoneNode
            )

            treeNodeCollection.nodes = nodeCollection.initialize(
                    instanceDatas,
                    groupListState.expandedGroups,
                    groupListState.expandedInstances,
                    groupListState.doneExpanded,
                    groupListState.selectedInstances,
                    groupListState.selectedGroups,
                    taskDatas,
                    groupListState.unscheduledExpanded,
                    groupListState.expandedTaskKeys,
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
                                groupListFragment.showImage)
                    }
            )

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)
            treeViewAdapter.showProgress = showProgress

            groupListFragment.showImage = false
        }

        override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
        ) = NodeHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_list, parent, false))

        override val hasActionMode get() = groupListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) =
                groupListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) =
                groupListFragment.selectionCallback.decrementSelected(x)

        override val groupAdapter = this

        override fun scrollToTop() = groupListFragment.scrollToTop()
    }

    private class NoSelectionException(message: String) : Exception(message)
}