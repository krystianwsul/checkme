package com.krystianwsul.checkme.gui.instances.tree

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
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
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_group_list.*
import java.util.*

class GroupListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val EXPANSION_STATE_KEY = "expansionState"
        private const val SELECTED_NODES_KEY = "selectedNodes"

        fun newInstance() = GroupListFragment()

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

    private var treeViewAdapter: TreeViewAdapter? = null

    private var position: Int? = null
    private var timeRange: MainActivity.TimeRange? = null
    private var timeStamp: TimeStamp? = null
    private var instanceKey: InstanceKey? = null

    var taskKey: TaskKey? = null
        private set

    var instanceKeys: Set<InstanceKey>? = null
        private set

    private var expansionState: ExpansionState? = null
    private var selectedNodes: ArrayList<InstanceKey>? = null

    var dataId: Int? = null
        private set

    var dataWrapper: DataWrapper? = null
        private set

    val dragHelper by lazy { DragHelper(treeViewAdapter!!) }

    val selectionCallback: SelectionCallback = object : SelectionCallback() {

        override fun unselect() {
            treeViewAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            check(treeViewAdapter != null)

            val treeNodes = treeViewAdapter!!.selectedNodes

            val instanceDatas = nodesToInstanceDatas(treeNodes)
            check(instanceDatas.isNotEmpty())

            when (menuItem.itemId) {
                R.id.action_group_edit_instance -> {
                    check(instanceDatas.isNotEmpty())

                    if (instanceDatas.size == 1) {
                        val instanceData = instanceDatas[0]
                        check(instanceData.IsRootInstance)

                        startActivity(EditInstanceActivity.getIntent(instanceData.InstanceKey))
                    } else {
                        check(instanceDatas.size > 1)
                        check(instanceDatas.all { it.IsRootInstance })

                        val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

                        startActivity(EditInstancesActivity.getIntent(instanceKeys))
                    }
                }
                R.id.action_group_share -> Utils.share(requireActivity(), getShareData(instanceDatas))
                R.id.action_group_show_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    startActivity(ShowTaskActivity.newIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_edit_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    startActivity(CreateTaskActivity.getEditIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.taskKey })
                    check(taskKeys.isNotEmpty())
                    check(instanceDatas.all { it.TaskCurrent })

                    var selectedTreeNodes = treeViewAdapter!!.selectedNodes
                    check(selectedTreeNodes.isNotEmpty())

                    do {
                        val treeNode = selectedTreeNodes[0]

                        recursiveDelete(treeNode, true)

                        decrementSelected()
                        selectedTreeNodes = treeViewAdapter!!.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    DomainFactory.getKotlinDomainFactory().setTaskEndTimeStamps((treeViewAdapter!!.treeModelAdapter as GroupAdapter).mDataId, SaveService.Source.GUI, taskKeys)

                    updateSelectAll()
                }
                R.id.action_group_add_task -> {
                    check(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    check(instanceData.TaskCurrent)

                    activity!!.startActivity(CreateTaskActivity.getCreateIntent(instanceData.InstanceKey.taskKey))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.taskKey })
                    check(taskKeys.size > 1)

                    if (instanceKey == null) {
                        val firstInstanceData = instanceDatas.minBy { it.InstanceTimeStamp }!!

                        val date = firstInstanceData.InstanceTimeStamp.date

                        val timePair = firstInstanceData.InstanceTimePair

                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, CreateTaskActivity.ScheduleHint(date, timePair)))
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, instanceKey!!.taskKey))
                    }
                }
                R.id.action_group_mark_done -> {
                    check(dataId != null)
                    check(dataWrapper != null)

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = DomainFactory.getKotlinDomainFactory().setInstancesDone(dataId!!, SaveService.Source.GUI, instanceKeys)

                    var selectedTreeNodes = treeViewAdapter!!.selectedNodes
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

                                nodeCollection.dividerNode.add(instanceData)
                                nodeCollection.notDoneGroupCollection.remove(it)
                            } else {
                                val instanceData = (it as NotDoneGroupNode.NotDoneInstanceNode).instanceData
                                instanceData.Done = done

                                recursiveExists(instanceData)

                                it.removeFromParent()

                                it.parentNodeCollection.dividerNode.add(instanceData)
                            }
                        }

                        decrementSelected()
                        selectedTreeNodes = treeViewAdapter!!.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    updateSelectAll()
                }
                else -> {
                    throw UnsupportedOperationException()
                }
            }
        }

        private fun recursiveDelete(treeNode: TreeNode, root: Boolean) {
            treeNode.modelNode.let {
                val instanceData1 = when (it) {
                    is NotDoneGroupNode -> it.singleInstanceData
                    is NotDoneGroupNode.NotDoneInstanceNode -> it.instanceData
                    is DoneInstanceNode -> it.instanceData
                    else -> {
                        check(it is DividerNode)

                        treeNode.allChildren.forEach { recursiveDelete(it, false) }

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
                    treeNode.unselect()

                    treeNode.update()

                    treeNode.allChildren.forEach { recursiveDelete(it, false) }
                } else {
                    when (it) {
                        is NotDoneGroupNode -> it.removeFromParent()
                        is NotDoneGroupNode.NotDoneInstanceNode -> it.removeFromParent()
                        else -> (it as DoneInstanceNode).removeFromParent()
                    }
                }
            }
        }

        override fun onFirstAdded() {
            treeViewAdapter!!.updateDisplayedNodes {
                (activity as AppCompatActivity).startSupportActionMode(this)
            }

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups, actionMode!!.menu)

            updateFabVisibility()

            (activity as GroupListListener).onCreateGroupActionMode(actionMode!!)

            updateMenu()

            dragHelper.attachToRecyclerView(groupListRecycler)
        }

        override fun onSecondAdded() {
            updateMenu()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onOtherAdded() {
            updateMenu()
        }

        override fun onLastRemoved(action: () -> Unit) {
            treeViewAdapter!!.updateDisplayedNodes(action)

            updateFabVisibility()

            (activity as GroupListListener).onDestroyGroupActionMode()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onSecondToLastRemoved() {
            updateMenu()

            dragHelper.attachToRecyclerView(groupListRecycler)
        }

        override fun onOtherRemoved() {
            updateMenu()
        }

        private fun updateMenu() {
            check(actionMode != null)

            val menu = actionMode!!.menu!!

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter!!.selectedNodes)
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
            check(dataWrapper != null)
            check(dataId != null)

            val instanceDatas = ArrayList(dataWrapper!!.instanceDatas.values)

            instanceDatas.sort()

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas)
                printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    private val dataRelay = PublishRelay.create<Pair<Int, DataWrapper>>()
    private val viewCreatedRelay = PublishRelay.create<Unit>()

    private fun getShareData(instanceDatas: List<InstanceData>): String {
        check(instanceDatas.isNotEmpty())

        val tree = LinkedHashMap<InstanceKey, InstanceData>()

        instanceDatas.filterNot { inTree(tree, it) }.forEach { tree[it.InstanceKey] = it }

        val lines = mutableListOf<String>()

        for (instanceData in tree.values)
            printTree(lines, 0, instanceData)

        return lines.joinToString("\n")
    }

    private fun inTree(shareTree: Map<InstanceKey, InstanceData>, instanceData: InstanceData): Boolean {
        if (shareTree.isEmpty())
            return false

        return if (shareTree.containsKey(instanceData.InstanceKey)) true else shareTree.values.any { inTree(it.children, instanceData) }
    }

    private fun printTree(lines: MutableList<String>, indentation: Int, instanceData: InstanceData) {
        lines.add("-".repeat(indentation) + instanceData.Name)

        instanceData.children
                .values
                .sorted()
                .forEach { printTree(lines, indentation + 1, it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        check(context is GroupListListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.takeIf { it.containsKey(EXPANSION_STATE_KEY) }?.apply {
            expansionState = getParcelable(EXPANSION_STATE_KEY)

            if (containsKey(SELECTED_NODES_KEY)) {
                selectedNodes = getParcelableArrayList(SELECTED_NODES_KEY)
                check(selectedNodes!!.isNotEmpty())
            }
        }

        Observables.combineLatest(dataRelay, viewCreatedRelay) { pair, _ -> pair }
                .subscribe { initialize(it.first, it.second) }
                .addTo(createDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_group_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupListRecycler.layoutManager = LinearLayoutManager(context)

        viewCreatedRelay.accept(Unit)
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, dataWrapper: DataWrapper) {
        check(this.position == null || this.position == position)
        check(this.timeRange == null || this.timeRange == timeRange)
        check(timeStamp == null)
        check(instanceKey == null)
        check(instanceKeys == null)
        check(taskKey == null)

        check(position >= 0)

        this.position = position
        this.timeRange = timeRange

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, dataWrapper: DataWrapper) {
        check(position == null)
        check(timeRange == null)
        check(this.timeStamp == null || this.timeStamp == timeStamp)
        check(instanceKey == null)
        check(instanceKeys == null)
        check(taskKey == null)

        this.timeStamp = timeStamp

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, dataWrapper: DataWrapper) {
        check(position == null)
        check(timeRange == null)
        check(timeStamp == null)
        check(instanceKeys == null)
        check(taskKey == null)

        this.instanceKey = instanceKey

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setInstanceKeys(instanceKeys: Set<InstanceKey>, dataId: Int, dataWrapper: DataWrapper) {
        check(position == null)
        check(timeRange == null)
        check(timeStamp == null)
        check(instanceKey == null)
        check(taskKey == null)

        this.instanceKeys = instanceKeys

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setTaskKey(taskKey: TaskKey, dataId: Int, dataWrapper: DataWrapper) {
        check(position == null)
        check(timeRange == null)
        check(timeStamp == null)
        check(instanceKey == null)
        check(this.taskKey == null)

        this.taskKey = taskKey

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    private fun useGroups(): Boolean {
        check(position == null == (timeRange == null))
        return position != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (treeViewAdapter != null) {
            outState.putParcelable(EXPANSION_STATE_KEY, (treeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState)

            if (selectionCallback.hasActionMode) {
                val instanceDatas = nodesToInstanceDatas(treeViewAdapter!!.selectedNodes)
                check(instanceDatas.isNotEmpty())

                val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })
                check(instanceKeys.isNotEmpty())

                outState.putParcelableArrayList(SELECTED_NODES_KEY, instanceKeys)
            }
        }
    }

    private fun initialize(dataId: Int, dataWrapper: DataWrapper) {
        groupListProgress.visibility = View.GONE

        if (this.dataWrapper != null) {
            check(this.dataId != null)

            DataDiff.diffData(this.dataWrapper!!, dataWrapper)
            Log.e("asdf", "difference w data:\n" + DataDiff.diff)
        } else {
            check(this.dataId == null)
        }

        this.dataWrapper = dataWrapper
        this.dataId = dataId

        if (treeViewAdapter != null) {
            expansionState = (treeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState

            val instanceDatas = nodesToInstanceDatas(treeViewAdapter!!.selectedNodes)

            val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

            selectedNodes = if (instanceKeys.isEmpty()) {
                check(!selectionCallback.hasActionMode)
                null
            } else {
                check(selectionCallback.hasActionMode)
                instanceKeys
            }
        }

        val emptyTextId = when {
            position != null -> {
                check(timeRange != null)

                check(timeStamp == null)
                check(instanceKey == null)
                check(instanceKeys == null)

                check(this.dataWrapper!!.TaskEditable == null)

                R.string.instances_empty_root
            }
            timeStamp != null -> {
                check(instanceKey == null)
                check(instanceKeys == null)

                check(this.dataWrapper!!.TaskEditable == null)

                null
            }
            instanceKey != null -> {
                check(instanceKeys == null)

                check(this.dataWrapper!!.TaskEditable != null)

                if (this.dataWrapper!!.TaskEditable!!) {
                    R.string.empty_child
                } else {
                    R.string.empty_disabled
                }
            }
            instanceKeys != null -> {
                check(instanceKeys!!.isNotEmpty())
                check(this.dataWrapper!!.TaskEditable == null)

                null
            }
            else -> {
                checkNotNull(taskKey)

                null
            }
        }

        updateFabVisibility()

        treeViewAdapter = GroupAdapter.getAdapter(this, this.dataId!!, this.dataWrapper!!.CustomTimeDatas, useGroups(), showPadding(), this.dataWrapper!!.instanceDatas.values, expansionState, selectedNodes, this.dataWrapper!!.TaskDatas, this.dataWrapper!!.mNote)

        groupListRecycler.adapter = treeViewAdapter

        selectionCallback.setSelected(treeViewAdapter!!.selectedNodes.size)

        if (this.dataWrapper!!.instanceDatas.isEmpty() && this.dataWrapper!!.mNote.isNullOrEmpty() && (this.dataWrapper!!.TaskDatas == null || this.dataWrapper!!.TaskDatas!!.isEmpty())) {
            groupListRecycler.visibility = View.GONE

            if (emptyTextId != null) {
                emptyText.visibility = View.VISIBLE
                emptyText.setText(emptyTextId)
            }
        } else {
            groupListRecycler.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

        updateSelectAll()
    }

    fun updateSelectAll() {
        check(dataWrapper != null)
        check(dataId != null)

        (activity as GroupListListener).setGroupSelectAllVisibility(position, dataWrapper!!.instanceDatas.values.any { it.Done == null })
    }

    fun selectAll() {
        treeViewAdapter!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        MyCrashlytics.log(javaClass.simpleName + ".setFab " + hashCode())
        this.floatingActionButton = floatingActionButton

        this.floatingActionButton!!.setOnClickListener {
            check(dataWrapper != null)
            check(instanceKeys == null)

            check(activity != null) // todo how the fuck is this null?

            when {
                position != null -> {
                    check(timeRange != null)

                    check(timeStamp == null)
                    check(instanceKey == null)

                    check(dataWrapper!!.TaskEditable == null)

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(rangePositionToDate(timeRange!!, position!!))))
                }
                timeStamp != null -> {
                    check(instanceKey == null)

                    check(dataWrapper!!.TaskEditable == null)

                    check(timeStamp!! > TimeStamp.now)

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(timeStamp!!.date, timeStamp!!.hourMinute)))
                }
                else -> {
                    check(instanceKey != null)

                    check(dataWrapper!!.TaskEditable != null)

                    check(dataWrapper!!.TaskEditable!!)

                    startActivity(CreateTaskActivity.getCreateIntent(instanceKey!!.taskKey))
                }
            }
        }

        updateFabVisibility()
    }

    private fun showPadding(): Boolean {
        check(dataWrapper != null)

        when {
            position != null -> {
                check(timeRange != null)

                check(timeStamp == null)
                check(instanceKey == null)
                check(instanceKeys == null)

                check(dataWrapper!!.TaskEditable == null)

                return true
            }
            timeStamp != null -> {
                check(instanceKey == null)
                check(instanceKeys == null)

                check(dataWrapper!!.TaskEditable == null)

                return (timeStamp!! > TimeStamp.now)
            }
            instanceKey != null -> {
                check(instanceKeys == null)

                check(dataWrapper!!.TaskEditable != null)

                return dataWrapper!!.TaskEditable!!
            }
            instanceKeys != null -> {
                check(instanceKeys!!.isNotEmpty())
                check(dataWrapper!!.TaskEditable == null)

                return false
            }
            else -> {
                check(taskKey != null)

                return false
            }
        }
    }

    private fun updateFabVisibility() {
        if (floatingActionButton == null)
            return

        if (dataWrapper != null && !selectionCallback.hasActionMode && showPadding()) {
            floatingActionButton!!.show()
        } else {
            floatingActionButton!!.hide()
        }
    }

    override fun clearFab() {
        MyCrashlytics.log(javaClass.simpleName + ".clearFab " + hashCode())
        if (floatingActionButton == null)
            return

        floatingActionButton!!.setOnClickListener(null)

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

        private var mTreeViewAdapter: TreeViewAdapter? = null

        lateinit var treeNodeCollection: TreeNodeCollection
            private set

        private var mNodeCollection: NodeCollection? = null

        private val mDensity = mGroupListFragment.activity!!.resources.displayMetrics.density

        val expansionState: ExpansionState
            get() {
                val expandedGroups = mNodeCollection!!.expandedGroups

                val expandedInstances = HashMap<InstanceKey, Boolean>()
                mNodeCollection!!.addExpandedInstances(expandedInstances)

                val doneExpanded = mNodeCollection!!.doneExpanded

                val unscheduledExpanded = mNodeCollection!!.unscheduledExpanded

                val expandedTaskKeys = mNodeCollection!!.expandedTaskKeys

                return ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys)
            }

        private fun initialize(useGroups: Boolean, instanceDatas: Collection<InstanceData>, expansionState: GroupListFragment.ExpansionState?, selectedNodes: ArrayList<InstanceKey>?, taskDatas: List<TaskData>?, note: String?): TreeViewAdapter {
            mTreeViewAdapter = TreeViewAdapter(this, if (mShowFab) R.layout.row_group_list_fab_padding else null)

            treeNodeCollection = TreeNodeCollection(mTreeViewAdapter!!)

            mNodeCollection = NodeCollection(mDensity, 0, this, useGroups, treeNodeCollection, note)

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

            treeNodeCollection.nodes = mNodeCollection!!.initialize(instanceDatas, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTaskKeys)

            mTreeViewAdapter!!.setTreeNodeCollection(treeNodeCollection)

            return mTreeViewAdapter!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val groupRow = LayoutInflater.from(parent.context).inflate(R.layout.row_group_list, parent, false) as LinearLayout

            val groupRowContainer = groupRow.findViewById<LinearLayout>(R.id.group_row_container)
            val groupRowName = groupRow.findViewById<TextView>(R.id.group_row_name)
            val groupRowDetails = groupRow.findViewById<TextView>(R.id.group_row_details)
            val groupRowChildren = groupRow.findViewById<TextView>(R.id.group_row_children)
            val groupRowExpand = groupRow.findViewById<ImageView>(R.id.group_row_expand)
            val groupCheckBox = groupRow.findViewById<CheckBox>(R.id.group_row_checkbox)
            val groupRowSeparator = groupRow.findViewById<View>(R.id.group_row_separator)

            return GroupHolder(groupRow, groupRowContainer, groupRowName, groupRowDetails, groupRowChildren, groupRowExpand, groupCheckBox, groupRowSeparator)
        }

        override val hasActionMode get() = mGroupListFragment.selectionCallback.hasActionMode

        override fun incrementSelected() {
            mGroupListFragment.selectionCallback.incrementSelected()
        }

        override fun decrementSelected() {
            mGroupListFragment.selectionCallback.decrementSelected()
        }

        override val groupAdapter = this

        class GroupHolder(
                val groupRow: LinearLayout,
                val groupRowContainer: LinearLayout,
                val groupRowName: TextView,
                val groupRowDetails: TextView,
                val groupRowChildren: TextView,
                val groupRowExpand: ImageView,
                val groupRowCheckBox: CheckBox,
                val groupRowSeparator: View) : RecyclerView.ViewHolder(groupRow)
    }

    @Parcelize
    class ExpansionState(
            val DoneExpanded: Boolean,
            val ExpandedGroups: List<TimeStamp>,
            val ExpandedInstances: HashMap<InstanceKey, Boolean>,
            val UnscheduledExpanded: Boolean,
            val ExpandedTaskKeys: List<TaskKey>?) : Parcelable

    interface GroupListListener {

        fun onCreateGroupActionMode(actionMode: ActionMode)

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
}