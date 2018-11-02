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
import android.widget.*
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
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

    private var mGroupListProgress: ProgressBar? = null
    private var mGroupListRecycler: RecyclerView? = null
    private var mTreeViewAdapter: TreeViewAdapter? = null
    private var mEmptyText: TextView? = null

    private var mPosition: Int? = null
    private var mTimeRange: MainActivity.TimeRange? = null
    private var mTimeStamp: TimeStamp? = null
    private var mInstanceKey: InstanceKey? = null

    var taskKey: TaskKey? = null
        private set

    var mInstanceKeys: Set<InstanceKey>? = null
        private set

    private var mExpansionState: ExpansionState? = null
    private var mSelectedNodes: ArrayList<InstanceKey>? = null

    var mDataId: Int? = null
        private set

    var mDataWrapper: DataWrapper? = null
        private set

    val dragHelper by lazy { DragHelper(mTreeViewAdapter!!) }

    val mSelectionCallback: SelectionCallback = object : SelectionCallback() {

        override fun unselect() {
            mTreeViewAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            check(mTreeViewAdapter != null)

            val treeNodes = mTreeViewAdapter!!.selectedNodes

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

                    var selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
                    check(selectedTreeNodes.isNotEmpty())

                    do {
                        val treeNode = selectedTreeNodes[0]

                        recursiveDelete(treeNode, true)

                        decrementSelected()
                        selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
                    } while (selectedTreeNodes.isNotEmpty())

                    KotlinDomainFactory.getKotlinDomainFactory().domainFactory.setTaskEndTimeStamps((mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).mDataId, SaveService.Source.GUI, taskKeys)

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

                    if (mInstanceKey == null) {
                        val firstInstanceData = instanceDatas.minBy { it.InstanceTimeStamp }!!

                        val date = firstInstanceData.InstanceTimeStamp.date

                        val timePair = firstInstanceData.InstanceTimePair

                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, CreateTaskActivity.ScheduleHint(date, timePair)))
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(taskKeys, mInstanceKey!!.taskKey))
                    }
                }
                R.id.action_group_mark_done -> {
                    check(mDataId != null)
                    check(mDataWrapper != null)

                    val instanceKeys = instanceDatas.map { it.InstanceKey }

                    val done = KotlinDomainFactory.getKotlinDomainFactory().setInstancesDone(mDataId!!, SaveService.Source.GUI, instanceKeys)

                    var selectedTreeNodes = mTreeViewAdapter!!.selectedNodes
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
            mTreeViewAdapter!!.updateDisplayedNodes {
                (activity as AppCompatActivity).startSupportActionMode(this)
            }

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_groups, actionMode!!.menu)

            updateFabVisibility()

            (activity as GroupListListener).onCreateGroupActionMode(actionMode!!)

            updateMenu()

            dragHelper.attachToRecyclerView(mGroupListRecycler)
        }

        override fun onSecondAdded() {
            updateMenu()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onOtherAdded() {
            updateMenu()
        }

        override fun onLastRemoved(action: () -> Unit) {
            mTreeViewAdapter!!.updateDisplayedNodes(action)

            updateFabVisibility()

            (activity as GroupListListener).onDestroyGroupActionMode()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onSecondToLastRemoved() {
            updateMenu()

            dragHelper.attachToRecyclerView(mGroupListRecycler)
        }

        override fun onOtherRemoved() {
            updateMenu()
        }

        private fun updateMenu() {
            check(actionMode != null)

            val menu = actionMode!!.menu!!

            val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)
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

    private var mFloatingActionButton: FloatingActionButton? = null

    val shareData: String?
        get() {
            check(mDataWrapper != null)
            check(mDataId != null)

            val instanceDatas = ArrayList(mDataWrapper!!.instanceDatas.values)

            instanceDatas.sort()

            val lines = mutableListOf<String>()

            for (instanceData in instanceDatas)
                printTree(lines, 1, instanceData)

            return lines.joinToString("\n")
        }

    private val dataRelay = PublishRelay.create<Pair<Int, DataWrapper>>()!!
    private val viewCreatedRelay = PublishRelay.create<Unit>()!!

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

        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANSION_STATE_KEY)) {
            mExpansionState = savedInstanceState.getParcelable(EXPANSION_STATE_KEY)

            if (savedInstanceState.containsKey(SELECTED_NODES_KEY)) {
                mSelectedNodes = savedInstanceState.getParcelableArrayList(SELECTED_NODES_KEY)
                check(mSelectedNodes!!.isNotEmpty())
            }
        }

        Observables.combineLatest(dataRelay, viewCreatedRelay) { pair, _ -> pair }
                .subscribe { initialize(it.first, it.second) }
                .addTo(createDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_list, container, false)!!

        mGroupListProgress = view.findViewById(R.id.group_list_progress)
        check(mGroupListProgress != null)

        mGroupListRecycler = view.findViewById(R.id.group_list_recycler)
        check(mGroupListRecycler != null)

        mGroupListRecycler!!.layoutManager = LinearLayoutManager(context)

        mEmptyText = view.findViewById(R.id.emptyText)
        check(mEmptyText != null)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewCreatedRelay.accept(Unit)
    }

    fun setAll(timeRange: MainActivity.TimeRange, position: Int, dataId: Int, dataWrapper: DataWrapper) {
        check(mPosition == null || mPosition == position)
        check(mTimeRange == null || mTimeRange == timeRange)
        check(mTimeStamp == null)
        check(mInstanceKey == null)
        check(mInstanceKeys == null)
        check(taskKey == null)

        check(position >= 0)

        mPosition = position
        mTimeRange = timeRange

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setTimeStamp(timeStamp: TimeStamp, dataId: Int, dataWrapper: DataWrapper) {
        check(mPosition == null)
        check(mTimeRange == null)
        check(mTimeStamp == null || mTimeStamp == timeStamp)
        check(mInstanceKey == null)
        check(mInstanceKeys == null)
        check(taskKey == null)

        mTimeStamp = timeStamp

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setInstanceKey(instanceKey: InstanceKey, dataId: Int, dataWrapper: DataWrapper) {
        check(mPosition == null)
        check(mTimeRange == null)
        check(mTimeStamp == null)
        check(mInstanceKeys == null)
        check(taskKey == null)

        mInstanceKey = instanceKey

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setInstanceKeys(instanceKeys: Set<InstanceKey>, dataId: Int, dataWrapper: DataWrapper) {
        check(mPosition == null)
        check(mTimeRange == null)
        check(mTimeStamp == null)
        check(mInstanceKey == null)
        check(taskKey == null)

        mInstanceKeys = instanceKeys

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    fun setTaskKey(taskKey: TaskKey, dataId: Int, dataWrapper: DataWrapper) {
        check(mPosition == null)
        check(mTimeRange == null)
        check(mTimeStamp == null)
        check(mInstanceKey == null)
        check(this.taskKey == null)

        this.taskKey = taskKey

        dataRelay.accept(Pair(dataId, dataWrapper))
    }

    private fun useGroups(): Boolean {
        check(mPosition == null == (mTimeRange == null))
        return mPosition != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (mTreeViewAdapter != null) {
            outState.putParcelable(EXPANSION_STATE_KEY, (mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState)

            if (mSelectionCallback.hasActionMode) {
                val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)
                check(instanceDatas.isNotEmpty())

                val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })
                check(instanceKeys.isNotEmpty())

                outState.putParcelableArrayList(SELECTED_NODES_KEY, instanceKeys)
            }
        }
    }

    private fun initialize(dataId: Int, dataWrapper: DataWrapper) {
        mGroupListProgress!!.visibility = View.GONE

        if (mDataWrapper != null) {
            check(mDataId != null)

            DataDiff.diffData(mDataWrapper!!, dataWrapper)
            Log.e("asdf", "difference w data:\n" + DataDiff.diff)
        } else {
            check(mDataId == null)
        }

        mDataWrapper = dataWrapper
        mDataId = dataId

        if (mTreeViewAdapter != null) {
            mExpansionState = (mTreeViewAdapter!!.treeModelAdapter as GroupAdapter).expansionState

            val instanceDatas = nodesToInstanceDatas(mTreeViewAdapter!!.selectedNodes)

            val instanceKeys = ArrayList(instanceDatas.map { it.InstanceKey })

            mSelectedNodes = if (instanceKeys.isEmpty()) {
                check(!mSelectionCallback.hasActionMode)
                null
            } else {
                check(mSelectionCallback.hasActionMode)
                instanceKeys
            }
        }

        val emptyTextId = when {
            mPosition != null -> {
                check(mTimeRange != null)

                check(mTimeStamp == null)
                check(mInstanceKey == null)
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable == null)

                R.string.instances_empty_root
            }
            mTimeStamp != null -> {
                check(mInstanceKey == null)
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable == null)

                null
            }
            mInstanceKey != null -> {
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable != null)

                if (mDataWrapper!!.TaskEditable!!) {
                    R.string.empty_child
                } else {
                    R.string.empty_disabled
                }
            }
            mInstanceKeys != null -> {
                check(mInstanceKeys!!.isNotEmpty())
                check(mDataWrapper!!.TaskEditable == null)

                null
            }
            else -> {
                checkNotNull(taskKey)

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
        check(mDataWrapper != null)
        check(mDataId != null)

        (activity as GroupListListener).setGroupSelectAllVisibility(mPosition, mDataWrapper!!.instanceDatas.values.any { it.Done == null })
    }

    fun selectAll() {
        mTreeViewAdapter!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        MyCrashlytics.log(javaClass.simpleName + ".setFab " + hashCode())
        mFloatingActionButton = floatingActionButton

        mFloatingActionButton!!.setOnClickListener {
            check(mDataWrapper != null)
            check(mInstanceKeys == null)

            check(activity != null) // todo how the fuck is this null?

            when {
                mPosition != null -> {
                    check(mTimeRange != null)

                    check(mTimeStamp == null)
                    check(mInstanceKey == null)

                    check(mDataWrapper!!.TaskEditable == null)

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(rangePositionToDate(mTimeRange!!, mPosition!!))))
                }
                mTimeStamp != null -> {
                    check(mInstanceKey == null)

                    check(mDataWrapper!!.TaskEditable == null)

                    check(mTimeStamp!! > TimeStamp.now)

                    startActivity(CreateTaskActivity.getCreateIntent(activity!!, CreateTaskActivity.ScheduleHint(mTimeStamp!!.date, mTimeStamp!!.hourMinute)))
                }
                else -> {
                    check(mInstanceKey != null)

                    check(mDataWrapper!!.TaskEditable != null)

                    check(mDataWrapper!!.TaskEditable!!)

                    startActivity(CreateTaskActivity.getCreateIntent(mInstanceKey!!.taskKey))
                }
            }
        }

        updateFabVisibility()
    }

    private fun showPadding(): Boolean {
        check(mDataWrapper != null)

        when {
            mPosition != null -> {
                check(mTimeRange != null)

                check(mTimeStamp == null)
                check(mInstanceKey == null)
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable == null)

                return true
            }
            mTimeStamp != null -> {
                check(mInstanceKey == null)
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable == null)

                return (mTimeStamp!! > TimeStamp.now)
            }
            mInstanceKey != null -> {
                check(mInstanceKeys == null)

                check(mDataWrapper!!.TaskEditable != null)

                return mDataWrapper!!.TaskEditable!!
            }
            mInstanceKeys != null -> {
                check(mInstanceKeys!!.isNotEmpty())
                check(mDataWrapper!!.TaskEditable == null)

                return false
            }
            else -> {
                check(taskKey != null)

                return false
            }
        }
    }

    private fun updateFabVisibility() {
        if (mFloatingActionButton == null)
            return

        if (mDataWrapper != null && !mSelectionCallback.hasActionMode && showPadding()) {
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

        override val hasActionMode get() = mGroupListFragment.mSelectionCallback.hasActionMode

        override fun incrementSelected() {
            mGroupListFragment.mSelectionCallback.incrementSelected()
        }

        override fun decrementSelected() {
            mGroupListFragment.mSelectionCallback.decrementSelected()
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