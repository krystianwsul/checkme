package com.krystianwsul.checkme.gui.instances.tree

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.EditInstanceActivity
import com.krystianwsul.checkme.gui.instances.EditInstancesActivity
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.view.*
import kotlinx.android.synthetic.main.fragment_group_list.view.*
import java.util.*

class GroupListFragment @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), FabUser {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        private const val EXPANSION_STATE_KEY = "expansionState"
        private const val SELECTED_NODES_KEY = "selectedNodes"

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

        private fun nodesToInstanceDatas(treeNodes: List<TreeNode>): List<InstanceData> {
            val instanceDatas = ArrayList<InstanceData>()
            for (treeNode in treeNodes) {
                treeNode.modelNode.let {
                    if (it is NotDoneGroupNode) {
                        instanceDatas.add(it.singleInstanceData)
                    } else {
                        instanceDatas.add((it as NotDoneGroupNode.NotDoneInstanceNode).instanceData)
                    }
                }
            }

            return instanceDatas
        }

        fun recursiveExists(instanceData: InstanceData) {
            instanceData.Exists = true

            if (instanceData.instanceDataParent is InstanceData) {
                recursiveExists(instanceData.instanceDataParent as InstanceData)
            } else {
                check(instanceData.instanceDataParent is DataWrapper)
            }
        }
    }

    val activity = context as AbstractActivity

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private val parametersRelay = BehaviorRelay.create<Parameters>()
    val parameters get() = parametersRelay.value!!

    private var expansionState: ExpansionState? = null
    private var selectedNodes: ArrayList<InstanceKey>? = null

    val dragHelper by lazy { DragHelper(treeViewAdapter) }

    val selectionCallback = object : SelectionCallback({ treeViewAdapter }) {

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(menuItem: MenuItem, x: TreeViewAdapter.Placeholder) {
            val treeNodes = treeViewAdapter.selectedNodes

            val instanceDatas = nodesToInstanceDatas(treeNodes)
            check(instanceDatas.isNotEmpty())

            when (menuItem.itemId) {
                R.id.action_group_edit_instance -> {
                    check(instanceDatas.isNotEmpty())

                    if (instanceDatas.size == 1) {
                        val instanceData = instanceDatas[0]
                        check(instanceData.IsRootInstance)

                        activity.startActivity(EditInstanceActivity.getIntent(instanceData.InstanceKey))
                    } else {
                        check(instanceDatas.size > 1)
                        check(instanceDatas.all { it.IsRootInstance })

                        val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

                        activity.startActivity(EditInstancesActivity.getIntent(instanceKeys))
                    }
                }
                R.id.action_group_share -> Utils.share(activity, getShareData(instanceDatas))
                R.id.action_group_show_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    activity.startActivity(ShowTaskActivity.newIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_edit_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    activity.startActivity(CreateTaskActivity.getEditIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.taskKey })
                    check(taskKeys.isNotEmpty())
                    check(instanceDatas.all { it.TaskCurrent })

                    var selectedTreeNodes = treeViewAdapter.selectedNodes
                    check(selectedTreeNodes.isNotEmpty())

                    do {
                        val treeNode = selectedTreeNodes.first()

                        recursiveDelete(treeNode, true, x)

                        decrementSelected(x)
                        selectedTreeNodes = treeViewAdapter.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    DomainFactory.getKotlinDomainFactory().setTaskEndTimeStamps((treeViewAdapter.treeModelAdapter as GroupAdapter).mDataId, SaveService.Source.GUI, taskKeys)

                    updateSelectAll()
                }
                R.id.action_group_add_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    activity.startActivity(CreateTaskActivity.getCreateIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.taskKey })
                    check(taskKeys.size > 1)

                    if (parameters is Parameters.InstanceKey) {
                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, (parameters as Parameters.InstanceKey).instanceKey.taskKey))
                    } else {
                        val firstInstanceData = instanceDatas.minBy { it.InstanceTimeStamp }!!

                        val date = firstInstanceData.InstanceTimeStamp.date

                        val timePair = firstInstanceData.InstanceTimePair

                        activity.startActivity(CreateTaskActivity.getJoinIntent(taskKeys, CreateTaskActivity.ScheduleHint(date, timePair)))
                    }
                }
                R.id.action_group_mark_done -> {
                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = DomainFactory.getKotlinDomainFactory().setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys)

                    var selectedTreeNodes = treeViewAdapter.selectedNodes
                    check(selectedTreeNodes.isNotEmpty())

                    do {
                        check(selectedTreeNodes.isNotEmpty())

                        val treeNode = selectedTreeNodes.maxBy { it.indentation }!!

                        treeNode.modelNode.let {
                            if (it is NotDoneGroupNode) {
                                check(it.singleInstance())

                                val instanceData = it.singleInstanceData
                                instanceData.Done = done

                                recursiveExists(instanceData)

                                val nodeCollection = it.nodeCollection

                                nodeCollection.dividerNode.add(instanceData, x)
                                nodeCollection.notDoneGroupCollection.remove(it, x)
                            } else {
                                val instanceData = (it as NotDoneGroupNode.NotDoneInstanceNode).instanceData
                                instanceData.Done = done

                                recursiveExists(instanceData)

                                it.removeFromParent(x)

                                it.parentNodeCollection.dividerNode.add(instanceData, x)
                            }
                        }

                        decrementSelected(x)
                        selectedTreeNodes = treeViewAdapter.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    updateSelectAll()
                }
                else -> {
                    throw UnsupportedOperationException()
                }
            }
        }

        private fun recursiveDelete(treeNode: TreeNode, root: Boolean, x: TreeViewAdapter.Placeholder) {
            treeNode.modelNode.let {
                val instanceData1 = when (it) {
                    is NotDoneGroupNode -> it.singleInstanceData
                    is NotDoneGroupNode.NotDoneInstanceNode -> it.instanceData
                    is DoneInstanceNode -> it.instanceData
                    else -> {
                        check(it is DividerNode)

                        treeNode.allChildren.forEach { recursiveDelete(it, false, x) }

                        return
                    }
                }

                if (instanceData1.Exists || !root) {
                    instanceData1.TaskCurrent = false
                    instanceData1.IsRootTask = null
                } else {
                    instanceData1.instanceDataParent.remove(instanceData1.InstanceKey)
                }

                if (instanceData1.Exists || !root) {
                    treeNode.unselect(x)

                    treeNode.allChildren.forEach { recursiveDelete(it, false, x) }
                } else {
                    when (it) {
                        is NotDoneGroupNode -> it.removeFromParent(x)
                        is NotDoneGroupNode.NotDoneInstanceNode -> it.removeFromParent(x)
                        else -> (it as DoneInstanceNode).removeFromParent(x)
                    }
                }
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups, actionMode!!.menu)

            updateFabVisibility()

            (activity as GroupListListener).onCreateGroupActionMode(actionMode!!, treeViewAdapter)

            updateMenu()

            dragHelper.attachToRecyclerView(groupListRecycler)
        }

        override fun onSecondAdded() {
            updateMenu()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onOtherAdded() = updateMenu()

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as GroupListListener).onDestroyGroupActionMode()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onSecondToLastRemoved() {
            updateMenu()

            dragHelper.attachToRecyclerView(groupListRecycler)
        }

        override fun onOtherRemoved() = updateMenu()

        private fun updateMenu() {
            checkNotNull(actionMode)

            val menu = actionMode!!.menu!!

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter.selectedNodes)
            check(instanceDatas.isNotEmpty())

            check(instanceDatas.all { it.Done == null })

            if (instanceDatas.size == 1) {
                val instanceData = instanceDatas.single()

                menu.apply {
                    findItem(R.id.action_group_edit_instance).isVisible = instanceData.IsRootInstance
                    findItem(R.id.action_group_show_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_edit_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_join).isVisible = false
                    findItem(R.id.action_group_delete_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_add_task).isVisible = instanceData.TaskCurrent
                }
            } else {
                check(instanceDatas.size > 1)

                menu.apply {
                    findItem(R.id.action_group_edit_instance).isVisible = instanceDatas.all { it.IsRootInstance }
                    findItem(R.id.action_group_show_task).isVisible = false
                    findItem(R.id.action_group_edit_task).isVisible = false
                    findItem(R.id.action_group_add_task).isVisible = false
                }

                if (instanceDatas.all { it.TaskCurrent }) {
                    val projectIdCount = instanceDatas.asSequence()
                            .map { it.InstanceKey.taskKey.remoteProjectId }
                            .distinct()
                            .count()

                    check(projectIdCount > 0)

                    menu.findItem(R.id.action_group_join).isVisible = (projectIdCount == 1)
                    menu.findItem(R.id.action_group_delete_task).isVisible = !containsLoop(instanceDatas)
                } else {
                    menu.findItem(R.id.action_group_join).isVisible = false
                    menu.findItem(R.id.action_group_delete_task).isVisible = false
                }
            }
        }

        private fun containsLoop(instanceDatas: List<InstanceData>): Boolean {
            check(instanceDatas.size > 1)

            for (instanceData in instanceDatas) {
                val parents = ArrayList<InstanceData>()
                addParents(parents, instanceData)

                parents.forEach {
                    if (instanceDatas.contains(it))
                        return true
                }
            }

            return false
        }

        private fun addParents(parents: MutableList<InstanceData>, instanceData: InstanceData) {
            if (instanceData.instanceDataParent !is InstanceData)
                return

            val parent = instanceData.instanceDataParent as InstanceData

            parents.add(parent)
            addParents(parents, parent)
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

    private fun getShareData(instanceDatas: List<InstanceData>): String {
        check(instanceDatas.isNotEmpty())

        val tree = LinkedHashMap<InstanceKey, InstanceData>()

        instanceDatas.filterNot { inTree(tree, it) }.forEach { tree[it.InstanceKey] = it }

        val lines = mutableListOf<String>()

        for (instanceData in tree.values)
            printTree(lines, 0, instanceData)

        return lines.joinToString("\n")
    }

    private fun inTree(shareTree: Map<InstanceKey, InstanceData>, instanceData: InstanceData): Boolean = if (shareTree.containsKey(instanceData.InstanceKey)) true else shareTree.values.any { inTree(it.children, instanceData) }

    private fun printTree(lines: MutableList<String>, indentation: Int, instanceData: InstanceData) {
        lines.add("-".repeat(indentation) + instanceData.Name)

        instanceData.children
                .values
                .sorted()
                .forEach { printTree(lines, indentation + 1, it) }
    }

    init {
        check(context is GroupListListener)

        inflate(context, R.layout.fragment_group_list, this)

        groupListRecycler.layoutManager = LinearLayoutManager(context)
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            state.takeIf { it.containsKey(EXPANSION_STATE_KEY) }?.apply {
                expansionState = getParcelable(EXPANSION_STATE_KEY)

                if (containsKey(SELECTED_NODES_KEY)) {
                    selectedNodes = getParcelableArrayList(SELECTED_NODES_KEY)
                    check(selectedNodes!!.isNotEmpty())
                }
            }
            super.onRestoreInstanceState(state.getParcelable(SUPER_STATE_KEY))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        Observables.combineLatest(parametersRelay, activity.onPostCreate) { _, _ -> Unit }
                .subscribe { initialize() }
                .addTo(compositeDisposable)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, dataWrapper: DataWrapper) {
        check(position >= 0)

        val differentPage = (parametersRelay.value as? Parameters.All)?.let { it.timeRange != timeRange || it.position != position }
                ?: false

        parametersRelay.accept(Parameters.All(dataId, dataWrapper, position, timeRange, differentPage))
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TimeStamp(dataId, dataWrapper, timeStamp))

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKey(dataId, dataWrapper, instanceKey))

    fun setInstanceKeys(dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.InstanceKeys(dataId, dataWrapper))

    fun setTaskKey(taskKey: TaskKey, dataId: Int, dataWrapper: DataWrapper) = parametersRelay.accept(Parameters.TaskKey(dataId, dataWrapper, taskKey))

    private fun useGroups() = parameters is Parameters.All

    public override fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

            if (this@GroupListFragment::treeViewAdapter.isInitialized) {
                putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter.treeModelAdapter as GroupAdapter).expansionState)

                if (selectionCallback.hasActionMode) {
                    val instanceDatas = nodesToInstanceDatas(treeViewAdapter.selectedNodes)
                    check(instanceDatas.isNotEmpty())

                    val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })
                    check(instanceKeys.isNotEmpty())

                    putParcelableArrayList(SELECTED_NODES_KEY, instanceKeys)
                }
            }
        }
    }

    private fun initialize() {
        if (this::treeViewAdapter.isInitialized && (parameters as? Parameters.All)?.differentPage == false) {
            expansionState = (treeViewAdapter.treeModelAdapter as GroupAdapter).expansionState

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter.selectedNodes)

            val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

            selectedNodes = if (instanceKeys.isEmpty()) {
                check(!selectionCallback.hasActionMode)
                null
            } else {
                check(selectionCallback.hasActionMode)
                instanceKeys
            }
        }

        treeViewAdapter = GroupAdapter.getAdapter(this, parameters.dataId, parameters.dataWrapper.CustomTimeDatas, useGroups(), showPadding(), parameters.dataWrapper.instanceDatas.values, expansionState, selectedNodes, parameters.dataWrapper.TaskDatas, parameters.dataWrapper.mNote)
        groupListRecycler.adapter = treeViewAdapter

        treeViewAdapter.updateDisplayedNodes {
            selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
        }

        updateFabVisibility()

        val emptyTextId = when (val parameters = parameters) {
            is Parameters.All -> R.string.instances_empty_root
            is Parameters.InstanceKey -> if (parameters.dataWrapper.TaskEditable!!) {
                R.string.empty_child
            } else {
                R.string.empty_disabled
            }
            else -> null
        }

        val hide = mutableListOf<View>(groupListProgress)
        val show = mutableListOf<View>()

        if (parameters.dataWrapper.instanceDatas.isEmpty() && parameters.dataWrapper.mNote.isNullOrEmpty() && parameters.dataWrapper.TaskDatas.isNullOrEmpty()) {
            hide.add(groupListRecycler)

            if (emptyTextId != null) {
                show.add(emptyText)
                emptyText.setText(emptyTextId)
            } else {
                hide.add(emptyText)
            }
        } else {
            show.add(groupListRecycler)
            hide.add(emptyText)
        }

        animateVisibility(show, hide)

        updateSelectAll()
    }

    fun updateSelectAll() = (activity as GroupListListener).setGroupSelectAllVisibility((parameters as? Parameters.All)?.position, parameters.dataWrapper.instanceDatas.values.any { it.Done == null })

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        this.floatingActionButton = floatingActionButton

        this.floatingActionButton!!.setOnClickListener {
            checkNotNull(activity) // todo how the fuck is this null?

            when (val parameters = parameters) {
                is Parameters.All -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(rangePositionToDate(parameters.timeRange, parameters.position))))
                is Parameters.TimeStamp -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, CreateTaskActivity.ScheduleHint(parameters.timeStamp.date, parameters.timeStamp.hourMinute)))
                is Parameters.InstanceKey -> activity.startActivity(CreateTaskActivity.getCreateIntent(parameters.instanceKey.taskKey))
                else -> throw IllegalStateException()
            }
        }

        updateFabVisibility()
    }

    private fun showPadding() = when (val parameters = parameters) {
        is Parameters.All -> true
        is Parameters.TimeStamp -> parameters.timeStamp > TimeStamp.now
        is Parameters.InstanceKey -> parameters.dataWrapper.TaskEditable!!
        else -> false
    }

    private fun updateFabVisibility() {
        floatingActionButton?.apply {
            if (parametersRelay.hasValue() && !selectionCallback.hasActionMode && showPadding()) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        floatingActionButton?.setOnClickListener(null)
        floatingActionButton = null
    }

    class GroupAdapter private constructor(val mGroupListFragment: GroupListFragment, val mDataId: Int, val mCustomTimeDatas: List<CustomTimeData>, private val mShowFab: Boolean) : TreeModelAdapter, NodeCollectionParent {

        companion object {

            const val TYPE_GROUP = 0

            fun getAdapter(groupListFragment: GroupListFragment, dataId: Int, customTimeDatas: List<CustomTimeData>, useGroups: Boolean, showFab: Boolean, instanceDatas: Collection<InstanceData>, expansionState: GroupListFragment.ExpansionState?, selectedNodes: ArrayList<InstanceKey>?, taskDatas: List<TaskData>?, note: String?): TreeViewAdapter {
                val groupAdapter = GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab)

                return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes, taskDatas, note)
            }
        }

        private var treeViewAdapter: TreeViewAdapter? = null

        lateinit var treeNodeCollection: TreeNodeCollection
            private set

        private var nodeCollection: NodeCollection? = null

        private val mDensity = mGroupListFragment.activity.resources.displayMetrics.density

        val expansionState: ExpansionState
            get() {
                val expandedGroups = nodeCollection!!.expandedGroups

                val expandedInstances = HashMap<InstanceKey, Boolean>()
                nodeCollection!!.addExpandedInstances(expandedInstances)

                val doneExpanded = nodeCollection!!.doneExpanded

                val unscheduledExpanded = nodeCollection!!.unscheduledExpanded

                val expandedTaskKeys = nodeCollection!!.expandedTaskKeys

                return ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys)
            }

        private fun initialize(useGroups: Boolean, instanceDatas: Collection<InstanceData>, expansionState: GroupListFragment.ExpansionState?, selectedNodes: ArrayList<InstanceKey>?, taskDatas: List<TaskData>?, note: String?): TreeViewAdapter {
            treeViewAdapter = TreeViewAdapter(this, if (mShowFab) R.layout.row_group_list_fab_padding else null)
            treeNodeCollection = TreeNodeCollection(treeViewAdapter!!)

            nodeCollection = NodeCollection(mDensity, 0, this, useGroups, treeNodeCollection, note)

            var expandedGroups: List<TimeStamp>? = null
            var expandedInstances: HashMap<InstanceKey, Boolean>? = null
            var doneExpanded = false
            var unscheduledExpanded = false
            var expandedTaskKeys: List<TaskKey>? = null

            if (expansionState != null) {
                expandedGroups = expansionState.ExpandedGroups

                expandedInstances = expansionState.ExpandedInstances

                doneExpanded = expansionState.DoneExpanded

                unscheduledExpanded = expansionState.UnscheduledExpanded

                expandedTaskKeys = expansionState.ExpandedTaskKeys
            } else if (taskDatas != null) {
                unscheduledExpanded = false
            }

            treeNodeCollection.nodes = nodeCollection!!.initialize(instanceDatas, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTaskKeys)
            treeViewAdapter!!.setTreeNodeCollection(treeNodeCollection)

            return treeViewAdapter!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_list, parent, false))

        override val hasActionMode get() = mGroupListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = mGroupListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = mGroupListFragment.selectionCallback.decrementSelected(x)

        override val groupAdapter = this

    }

    @Parcelize
    class ExpansionState(
            val DoneExpanded: Boolean,
            val ExpandedGroups: List<TimeStamp>,
            val ExpandedInstances: HashMap<InstanceKey, Boolean>,
            val UnscheduledExpanded: Boolean,
            val ExpandedTaskKeys: List<TaskKey>?) : Parcelable

    interface GroupListListener {

        fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter)

        fun onDestroyGroupActionMode()

        fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean)
    }

    data class DataWrapper(
            val CustomTimeDatas: List<CustomTimeData>,
            val TaskEditable: Boolean?,
            val TaskDatas: List<TaskData>?,
            val mNote: String?,
            val instanceDatas: MutableMap<InstanceKey, InstanceData>) : InstanceDataParent {

        override fun remove(instanceKey: InstanceKey) {
            check(instanceDatas.containsKey(instanceKey))

            instanceDatas.remove(instanceKey)
        }
    }

    data class InstanceData(
            var Done: ExactTimeStamp?,
            val InstanceKey: InstanceKey,
            val DisplayText: String?,
            val Name: String,
            val InstanceTimeStamp: TimeStamp,
            var TaskCurrent: Boolean,
            val IsRootInstance: Boolean,
            var IsRootTask: Boolean?,
            var Exists: Boolean,
            val InstanceTimePair: TimePair,
            val mNote: String?,
            val children: MutableMap<InstanceKey, InstanceData>,
            val hierarchyData: HierarchyData?,
            var ordinal: Double) : InstanceDataParent, Comparable<InstanceData> {

        lateinit var instanceDataParent: InstanceDataParent

        init {
            check(Name.isNotEmpty())
        }

        override fun remove(instanceKey: InstanceKey) {
            check(children.containsKey(instanceKey))

            children.remove(instanceKey)
        }

        override fun compareTo(other: InstanceData): Int {
            val timeStampComparison = InstanceTimeStamp.compareTo(other.InstanceTimeStamp)
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
    }

    data class CustomTimeData(val Name: String, val HourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            check(Name.isNotEmpty())
            check(HourMinutes.size == 7)
        }
    }

    interface InstanceDataParent {

        fun remove(instanceKey: InstanceKey)
    }

    data class TaskData(val mTaskKey: TaskKey, val Name: String, val Children: List<TaskData>, val mStartExactTimeStamp: ExactTimeStamp, val mNote: String?) {

        init {
            check(Name.isNotEmpty())
        }
    }

    sealed class Parameters(var dataId: Int, var dataWrapper: DataWrapper) {

        class All(dataId: Int, dataWrapper: DataWrapper, val position: Int, val timeRange: MainActivity.TimeRange, val differentPage: Boolean) : Parameters(dataId, dataWrapper)

        class TimeStamp(dataId: Int, dataWrapper: DataWrapper, val timeStamp: com.krystianwsul.checkme.utils.time.TimeStamp) : Parameters(dataId, dataWrapper)

        class InstanceKey(dataId: Int, dataWrapper: DataWrapper, val instanceKey: com.krystianwsul.checkme.utils.InstanceKey) : Parameters(dataId, dataWrapper)

        class InstanceKeys(dataId: Int, dataWrapper: DataWrapper) : Parameters(dataId, dataWrapper)

        class TaskKey(dataId: Int, dataWrapper: DataWrapper, val taskKey: com.krystianwsul.checkme.utils.TaskKey) : Parameters(dataId, dataWrapper)
    }
}