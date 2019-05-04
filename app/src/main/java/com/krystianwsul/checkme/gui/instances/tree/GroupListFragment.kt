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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.EditInstancesFragment
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.view.*
import kotlinx.android.synthetic.main.fragment_group_list.view.*
import java.util.*

class GroupListFragment @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), FabUser {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        const val EXPANSION_STATE_KEY = "expansionState"
        private const val LAYOUT_MANAGER_STATE = "layoutManagerState"
        private const val EDIT_INSTANCES_TAG = "editInstances"

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

            return Date(calendar)
        }

        private fun nodesToSelectedDatas(treeNodes: List<TreeNode>, includeGroups: Boolean): Set<SelectedData> {
            val instanceDatas = ArrayList<SelectedData>()
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

        fun recursiveExists(instanceData: InstanceData) {
            instanceData.exists = true

            if (instanceData.instanceDataParent is InstanceData) {
                recursiveExists(instanceData.instanceDataParent as InstanceData)
            } else {
                check(instanceData.instanceDataParent is DataWrapper)
            }
        }
    }

    val activity get() = context as AbstractActivity
    val listener get() = context as GroupListListener

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private val parametersRelay = BehaviorRelay.create<Parameters>()
    val parameters get() = parametersRelay.value!!

    private var state = State()

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

        override fun onMenuClick(@IdRes itemId: Int, x: TreeViewAdapter.Placeholder) {
            val treeNodes = treeViewAdapter.selectedNodes

            val selectedDatas = nodesToSelectedDatas(treeNodes, true)
            if (selectedDatas.isEmpty()) {
                MyCrashlytics.logException(NoSelectionException("menuItem.id: ${activity.normalizedId(itemId)}, selectedDatas: $selectedDatas, selectedNodes: $treeNodes"))
                return
            }

            when (itemId) {
                R.id.actionGroupHour -> {
                    check(showHour(selectedDatas))
                    val instanceKeys = selectedDatas.map { (it as InstanceData).instanceKey }

                    addHour(instanceKeys)
                }
                R.id.action_group_edit_instance -> {
                    check(selectedDatas.isNotEmpty())

                    val instanceDatas = selectedDatas.map { it as InstanceData }
                    check(instanceDatas.isNotEmpty())
                    check(instanceDatas.all { it.isRootInstance })

                    val instanceKeys = ArrayList(instanceDatas.map { it.instanceKey })

                    EditInstancesFragment.newInstance(instanceKeys).show(activity.supportFragmentManager, EDIT_INSTANCES_TAG)
                }
                R.id.action_group_share -> Utils.share(activity, getShareData(selectedDatas))
                R.id.action_group_show_task -> {
                    val instanceData = selectedDatas.single()
                    check(instanceData.taskCurrent)

                    activity.startActivity(ShowTaskActivity.newIntent(instanceData.taskKey))
                }
                R.id.action_group_edit_task -> {
                    val instanceData = selectedDatas.single()
                    check(instanceData.taskCurrent)

                    activity.startActivity(CreateTaskActivity.getEditIntent(instanceData.taskKey))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = selectedDatas.map { it.taskKey }
                    check(taskKeys.isNotEmpty())
                    check(selectedDatas.all { it.taskCurrent })

                    val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(0, SaveService.Source.GUI, taskKeys.toSet())

                    listener.showSnackbarRemoved(taskUndoData.taskKeys.size) {
                        DomainFactory.instance.clearTaskEndTimeStamps(0, SaveService.Source.GUI, taskUndoData)
                    }
                }
                R.id.action_group_add_task -> {
                    val instanceData = selectedDatas.single()
                    check(instanceData.taskCurrent)

                    activity.startActivity(CreateTaskActivity.getCreateIntent(instanceData.taskKey))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(selectedDatas.map { it.taskKey })
                    check(taskKeys.size > 1)

                    if (parameters is Parameters.InstanceKey) {
                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, (parameters as Parameters.InstanceKey).instanceKey.taskKey))
                    } else {
                        val scheduleHint = selectedDatas.filterIsInstance<InstanceData>()
                                .minBy { it.instanceTimeStamp }
                                ?.let {
                                    val date = it.instanceTimeStamp.date
                                    val timePair = it.instanceTimePair

                                    CreateTaskActivity.ScheduleHint(date, timePair)
                                }

                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, scheduleHint))
                    }
                }
                R.id.action_group_mark_done -> {
                    val instanceDatas = selectedDatas.map { it as InstanceData }

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
                    val instanceDatas = selectedDatas.map { it as InstanceData }

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
                    val instanceDatas = selectedDatas.map { it as InstanceData }

                    instanceDatas.all { it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown }

                    val instanceKeys = instanceDatas.map { it.instanceKey }

                    DomainFactory.instance.setInstancesNotNotified(parameters.dataId, SaveService.Source.GUI, instanceKeys)

                    instanceDatas.forEach { it.notificationShown = true }
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups_top, actionMode!!.menu)

            listener.onCreateGroupActionMode(actionMode!!, treeViewAdapter)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) = listener.onDestroyGroupActionMode()

        private fun showHour(selectedDatas: Collection<SelectedData>) = selectedDatas.all { it is InstanceData && it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            checkNotNull(actionMode)

            val selectedDatas = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true)
            check(selectedDatas.isNotEmpty())

            val itemVisibilities = mutableListOf(
                    R.id.action_group_notify to selectedDatas.all { it is InstanceData && it.isRootInstance && it.done == null && it.instanceTimeStamp <= TimeStamp.now && !it.notificationShown },
                    R.id.actionGroupHour to showHour(selectedDatas),
                    R.id.action_group_edit_instance to selectedDatas.all { it is InstanceData && it.isRootInstance && it.done == null },
                    R.id.action_group_mark_done to selectedDatas.all { it is InstanceData && it.done == null },
                    R.id.action_group_mark_not_done to selectedDatas.all { it is InstanceData && it.done != null }
            )

            if (selectedDatas.size == 1) {
                val instanceData = selectedDatas.single()

                itemVisibilities.addAll(listOf(
                        R.id.action_group_show_task to instanceData.taskCurrent,
                        R.id.action_group_edit_task to instanceData.taskCurrent,
                        R.id.action_group_join to false,
                        R.id.action_group_delete_task to instanceData.taskCurrent,
                        R.id.action_group_add_task to instanceData.taskCurrent
                ))
            } else {
                check(selectedDatas.size > 1)

                itemVisibilities.addAll(listOf(
                        R.id.action_group_show_task to false,
                        R.id.action_group_edit_task to false,
                        R.id.action_group_add_task to false
                ))

                if (selectedDatas.all { it.taskCurrent }) {
                    val projectIdCount = selectedDatas.asSequence()
                            .map { it.taskKey.remoteProjectId }
                            .distinct()
                            .count()

                    check(projectIdCount > 0)

                    itemVisibilities.addAll(listOf(
                            R.id.action_group_join to (projectIdCount == 1),
                            R.id.action_group_delete_task to true
                    ))
                } else {
                    itemVisibilities.addAll(listOf(
                            R.id.action_group_join to false,
                            R.id.action_group_delete_task to false
                    ))
                }
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
            val instanceDatas = parameters.dataWrapper
                    .instanceDatas
                    .values
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

    private fun getShareData(selectedDatas: Collection<SelectedData>): String {
        check(selectedDatas.isNotEmpty())

        val instanceDatas = selectedDatas.mapNotNull { it as? InstanceData }
        val taskDatas = selectedDatas.mapNotNull { it as? TaskData }

        val instanceMap = LinkedHashMap<InstanceKey, InstanceData>()
        val taskList = mutableListOf<TaskData>()

        instanceDatas.filterNot { inTree(instanceMap, it) }.forEach { instanceMap[it.instanceKey] = it }
        taskDatas.filterNot { inTree(taskList, it) }.forEach { taskList.add(it) }

        val lines = mutableListOf<String>()

        for (selectedData in instanceMap.values + taskList)
            printTree(lines, 0, selectedData)

        return lines.joinToString("\n")
    }

    private fun inTree(shareTree: Map<InstanceKey, InstanceData>, instanceData: InstanceData): Boolean = if (shareTree.containsKey(instanceData.instanceKey)) true else shareTree.values.any { inTree(it.children, instanceData) }

    private fun inTree(shareTree: List<TaskData>, childTaskData: TaskData): Boolean {
        if (shareTree.isEmpty())
            return false

        return if (shareTree.contains(childTaskData)) true else shareTree.any { inTree(it.children, childTaskData) }
    }

    private fun printTree(lines: MutableList<String>, indentation: Int, selectedData: SelectedData) {
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
                classLoader = State::class.java.classLoader
                if (containsKey(EXPANSION_STATE_KEY))
                    this@GroupListFragment.state = getParcelable(EXPANSION_STATE_KEY)!!

                groupListRecycler.layoutManager!!.onRestoreInstanceState(state.getParcelable(LAYOUT_MANAGER_STATE))
            }

            super.onRestoreInstanceState(state.getParcelable(SUPER_STATE_KEY))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val observable = activity.started.switchMap {
            if (it) {
                parametersRelay
            } else {
                Observable.never<Parameters>()
            }
        }

        compositeDisposable += observable.subscribe { initialize() }

        observable.reduce { one, two ->
            DataDiff.diffData(one.dataWrapper, two.dataWrapper)
            two
        }
                .subscribe()
                .addTo(compositeDisposable)

        activity.startTicks(receiver)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        activity.unregisterReceiver(receiver)

        super.onDetachedFromWindow()
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, immediate: Boolean, dataWrapper: DataWrapper) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? Parameters.All)?.let { it.timeRange != timeRange || it.position != position }
                ?: false

        parametersRelay.accept(Parameters.All(dataId, immediate, dataWrapper, position, timeRange, differentPage))
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, immediate: Boolean, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TimeStamp(dataId, immediate, dataWrapper, timeStamp))

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, immediate: Boolean, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKey(dataId, immediate, dataWrapper, instanceKey))

    fun setInstanceKeys(dataId: Int, immediate: Boolean, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKeys(dataId, immediate, dataWrapper))

    fun setTaskKey(taskKey: TaskKey, dataId: Int, immediate: Boolean, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TaskKey(dataId, immediate, dataWrapper, taskKey))

    private fun useGroups() = parameters is Parameters.All

    public override fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

            if (this@GroupListFragment::treeViewAdapter.isInitialized)
                putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter.treeModelAdapter as GroupAdapter).state)

            putParcelable(LAYOUT_MANAGER_STATE, groupListRecycler.layoutManager!!.onSaveInstanceState())
        }
    }

    private fun initialize() {
        if (this::treeViewAdapter.isInitialized && (parameters as? Parameters.All)?.differentPage != true) {
            state = (treeViewAdapter.treeModelAdapter as GroupAdapter).state

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as GroupAdapter).initialize(
                        parameters.dataId,
                        parameters.dataWrapper.customTimeDatas,
                        useGroups(),
                        parameters.dataWrapper
                                .instanceDatas
                                .values,
                        state,
                        parameters.dataWrapper.taskDatas,
                        parameters.dataWrapper.note,
                        parameters.dataWrapper.imageData)
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        } else {
            val groupAdapter = GroupAdapter(this)
            groupAdapter.initialize(
                    parameters.dataId,
                    parameters.dataWrapper.customTimeDatas,
                    useGroups(),
                    parameters.dataWrapper
                            .instanceDatas
                            .values,
                    state,
                    parameters.dataWrapper.taskDatas,
                    parameters.dataWrapper.note,
                    parameters.dataWrapper.imageData)
            treeViewAdapter = groupAdapter.treeViewAdapter
            groupListRecycler.adapter = treeViewAdapter

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
            is Parameters.All -> R.string.instances_empty_root
            is Parameters.InstanceKey -> if (parameters.dataWrapper.taskEditable!!) {
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
    }

    private fun setGroupMenuItemVisibility() {
        listener.apply {
            val position = (parametersRelay.value as? Parameters.All)?.position

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

            when (val parameters = parameters) {
                is Parameters.All -> {
                    val actionMode = selectionCallback.actionMode

                    if (actionMode != null) {
                        nodesToSelectedDatas(treeViewAdapter.selectedNodes, true).map { it as InstanceData }.let {
                            (it.firstOrNull { it.instanceTimePair.customTimeKey != null }
                                    ?: it.first()).let {
                                activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(it.instanceTimeStamp.date, it.instanceTimePair)))
                            }
                        }

                        actionMode.finish()
                    } else {
                        activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(rangePositionToDate(parameters.timeRange, parameters.position))))
                    }
                }
                is Parameters.TimeStamp -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(parameters.timeStamp.date, parameters.timeStamp.hourMinute)))
                is Parameters.InstanceKey -> activity.startActivity(CreateTaskActivity.getCreateIntent(parameters.instanceKey.taskKey))
                else -> throw IllegalStateException()
            }
        }

        updateFabVisibility()
    }

    private fun showFab() = when (val parameters = parameters) {
        is Parameters.All -> {
            if (selectionCallback.hasActionMode) {
                val selectedDatas = nodesToSelectedDatas(treeViewAdapter.selectedNodes, true)
                if (selectedDatas.all { it is InstanceData }) {
                    selectedDatas.asSequence()
                            .map { it as InstanceData }
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
        is Parameters.TimeStamp -> (parameters.timeStamp > TimeStamp.now) && !selectionCallback.hasActionMode
        is Parameters.InstanceKey -> parameters.dataWrapper.taskEditable!! && !selectionCallback.hasActionMode
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

    class GroupAdapter(val groupListFragment: GroupListFragment) : TreeModelAdapter, NodeCollectionParent {

        companion object {

            const val TYPE_GROUP = 0
        }

        val treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)

        lateinit var treeNodeCollection: TreeNodeCollection
            private set

        private lateinit var nodeCollection: NodeCollection

        val state: State
            get() {
                val expandedGroups = nodeCollection.expandedGroups

                val expandedInstances = mutableMapOf<InstanceKey, Boolean>()
                nodeCollection.addExpandedInstances(expandedInstances)

                val doneExpanded = nodeCollection.doneExpanded
                val unscheduledExpanded = nodeCollection.unscheduledExpanded
                val expandedTaskKeys = nodeCollection.expandedTaskKeys

                val selectedNodes = treeViewAdapter.selectedNodes
                val selectedDatas = nodesToSelectedDatas(selectedNodes, false)
                val selectedInstances = selectedDatas.filterIsInstance<InstanceData>().map { it.instanceKey }
                val selectedTasks = selectedDatas.filterIsInstance<TaskData>().map { it.taskKey }
                val selectedGroups = selectedNodes.map { it.modelNode }
                        .filterIsInstance<NotDoneGroupNode>().filterNot { it.singleInstance() }
                        .map { it.exactTimeStamp.long }

                return State(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys, selectedInstances, selectedGroups, selectedTasks)
            }

        var dataId = -1
            private set

        lateinit var customTimeDatas: List<CustomTimeData>
            private set

        fun initialize(
                dataId: Int,
                customTimeDatas: List<CustomTimeData>,
                useGroups: Boolean,
                instanceDatas: Collection<InstanceData>,
                state: State,
                taskDatas: List<TaskData>,
                note: String?,
                imageState: ImageState?) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            nodeCollection = NodeCollection(0, this, useGroups, treeNodeCollection, note)

            treeNodeCollection.nodes = nodeCollection.initialize(
                    instanceDatas,
                    state.expandedGroups,
                    state.expandedInstances,
                    state.doneExpanded,
                    state.selectedInstances,
                    state.selectedGroups,
                    taskDatas,
                    state.unscheduledExpanded,
                    state.expandedTaskKeys,
                    state.selectedTaskKeys,
                    imageState)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_list, parent, false))

        override val hasActionMode get() = groupListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = groupListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = groupListFragment.selectionCallback.decrementSelected(x)

        override val groupAdapter = this
    }

    @Parcelize
    data class State(
            val doneExpanded: Boolean = false,
            val expandedGroups: List<TimeStamp> = listOf(),
            val expandedInstances: Map<InstanceKey, Boolean> = mapOf(),
            val unscheduledExpanded: Boolean = false,
            val expandedTaskKeys: List<TaskKey> = listOf(),
            val selectedInstances: List<InstanceKey> = listOf(),
            val selectedGroups: List<Long> = listOf(),
            val selectedTaskKeys: List<TaskKey> = listOf()) : Parcelable

    interface GroupListListener : SnackbarListener {

        fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter)

        fun onDestroyGroupActionMode()

        fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }

    data class DataWrapper(
            val customTimeDatas: List<CustomTimeData>,
            val taskEditable: Boolean?,
            val taskDatas: List<TaskData>,
            val note: String?,
            val instanceDatas: MutableMap<InstanceKey, InstanceData>,
            val imageData: ImageState?) : InstanceDataParent

    interface SelectedData {

        val taskCurrent: Boolean
        val taskKey: TaskKey
        val name: String
        val note: String?
        val childSelectedDatas: Collection<SelectedData>
    }

    data class InstanceData(
            var done: ExactTimeStamp?,
            val instanceKey: InstanceKey,
            var displayText: String?,
            override val name: String,
            var instanceTimeStamp: TimeStamp,
            override var taskCurrent: Boolean,
            val isRootInstance: Boolean,
            var isRootTask: Boolean?,
            var exists: Boolean,
            val instanceTimePair: TimePair,
            override val note: String?,
            val children: MutableMap<InstanceKey, InstanceData>,
            val hierarchyData: HierarchyData?,
            var ordinal: Double,
            var notificationShown: Boolean) : InstanceDataParent, Comparable<InstanceData>, SelectedData {

        lateinit var instanceDataParent: InstanceDataParent

        init {
            check(name.isNotEmpty())
        }

        override fun compareTo(other: InstanceData): Int {
            val timeStampComparison = instanceTimeStamp.compareTo(other.instanceTimeStamp)
            if (timeStampComparison != 0)
                return timeStampComparison

            return if (hierarchyData != null) {
                checkNotNull(other.hierarchyData)

                hierarchyData.ordinal.compareTo(other.hierarchyData.ordinal)
            } else {
                check(other.hierarchyData == null)

                ordinal.compareTo(other.ordinal)
            }
        }

        override val taskKey = instanceKey.taskKey

        override val childSelectedDatas get() = children.values
    }

    data class CustomTimeData(val Name: String, val HourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            check(Name.isNotEmpty())
            check(HourMinutes.size == 7)
        }
    }

    interface InstanceDataParent

    data class TaskData(
            override val taskKey: TaskKey,
            override val name: String,
            val children: List<TaskData>,
            val startExactTimeStamp: ExactTimeStamp,
            override val note: String?) : SelectedData {

        init {
            check(name.isNotEmpty())
        }

        override val taskCurrent = true

        override val childSelectedDatas get() = children
    }

    sealed class Parameters {

        abstract val dataId: Int
        abstract val immediate: Boolean
        abstract val dataWrapper: DataWrapper

        class All(
                override val dataId: Int,
                override val immediate: Boolean,
                override val dataWrapper: DataWrapper,
                val position: Int,
                val timeRange: MainActivity.TimeRange,
                val differentPage: Boolean) : Parameters()

        class TimeStamp(
                override val dataId: Int,
                override val immediate: Boolean,
                override val dataWrapper: DataWrapper,
                val timeStamp: com.krystianwsul.checkme.utils.time.TimeStamp) : Parameters()

        class InstanceKey(
                override val dataId: Int,
                override val immediate: Boolean,
                override val dataWrapper: DataWrapper,
                val instanceKey: com.krystianwsul.checkme.utils.InstanceKey) : Parameters()

        class InstanceKeys(
                override val dataId: Int,
                override val immediate: Boolean,
                override val dataWrapper: DataWrapper) : Parameters()

        class TaskKey(
                override val dataId: Int,
                override val immediate: Boolean,
                override val dataWrapper: DataWrapper,
                val taskKey: com.krystianwsul.checkme.utils.TaskKey) : Parameters()
    }

    private class NoSelectionException(message: String) : Exception(message)
}