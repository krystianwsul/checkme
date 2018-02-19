package com.krystianwsul.checkme.gui.instances.tree

import android.content.Context
import android.os.Bundle
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
import android.widget.*
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.SelectionCallback
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
import junit.framework.Assert
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

class GroupListFragment : AbstractFragment(), FabUser {

    companion object {

        private val EXPANSION_STATE_KEY = "expansionState"
        private val SELECTED_NODES_KEY = "selectedNodes"

        fun newInstance() = GroupListFragment()

        private fun rangePositionToDate(timeRange: MainActivity.TimeRange, position: Int): Date {
            Assert.assertTrue(position >= 0)

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
                Assert.assertTrue(instanceData.instanceDataParent is DataWrapper)
            }
        }
    }

    private var mGroupListProgress: ProgressBar? = null
    private var mGroupListRecycler: RecyclerView? = null
    private var mTreeViewAdapter: TreeViewAdapter? = null
    private var mEmptyText: TextView? = null

    private var mPosition: Int? = null
    private var mTimeRange: MainActivity.TimeRange? = null
    private var mTimeStamp: TimeStamp? = null
    private var mInstanceKey: InstanceKey? = null
    private var mInstanceKeys: Set<InstanceKey>? = null

    private var mExpansionState: ExpansionState? = null
    private var mSelectedNodes: ArrayList<InstanceKey>? = null

    private var mDataId: Int? = null

    private var mDataWrapper: DataWrapper? = null

    val mSelectionCallback: SelectionCallback = object : SelectionCallback() {

        override fun unselect() {
            mTreeViewAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            Assert.assertTrue(mTreeViewAdapter != null)

            val treeNodes = mTreeViewAdapter!!.selectedNodes

            val instanceDatas = nodesToInstanceDatas(treeNodes)
            Assert.assertTrue(instanceDatas.isNotEmpty())

            when (menuItem.itemId) {
                R.id.action_group_edit_instance -> {
                    Assert.assertTrue(instanceDatas.isNotEmpty())

                    if (instanceDatas.size == 1) {
                        val instanceData = instanceDatas[0]
                        Assert.assertTrue(instanceData.IsRootInstance)

                        startActivity(EditInstanceActivity.getIntent(instanceData.InstanceKey))
                    } else {
                        Assert.assertTrue(instanceDatas.size > 1)
                        Assert.assertTrue(instanceDatas.all { it.IsRootInstance })

                        val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

                        startActivity(EditInstancesActivity.getIntent(instanceKeys))
                    }
                }
                R.id.action_group_share -> Utils.share(getShareData(instanceDatas))
                R.id.action_group_show_task -> {
                    Assert.assertTrue(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    Assert.assertTrue(instanceData.TaskCurrent)

                    startActivity(ShowTaskActivity.newIntent(instanceData.InstanceKey.mTaskKey))
                }
                R.id.action_group_edit_task -> {
                    Assert.assertTrue(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    Assert.assertTrue(instanceData.TaskCurrent)

                    startActivity(CreateTaskActivity.getEditIntent(instanceData.InstanceKey.mTaskKey))
                }
                R.id.action_group_delete_task -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.mTaskKey })
                    Assert.assertTrue(taskKeys.isNotEmpty())
                    Assert.assertTrue(instanceDatas.all { it.TaskCurrent })

                    var selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
                    Assert.assertTrue(selectedTreeNodes.isNotEmpty())

                    do {
                        val treeNode = selectedTreeNodes[0]
                        Assert.assertTrue(treeNode != null)

                        recursiveDelete(treeNode, true)

                        decrementSelected()
                        selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    DomainFactory.getDomainFactory().setTaskEndTimeStamps(activity!!, (mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).mDataId, SaveService.Source.GUI, taskKeys)

                    updateSelectAll()
                }
                R.id.action_group_add_task -> {
                    Assert.assertTrue(instanceDatas.size == 1)

                    val instanceData = instanceDatas[0]
                    Assert.assertTrue(instanceData.TaskCurrent)

                    activity!!.startActivity(CreateTaskActivity.getCreateIntent(instanceData.InstanceKey.mTaskKey))
                }
                R.id.action_group_join -> {
                    val taskKeys = ArrayList(instanceDatas.map { it.InstanceKey.mTaskKey })
                    Assert.assertTrue(taskKeys.size > 1)

                    if (mInstanceKey == null) {
                        val firstInstanceData = instanceDatas.minBy { it.InstanceTimeStamp }!!

                        val date = firstInstanceData.InstanceTimeStamp.date

                        val timePair = firstInstanceData.InstanceTimePair

                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, CreateTaskActivity.ScheduleHint(date, timePair)))
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, mInstanceKey!!.mTaskKey))
                    }
                }
                R.id.action_group_mark_done -> {
                    Assert.assertTrue(mDataId != null)
                    Assert.assertTrue(mDataWrapper != null)

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = DomainFactory.getDomainFactory().setInstancesDone(activity!!, mDataId!!, SaveService.Source.GUI, instanceKeys)

                    var selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
                    Assert.assertTrue(selectedTreeNodes.isNotEmpty())

                    do {
                        Assert.assertTrue(selectedTreeNodes.isNotEmpty())

                        val treeNode = selectedTreeNodes.maxBy { it.indentation }!!

                        treeNode.modelNode.let {
                            if (it is NotDoneGroupNode) {
                                Assert.assertTrue(it.singleInstance())

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
                        selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
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
                        Assert.assertTrue(it is DividerNode)

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
            (activity as AppCompatActivity).startSupportActionMode(this)

            mTreeViewAdapter!!.onCreateActionMode()

            mActionMode.menuInflater.inflate(R.menu.menu_edit_groups, mActionMode.menu)

            updateFabVisibility()

            (activity as GroupListListener).onCreateGroupActionMode(mActionMode)

            updateMenu()
        }

        override fun onSecondAdded() {
            updateMenu()
        }

        override fun onOtherAdded() {
            updateMenu()
        }

        override fun onLastRemoved() {
            mTreeViewAdapter!!.onDestroyActionMode()

            updateFabVisibility()

            (activity as GroupListListener).onDestroyGroupActionMode()
        }

        override fun onSecondToLastRemoved() {
            updateMenu()
        }

        override fun onOtherRemoved() {
            updateMenu()
        }

        private fun updateMenu() {
            Assert.assertTrue(mActionMode != null)

            val menu = mActionMode.menu
            Assert.assertTrue(menu != null)

            val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)
            Assert.assertTrue(instanceDatas.isNotEmpty())

            Assert.assertTrue(instanceDatas.all { it.Done == null })

            if (instanceDatas.size == 1) {
                val instanceData = instanceDatas[0]

                menu!!.findItem(R.id.action_group_edit_instance).isVisible = instanceData.IsRootInstance
                menu.findItem(R.id.action_group_show_task).isVisible = instanceData.TaskCurrent
                menu.findItem(R.id.action_group_edit_task).isVisible = instanceData.TaskCurrent
                menu.findItem(R.id.action_group_join).isVisible = false
                menu.findItem(R.id.action_group_delete_task).isVisible = instanceData.TaskCurrent
                menu.findItem(R.id.action_group_add_task).isVisible = instanceData.TaskCurrent
            } else {
                Assert.assertTrue(instanceDatas.size > 1)

                menu!!.findItem(R.id.action_group_edit_instance).isVisible = instanceDatas.all { it.IsRootInstance }
                menu.findItem(R.id.action_group_show_task).isVisible = false
                menu.findItem(R.id.action_group_edit_task).isVisible = false
                menu.findItem(R.id.action_group_add_task).isVisible = false

                if (instanceDatas.all { it.TaskCurrent }) {
                    val projectIdCount = instanceDatas.map { it.InstanceKey.mTaskKey.mRemoteProjectId }
                            .distinct()
                            .count()

                    Assert.assertTrue(projectIdCount > 0)

                    menu.findItem(R.id.action_group_join).isVisible = (projectIdCount == 1)
                    menu.findItem(R.id.action_group_delete_task).isVisible = !containsLoop(instanceDatas)
                } else {
                    menu.findItem(R.id.action_group_join).isVisible = false
                    menu.findItem(R.id.action_group_delete_task).isVisible = false
                }
            }
        }

        private fun containsLoop(instanceDatas: List<InstanceData>): Boolean {
            Assert.assertTrue(instanceDatas.size > 1)

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

    private var mFloatingActionButton: FloatingActionButton? = null

    val shareData: String?
        get() {
            Assert.assertTrue(mDataWrapper != null)
            Assert.assertTrue(mDataId != null)

            val instanceDatas = ArrayList(mDataWrapper!!.instanceDatas.values)

            Collections.sort(instanceDatas) { lhs, rhs ->
                val timeStampComparison = lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp)
                if (timeStampComparison != 0) {
                    return@sort timeStampComparison
                } else {
                    return@sort lhs.mTaskStartExactTimeStamp.compareTo(rhs.mTaskStartExactTimeStamp)
                }
            }

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas)
                printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    private fun getShareData(instanceDatas: List<InstanceData>): String {
        Assert.assertTrue(instanceDatas.isNotEmpty())

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
                .sortedBy { it.mTaskStartExactTimeStamp }
                .forEach { printTree(lines, indentation + 1, it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        Assert.assertTrue(context is GroupListListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANSION_STATE_KEY)) {
            mExpansionState = savedInstanceState.getParcelable(EXPANSION_STATE_KEY)

            if (savedInstanceState.containsKey(SELECTED_NODES_KEY)) {
                mSelectedNodes = savedInstanceState.getParcelableArrayList(SELECTED_NODES_KEY)
                Assert.assertTrue(mSelectedNodes!!.isNotEmpty())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_list, container, false)!!

        mGroupListProgress = view.findViewById(R.id.group_list_progress)
        Assert.assertTrue(mGroupListProgress != null)

        mGroupListRecycler = view.findViewById(R.id.group_list_recycler)
        Assert.assertTrue(mGroupListRecycler != null)

        mGroupListRecycler!!.layoutManager = LinearLayoutManager(context)

        mEmptyText = view.findViewById(R.id.emptyText)
        Assert.assertTrue(mEmptyText != null)

        return view
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, dataWrapper: DataWrapper) {
        Assert.assertTrue(mPosition == null || mPosition == position)
        Assert.assertTrue(mTimeRange == null || mTimeRange == timeRange)
        Assert.assertTrue(mTimeStamp == null)
        Assert.assertTrue(mInstanceKey == null)
        Assert.assertTrue(mInstanceKeys == null)

        Assert.assertTrue(position >= 0)

        mPosition = position
        mTimeRange = timeRange

        initialize(dataId, dataWrapper)
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, dataWrapper: DataWrapper) {
        Assert.assertTrue(mPosition == null)
        Assert.assertTrue(mTimeRange == null)
        Assert.assertTrue(mTimeStamp == null || mTimeStamp == timeStamp)
        Assert.assertTrue(mInstanceKey == null)
        Assert.assertTrue(mInstanceKeys == null)

        mTimeStamp = timeStamp

        initialize(dataId, dataWrapper)
    }

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, dataWrapper: DataWrapper) {
        Assert.assertTrue(mPosition == null)
        Assert.assertTrue(mTimeRange == null)
        Assert.assertTrue(mTimeStamp == null)
        Assert.assertTrue(mInstanceKeys == null)

        mInstanceKey = instanceKey

        initialize(dataId, dataWrapper)
    }

    fun setInstanceKeys(instanceKeys: Set<InstanceKey>, dataId: Int, dataWrapper: DataWrapper) {
        Assert.assertTrue(mPosition == null)
        Assert.assertTrue(mTimeRange == null)
        Assert.assertTrue(mTimeStamp == null)
        Assert.assertTrue(mInstanceKey == null)

        mInstanceKeys = instanceKeys

        initialize(dataId, dataWrapper)
    }

    private fun useGroups(): Boolean {
        Assert.assertTrue(mPosition == null == (mTimeRange == null))
        return mPosition != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (mTreeViewAdapter != null) {
            outState.putParcelable(EXPANSION_STATE_KEY, (mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState)

            if (mSelectionCallback.hasActionMode()) {
                val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)
                Assert.assertTrue(instanceDatas.isNotEmpty())

                val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })
                Assert.assertTrue(instanceKeys.isNotEmpty())

                outState.putParcelableArrayList(SELECTED_NODES_KEY, instanceKeys)
            }
        }
    }

    private fun initialize(dataId: Int, dataWrapper: DataWrapper) {
        mGroupListProgress!!.visibility = View.GONE

        if (mDataWrapper != null) {
            Assert.assertTrue(mDataId != null)

            DataDiff.diffData(mDataWrapper!!, dataWrapper)
            Log.e("asdf", "difference w data:\n" + DataDiff.getDiff()!!)
        } else {
            Assert.assertTrue(mDataId == null)
        }

        mDataWrapper = dataWrapper
        mDataId = dataId

        if (mTreeViewAdapter != null) {
            mExpansionState = (mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState

            val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)

            val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

            mSelectedNodes = if (instanceKeys.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode())
                null
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode())
                instanceKeys
            }
        }

        val emptyTextId = when {
            mPosition != null -> {
                Assert.assertTrue(mTimeRange != null)

                Assert.assertTrue(mTimeStamp == null)
                Assert.assertTrue(mInstanceKey == null)
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                R.string.instances_empty_root
            }
            mTimeStamp != null -> {
                Assert.assertTrue(mInstanceKey == null)
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                null
            }
            mInstanceKey != null -> {
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable != null)

                if (mDataWrapper!!.TaskEditable!!) {
                    R.string.empty_child
                } else {
                    R.string.empty_disabled
                }
            }
            else -> {
                Assert.assertTrue(mInstanceKeys!!.isNotEmpty())
                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                null
            }
        }

        updateFabVisibility()

        mTreeViewAdapter = GroupAdapter.getAdapter(this, mDataId!!, mDataWrapper!!.CustomTimeDatas, useGroups(), showPadding(), mDataWrapper!!.instanceDatas.values, mExpansionState, mSelectedNodes, mDataWrapper!!.TaskDatas, mDataWrapper!!.mNote)

        mGroupListRecycler!!.adapter = mTreeViewAdapter

        mSelectionCallback.setSelected(mTreeViewAdapter!!.selectedNodes.size)

        if (mDataWrapper!!.instanceDatas.isEmpty() && mDataWrapper!!.mNote.isNullOrEmpty() && (mDataWrapper!!.TaskDatas == null || mDataWrapper!!.TaskDatas!!.isEmpty())) {
            mGroupListRecycler!!.visibility = View.GONE

            if (emptyTextId != null) {
                mEmptyText!!.visibility = View.VISIBLE
                mEmptyText!!.setText(emptyTextId)
            }
        } else {
            mGroupListRecycler!!.visibility = View.VISIBLE
            mEmptyText!!.visibility = View.GONE
        }

        updateSelectAll()
    }

    fun updateSelectAll() {
        Assert.assertTrue(mDataWrapper != null)
        Assert.assertTrue(mDataId != null)

        (activity as GroupListListener).setGroupSelectAllVisibility(mPosition, mDataWrapper!!.instanceDatas.values.any { it.Done == null })
    }

    fun selectAll() {
        mTreeViewAdapter!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        MyCrashlytics.log(javaClass.simpleName + ".setFab " + hashCode())
        mFloatingActionButton = floatingActionButton

        mFloatingActionButton!!.setOnClickListener {
            Assert.assertTrue(mDataWrapper != null)
            Assert.assertTrue(mInstanceKeys == null)

            Assert.assertTrue(activity != null) // todo how the fuck is this null?

            when {
                mPosition != null -> {
                    Assert.assertTrue(mTimeRange != null)

                    Assert.assertTrue(mTimeStamp == null)
                    Assert.assertTrue(mInstanceKey == null)

                    Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(rangePositionToDate(mTimeRange!!, mPosition!!))))
                }
                mTimeStamp != null -> {
                    Assert.assertTrue(mInstanceKey == null)

                    Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                    Assert.assertTrue(mTimeStamp!! > TimeStamp.getNow())

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(mTimeStamp!!.date, mTimeStamp!!.hourMinute)))
                }
                else -> {
                    Assert.assertTrue(mInstanceKey != null)

                    Assert.assertTrue(mDataWrapper!!.TaskEditable != null)

                    Assert.assertTrue(mDataWrapper!!.TaskEditable!!)

                    startActivity(CreateTaskActivity.getCreateIntent(mInstanceKey!!.mTaskKey))
                }
            }
        }

        updateFabVisibility()
    }

    private fun showPadding(): Boolean {
        Assert.assertTrue(mDataWrapper != null)

        when {
            mPosition != null -> {
                Assert.assertTrue(mTimeRange != null)

                Assert.assertTrue(mTimeStamp == null)
                Assert.assertTrue(mInstanceKey == null)
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                return true
            }
            mTimeStamp != null -> {
                Assert.assertTrue(mInstanceKey == null)
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                return (mTimeStamp!! > TimeStamp.getNow())
            }
            mInstanceKey != null -> {
                Assert.assertTrue(mInstanceKeys == null)

                Assert.assertTrue(mDataWrapper!!.TaskEditable != null)

                return mDataWrapper!!.TaskEditable!!
            }
            else -> {
                Assert.assertTrue(mInstanceKeys!!.isNotEmpty())
                Assert.assertTrue(mDataWrapper!!.TaskEditable == null)

                return false
            }
        }
    }

    private fun updateFabVisibility() {
        if (mFloatingActionButton == null)
            return

        if (mDataWrapper != null && !mSelectionCallback.hasActionMode() && showPadding()) {
            mFloatingActionButton!!.show()
        } else {
            mFloatingActionButton!!.hide()
        }
    }

    override fun clearFab() {
        MyCrashlytics.log(javaClass.simpleName + ".clearFab " + hashCode())
        if (mFloatingActionButton == null)
            return

        mFloatingActionButton!!.setOnClickListener(null)

        mFloatingActionButton = null
    }

    class GroupAdapter private constructor(val mGroupListFragment: GroupListFragment, val mDataId: Int, val mCustomTimeDatas: List<CustomTimeData>, private val mShowFab: Boolean) : TreeModelAdapter, NodeCollectionParent {

        companion object {

            val TYPE_GROUP = 0

            fun getAdapter(groupListFragment: GroupListFragment, dataId: Int, customTimeDatas: List<CustomTimeData>, useGroups: Boolean, showFab: Boolean, instanceDatas: Collection<InstanceData>, expansionState: GroupListFragment.ExpansionState?, selectedNodes: ArrayList<InstanceKey>?, taskDatas: List<TaskData>?, note: String?): TreeViewAdapter {
                val groupAdapter = GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab)

                return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes, taskDatas, note)
            }
        }

        private var mTreeViewAdapter: TreeViewAdapter? = null

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

        private fun initialize(useGroups: Boolean, instanceDatas: Collection<InstanceData>?, expansionState: GroupListFragment.ExpansionState?, selectedNodes: ArrayList<InstanceKey>?, taskDatas: List<TaskData>?, note: String?): TreeViewAdapter {
            Assert.assertTrue(instanceDatas != null)

            mTreeViewAdapter = TreeViewAdapter(this, if (mShowFab) R.layout.row_group_list_fab_padding else null)

            val treeNodeCollection = TreeNodeCollection(mTreeViewAdapter!!)

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

            treeNodeCollection.setNodes(mNodeCollection!!.initialize(instanceDatas!!, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTaskKeys))

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

        override fun hasActionMode() = mGroupListFragment.mSelectionCallback.hasActionMode()

        override fun incrementSelected() {
            mGroupListFragment.mSelectionCallback.incrementSelected()
        }

        override fun decrementSelected() {
            mGroupListFragment.mSelectionCallback.decrementSelected()
        }

        override fun getGroupAdapter() = this

        class GroupHolder(val mGroupRow: LinearLayout, val mGroupRowContainer: LinearLayout, val mGroupRowName: TextView, val mGroupRowDetails: TextView, val mGroupRowChildren: TextView, val mGroupRowExpand: ImageView, val mGroupRowCheckBox: CheckBox, val mGroupRowSeparator: View) : RecyclerView.ViewHolder(mGroupRow)
    }

    @PaperParcel
    class ExpansionState(val DoneExpanded: Boolean, val ExpandedGroups: List<TimeStamp>, val ExpandedInstances: HashMap<InstanceKey, Boolean>, val UnscheduledExpanded: Boolean, val ExpandedTaskKeys: List<TaskKey>?) : PaperParcelable {

        companion object {

            @JvmField
            val CREATOR = PaperParcelGroupListFragment_ExpansionState.CREATOR
        }
    }

    interface GroupListListener {

        fun onCreateGroupActionMode(actionMode: ActionMode)

        fun onDestroyGroupActionMode()

        fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean)
    }

    data class DataWrapper(val CustomTimeDatas: List<CustomTimeData>, val TaskEditable: Boolean?, val TaskDatas: List<TaskData>?, val mNote: String?, val instanceDatas: HashMap<InstanceKey, InstanceData>) : InstanceDataParent {

        override fun remove(instanceKey: InstanceKey) {
            Assert.assertTrue(instanceDatas.containsKey(instanceKey))

            instanceDatas.remove(instanceKey)
        }
    }

    data class InstanceData(var Done: ExactTimeStamp?, val InstanceKey: InstanceKey, val DisplayText: String?, val Name: String, val InstanceTimeStamp: TimeStamp, var TaskCurrent: Boolean, val IsRootInstance: Boolean, var IsRootTask: Boolean?, var Exists: Boolean, val InstanceTimePair: TimePair, val mNote: String?, val mTaskStartExactTimeStamp: ExactTimeStamp, val children: HashMap<InstanceKey, InstanceData>) : InstanceDataParent {

        lateinit var instanceDataParent: InstanceDataParent

        init {
            Assert.assertTrue(Name.isNotEmpty())
        }

        override fun remove(instanceKey: InstanceKey) {
            Assert.assertTrue(children.containsKey(instanceKey))

            children.remove(instanceKey)
        }
    }

    data class CustomTimeData(val Name: String, val HourMinutes: TreeMap<DayOfWeek, HourMinute>) {

        init {
            Assert.assertTrue(Name.isNotEmpty())
            Assert.assertTrue(HourMinutes.size == 7)
        }

    }

    interface InstanceDataParent {

        fun remove(instanceKey: InstanceKey)
    }

    data class TaskData(val mTaskKey: TaskKey, val Name: String, val Children: List<TaskData>, val mStartExactTimeStamp: ExactTimeStamp, val mNote: String?) {

        init {
            Assert.assertTrue(Name.isNotEmpty())
        }
    }
}