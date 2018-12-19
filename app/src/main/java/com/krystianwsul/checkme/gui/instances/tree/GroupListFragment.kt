package com.krystianwsul.checkme.gui.instances.tree

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

class GroupListFragment @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0, defStyleRes: Int = 0) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), FabUser {

    companion object {

        private const val SUPER_STATE_KEY = "superState"
        private const val EXPANSION_STATE_KEY = "expansionState"
        private const val LAYOUT_MANAGER_STATE = "layoutManagerState"

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

        private fun nodesToInstanceDatas(treeNodes: List<TreeNode>, includeGroups: Boolean): List<InstanceData> {
            val instanceDatas = ArrayList<InstanceData>()
            treeNodes.map { it.modelNode }.forEach {
                when (it) {
                    is NotDoneGroupNode -> {
                        if (includeGroups || it.singleInstance())
                            instanceDatas.addAll(it.instanceDatas)
                    }
                    is NotDoneGroupNode.NotDoneInstanceNode -> instanceDatas.add(it.instanceData)
                    is DoneInstanceNode -> instanceDatas.add(it.instanceData)
                    else -> throw IllegalArgumentException()
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
    val listener = context as GroupListListener

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private val parametersRelay = BehaviorRelay.create<Parameters>()
    val parameters get() = parametersRelay.value!!

    private var state = State()

    val dragHelper by lazy {
        DragHelper(object : DragHelper.MyCallback() {

            override fun getTreeViewAdapter() = treeViewAdapter

            override fun onSetNewItemPosition() = selectionCallback.actionMode!!.finish()
        })
    }

    val selectionCallback = object : SelectionCallback() {

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)

            return true
        }

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val treeNodes = treeViewAdapter.selectedNodes

            val instanceDatas = nodesToInstanceDatas(treeNodes, true)
            check(instanceDatas.isNotEmpty())

            when (itemId) {
                R.id.action_group_edit_instance -> {
                    check(instanceDatas.isNotEmpty())

                    if (instanceDatas.size == 1) {
                        val instanceData = instanceDatas.single()
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
                    val instanceData = instanceDatas.single()
                    check(instanceData.TaskCurrent)

                    activity.startActivity(ShowTaskActivity.newIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_edit_task -> {
                    val instanceData = instanceDatas.single()
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

                    DomainFactory.getKotlinDomainFactory().setTaskEndTimeStamps((treeViewAdapter.treeModelAdapter as GroupAdapter).dataId, SaveService.Source.GUI, taskKeys)

                    updateSelectAll()
                }
                R.id.action_group_add_task -> {
                    val instanceData = instanceDatas.single()
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
                    check(instanceDatas.all { it.Done == null })

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = DomainFactory.getKotlinDomainFactory().setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, true)

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
                R.id.action_group_mark_not_done -> {
                    check(instanceDatas.all { it.Done != null })

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    DomainFactory.getKotlinDomainFactory().setInstancesDone(parameters.dataId, SaveService.Source.GUI, instanceKeys, false)

                    var selectedTreeNodes = treeViewAdapter.selectedNodes
                    check(selectedTreeNodes.isNotEmpty())

                    do {
                        check(selectedTreeNodes.isNotEmpty())

                        val treeNode = selectedTreeNodes.maxBy { it.indentation }!!

                        treeNode.modelNode.let {
                            val instanceData = (it as DoneInstanceNode).instanceData
                            instanceData.Done = null

                            recursiveExists(instanceData)

                            it.removeFromParent(x)

                            it.dividerNode
                                    .nodeCollection
                                    .notDoneGroupCollection
                                    .add(instanceData, x)
                        }

                        decrementSelected(x)
                        selectedTreeNodes = treeViewAdapter.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
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
        }

        override fun onSecondAdded() = updateMenu()

        override fun onOtherAdded() = updateMenu()

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            listener.onDestroyGroupActionMode()
        }

        override fun onSecondToLastRemoved() = updateMenu()

        override fun onOtherRemoved() = updateMenu()

        private fun updateMenu() {
            checkNotNull(actionMode)

            val menu = actionMode!!.menu!!

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter.selectedNodes, true)
            check(instanceDatas.isNotEmpty())

            val modelNodes = treeViewAdapter.selectedNodes.map { it.modelNode }

            val allNotDone = modelNodes.all { (it is NotDoneGroupNode && it.singleInstance()) || it is NotDoneGroupNode.NotDoneInstanceNode }
            val allDone = modelNodes.all { it is DoneInstanceNode }

            menu.apply {
                findItem(R.id.action_group_mark_done).isVisible = allNotDone
                findItem(R.id.action_group_mark_not_done).isVisible = allDone
            }

            if (instanceDatas.size == 1) {
                val instanceData = instanceDatas.single()

                menu.apply {
                    findItem(R.id.action_group_edit_instance).isVisible = instanceData.let { it.IsRootInstance && it.Done == null }
                    findItem(R.id.action_group_show_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_edit_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_join).isVisible = false
                    findItem(R.id.action_group_delete_task).isVisible = instanceData.TaskCurrent
                    findItem(R.id.action_group_add_task).isVisible = instanceData.TaskCurrent
                }
            } else {
                check(instanceDatas.size > 1)

                menu.apply {
                    findItem(R.id.action_group_edit_instance).isVisible = instanceDatas.all { it.IsRootInstance && it.Done == null }
                    findItem(R.id.action_group_show_task).isVisible = false
                    findItem(R.id.action_group_edit_task).isVisible = false
                    findItem(R.id.action_group_add_task).isVisible = false

                    if (instanceDatas.all { it.TaskCurrent }) {
                        val projectIdCount = instanceDatas.asSequence()
                                .map { it.InstanceKey.taskKey.remoteProjectId }
                                .distinct()
                                .count()

                        check(projectIdCount > 0)

                        findItem(R.id.action_group_join).isVisible = (projectIdCount == 1)
                        findItem(R.id.action_group_delete_task).isVisible = !containsLoop(instanceDatas)
                    } else {
                        findItem(R.id.action_group_join).isVisible = false
                        findItem(R.id.action_group_delete_task).isVisible = false
                    }
                }
            }
        }

        private fun containsLoop(instanceDatas: Collection<InstanceData>): Boolean {
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

    private fun getShareData(instanceDatas: Collection<InstanceData>): String {
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
        lines.add("-".repeat(indentation) + instanceData.name)

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
                putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter.treeModelAdapter as GroupAdapter).state)
                Log.e("asdf", "save expansion" + (treeViewAdapter.treeModelAdapter as GroupAdapter).state)
            }

            putParcelable(LAYOUT_MANAGER_STATE, groupListRecycler.layoutManager!!.onSaveInstanceState())
        }
    }

    private fun initialize() {
        if (this::treeViewAdapter.isInitialized && (parameters as? Parameters.All)?.differentPage == false) {
            state = (treeViewAdapter.treeModelAdapter as GroupAdapter).state

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as GroupAdapter).initialize(parameters.dataId, parameters.dataWrapper.customTimeDatas, showPadding(), useGroups(), parameters.dataWrapper.instanceDatas.values, state, parameters.dataWrapper.taskDatas, parameters.dataWrapper.note)
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        } else {
            val groupAdapter = GroupAdapter(this)
            groupAdapter.initialize(parameters.dataId, parameters.dataWrapper.customTimeDatas, showPadding(), useGroups(), parameters.dataWrapper.instanceDatas.values, state, parameters.dataWrapper.taskDatas, parameters.dataWrapper.note)
            treeViewAdapter = groupAdapter.treeViewAdapter
            groupListRecycler.adapter = treeViewAdapter

            dragHelper.attachToRecyclerView(groupListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        }

        updateFabVisibility()

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

        if (parameters.dataWrapper.instanceDatas.isEmpty() && parameters.dataWrapper.note.isNullOrEmpty() && parameters.dataWrapper.taskDatas.isNullOrEmpty()) {
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

        floatingActionButton.setOnClickListener {
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
        is Parameters.InstanceKey -> parameters.dataWrapper.taskEditable!!
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
                val selectedInstances = nodesToInstanceDatas(selectedNodes, false).map { it.InstanceKey }
                val selectedGroups = selectedNodes.map { it.modelNode }
                        .filterIsInstance<NotDoneGroupNode>().filterNot { it.singleInstance() }
                        .map { it.exactTimeStamp.long }

                return State(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys, selectedInstances, selectedGroups)
            }

        var dataId = -1
            private set

        lateinit var customTimeDatas: List<CustomTimeData>
            private set

        fun initialize(dataId: Int, customTimeDatas: List<CustomTimeData>, showFab: Boolean, useGroups: Boolean, instanceDatas: Collection<InstanceData>, state: GroupListFragment.State, taskDatas: List<TaskData>, note: String?) {
            this.dataId = dataId
            this.customTimeDatas = customTimeDatas

            treeViewAdapter.showPadding = showFab
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            nodeCollection = NodeCollection(0, this, useGroups, treeNodeCollection, note)

            treeNodeCollection.nodes = nodeCollection.initialize(instanceDatas, state.expandedGroups, state.expandedInstances, state.doneExpanded, state.selectedInstances, state.selectedGroups, taskDatas, state.unscheduledExpanded, state.expandedTaskKeys)
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
            val selectedGroups: List<Long> = listOf()) : Parcelable

    interface GroupListListener {

        fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter)

        fun onDestroyGroupActionMode()

        fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean)

        val bottomActionModeId: Int
    }

    data class DataWrapper(
            val customTimeDatas: List<CustomTimeData>,
            val taskEditable: Boolean?,
            val taskDatas: List<TaskData>,
            val note: String?,
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
            val name: String,
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
            check(name.isNotEmpty())
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

    data class TaskData(val taskKey: TaskKey, val Name: String, val children: List<TaskData>, val mStartExactTimeStamp: ExactTimeStamp, val mNote: String?) {

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