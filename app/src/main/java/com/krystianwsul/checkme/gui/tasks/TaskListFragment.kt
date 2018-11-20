package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.treeadapter.*
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_task_list.*
import kotlinx.android.synthetic.main.row_task_list.view.*
import java.util.*

class TaskListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_TASK_KEYS_KEY = "selectedTaskKeys"
        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"
        private const val QUERY_KEY = "query"

        fun newInstance() = TaskListFragment()
    }

    private var allTasks: Boolean = false

    private var taskKey: TaskKey? = null

    private var dataId: Int? = null

    private var taskData: TaskData? = null

    private var treeViewAdapter: TreeViewAdapter? = null

    private val dragHelper by lazy { DragHelper(treeViewAdapter!!) }

    private val selectionCallback = object : SelectionCallback() {

        override fun unselect() = treeViewAdapter!!.unselect()

        override fun onMenuClick(menuItem: MenuItem) {
            var selected = treeViewAdapter!!.selectedNodes
            check(!selected.isEmpty())

            val taskWrappers = selected.map { it.modelNode as TaskAdapter.TaskWrapper }

            val childTaskDatas = taskWrappers.map { it.childTaskData }

            val taskKeys = ArrayList(childTaskDatas.map { it.taskKey })

            when (menuItem.itemId) {
                R.id.action_task_share -> Utils.share(requireActivity(), getShareData(childTaskDatas))
                R.id.action_task_edit -> {
                    check(selected.size == 1)

                    val childTaskData = (selected[0].modelNode as TaskAdapter.TaskWrapper).childTaskData

                    startActivity(CreateTaskActivity.getEditIntent(childTaskData.taskKey))
                }
                R.id.action_task_join -> startActivity(if (taskKey == null)
                    CreateTaskActivity.getJoinIntent(taskKeys)
                else
                    CreateTaskActivity.getJoinIntent(taskKeys, taskKey!!))
                R.id.action_task_delete -> {
                    checkNotNull(dataId)

                    do {
                        val treeNode = selected.first()

                        val taskWrapper = treeNode.modelNode as TaskAdapter.TaskWrapper

                        taskWrapper.removeFromParent()

                        decrementSelected()

                        selected = treeViewAdapter!!.selectedNodes
                    } while (selected.isNotEmpty())

                    DomainFactory.getKotlinDomainFactory().setTaskEndTimeStamps(dataId!!, SaveService.Source.GUI, taskKeys)

                    updateSelectAll()
                }
                R.id.action_task_add -> {
                    val childTaskData1 = (selected.single().modelNode as TaskAdapter.TaskWrapper).childTaskData

                    startActivity(CreateTaskActivity.getCreateIntent(childTaskData1.taskKey))
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded() {
            treeViewAdapter!!.updateDisplayedNodes {
                (activity as AppCompatActivity).startSupportActionMode(this)
            }

            actionMode!!.menuInflater.inflate(R.menu.menu_edit_tasks, actionMode!!.menu)

            updateFabVisibility()

            (activity as TaskListListener).onCreateTaskActionMode(actionMode!!)

            dragHelper.attachToRecyclerView(taskListRecycler)
        }

        override fun onSecondAdded() {
            val selectedNodes = treeViewAdapter!!.selectedNodes
            check(!selectedNodes.isEmpty())

            val projectIdCount = selectedNodes.asSequence()
                    .map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey.remoteProjectId }
                    .distinct()
                    .count()

            check(projectIdCount > 0)

            actionMode!!.menu.run {
                findItem(R.id.action_task_join).isVisible = projectIdCount == 1
                findItem(R.id.action_task_edit).isVisible = false
                findItem(R.id.action_task_delete).isVisible = !containsLoop(selectedNodes)
                findItem(R.id.action_task_add).isVisible = false
            }

            dragHelper.attachToRecyclerView(null)
        }

        override fun onOtherAdded() {
            val selectedNodes = treeViewAdapter!!.selectedNodes
            check(!selectedNodes.isEmpty())

            val projectIdCount = selectedNodes.asSequence()
                    .map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey.remoteProjectId }
                    .distinct()
                    .count()

            check(projectIdCount > 0)

            actionMode!!.menu.run {
                findItem(R.id.action_task_join).isVisible = projectIdCount == 1
                findItem(R.id.action_task_delete).isVisible = !containsLoop(selectedNodes)
            }
        }

        override fun onLastRemoved(action: () -> Unit) {
            treeViewAdapter!!.updateDisplayedNodes(action)

            updateFabVisibility()

            (activity as TaskListListener).onDestroyTaskActionMode()

            dragHelper.attachToRecyclerView(null)
        }

        override fun onSecondToLastRemoved() {
            actionMode!!.menu.run {
                findItem(R.id.action_task_join).isVisible = false
                findItem(R.id.action_task_edit).isVisible = true
                findItem(R.id.action_task_delete).isVisible = true
                findItem(R.id.action_task_add).isVisible = true
            }

            dragHelper.attachToRecyclerView(taskListRecycler)
        }

        override fun onOtherRemoved() {
            val selectedNodes = treeViewAdapter!!.selectedNodes
            check(selectedNodes.size > 1)

            val projectIdCount = selectedNodes.asSequence()
                    .map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey.remoteProjectId }
                    .distinct()
                    .count()

            check(projectIdCount > 0)

            actionMode!!.menu.run {
                findItem(R.id.action_task_join).isVisible = projectIdCount == 1
                findItem(R.id.action_task_delete).isVisible = !containsLoop(selectedNodes)
            }
        }

        private fun containsLoop(treeNodes: List<TreeNode>): Boolean {
            check(treeNodes.size > 1)

            for (treeNode in treeNodes) {
                val parents = ArrayList<TreeNode>()
                addParents(parents, treeNode)

                for (parent in parents) {
                    if (treeNodes.contains(parent))
                        return true
                }
            }

            return false
        }

        private fun addParents(parents: MutableList<TreeNode>, treeNode: TreeNode) {
            val parent = treeNode.parent as? TreeNode ?: return

            parents.add(parent)
            addParents(parents, parent)
        }
    }

    private var taskListFragmentFab: FloatingActionButton? = null

    val shareData
        get() = mutableListOf<String>().also {
            checkNotNull(taskData)

            for (childTaskData in taskData!!.childTaskDatas)
                printTree(it, 1, childTaskData)

        }.joinToString("\n")

    private var selectedTaskKeys: List<TaskKey>? = null
    private var expandedTaskIds: List<TaskKey>? = null

    private var query: String? = null

    private fun getShareData(childTaskDatas: List<ChildTaskData>) = mutableListOf<String>().also {
        check(!childTaskDatas.isEmpty())

        mutableListOf<ChildTaskData>().apply {
            childTaskDatas.filterNot { inTree(this, it) }.forEach { this.add(it) }
        }.forEach { childTaskData ->
                    printTree(it, 0, childTaskData)
                }
    }.joinToString("\n")

    private fun inTree(shareTree: List<ChildTaskData>, childTaskData: ChildTaskData): Boolean {
        if (shareTree.isEmpty())
            return false

        return if (shareTree.contains(childTaskData)) true else shareTree.any { inTree(it.children, childTaskData) }
    }

    private fun printTree(lines: MutableList<String>, indentation: Int, childTaskData: ChildTaskData) {
        lines.add("-".repeat(indentation) + childTaskData.name)


        childTaskData.children.forEach { printTree(lines, indentation + 1, it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is TaskListListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.run {
            if (containsKey(SELECTED_TASK_KEYS_KEY)) {
                selectedTaskKeys = getParcelableArrayList(SELECTED_TASK_KEYS_KEY)!!
                check(!selectedTaskKeys!!.isEmpty())
            }

            if (containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expandedTaskIds = getParcelableArrayList(EXPANDED_TASK_KEYS_KEY)!!
                check(!expandedTaskIds!!.isEmpty())
            }

            query = getString(QUERY_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_task_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskListRecycler.layoutManager = LinearLayoutManager(context)

        initialize()
    }

    fun setAllTasks(dataId: Int, taskData: TaskData) {
        check(taskKey == null)

        allTasks = true

        this.dataId = dataId
        this.taskData = taskData

        initialize()
    }

    fun setTaskKey(taskKey: TaskKey, dataId: Int, taskData: TaskData) {
        check(!allTasks)

        this.taskKey = taskKey

        this.dataId = dataId
        this.taskData = taskData

        initialize()
    }

    private fun initialize() {
        if (view == null)
            return

        if (taskData == null)
            return

        checkNotNull(dataId)

        if (treeViewAdapter != null) {
            val selected = treeViewAdapter!!.selectedNodes

            selectedTaskKeys = if (selected.isEmpty()) {
                check(!selectionCallback.hasActionMode)
                null
            } else {
                check(selectionCallback.hasActionMode)
                selected.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey }
            }

            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedTaskKeys

            expandedTaskIds = if (expanded.isEmpty()) null else expanded
        }

        treeViewAdapter = TaskAdapter.getAdapter(this, taskData!!, selectedTaskKeys, expandedTaskIds)

        taskListRecycler.adapter = treeViewAdapter

        selectionCallback.setSelected(treeViewAdapter!!.selectedNodes.size)

        updateFabVisibility()

        val hide = mutableListOf<View>(taskListProgress)
        val show: View

        if (taskData!!.childTaskDatas.isEmpty() && taskData!!.note.isNullOrEmpty()) {
            hide.add(taskListRecycler)
            show = emptyText

            emptyText.setText(if (taskKey != null) {
                R.string.empty_child
            } else {
                R.string.tasks_empty_root
            })
        } else {
            show = taskListRecycler
            hide.add(emptyText)
        }

        animateVisibility(listOf(show), hide)

        updateSelectAll()

        query?.let { search(it) }
    }

    private fun updateSelectAll() {
        checkNotNull(treeViewAdapter)
        val taskAdapter = treeViewAdapter!!.treeModelAdapter as TaskAdapter

        (activity as TaskListListener).setTaskSelectAllVisibility(!taskAdapter.taskWrappers.isEmpty())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (treeViewAdapter != null) {
                val selected = treeViewAdapter!!.selectedNodes

                if (!selected.isEmpty()) {
                    check(selectionCallback.hasActionMode)

                    val taskKeys = ArrayList(selected.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey })
                    check(!taskKeys.isEmpty())

                    putParcelableArrayList(SELECTED_TASK_KEYS_KEY, taskKeys)
                }

                val expandedTaskKeys = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedTaskKeys

                if (!expandedTaskKeys.isEmpty())
                    putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, ArrayList(expandedTaskKeys))
            }

            putString(QUERY_KEY, query)
        }
    }

    fun selectAll() = treeViewAdapter!!.selectAll()

    override fun setFab(floatingActionButton: FloatingActionButton) {
        taskListFragmentFab = floatingActionButton

        taskListFragmentFab!!.setOnClickListener {
            if (taskKey == null)
                startActivity(CreateTaskActivity.getCreateIntent(context!!))
            else
                startActivity(CreateTaskActivity.getCreateIntent(taskKey!!))
        }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        taskListFragmentFab?.run {
            if (dataId != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        taskListFragmentFab?.setOnClickListener(null)

        taskListFragmentFab = null
    }

    fun search(query: String) {
        this.query = query

        treeViewAdapter?.let {
            it.updateDisplayedNodes {
                it.query = query
            }
        }
    }

    private class TaskAdapter private constructor(val taskListFragment: TaskListFragment) : TreeModelAdapter, TaskParent {

        companion object {

            private const val TYPE_TASK = 0
            private const val TYPE_NOTE = 1

            fun getAdapter(taskListFragment: TaskListFragment, taskData: TaskData, selectedTaskKeys: List<TaskKey>?, expandedTaskKeys: List<TaskKey>?): TreeViewAdapter {
                val taskAdapter = TaskAdapter(taskListFragment)

                val density = taskListFragment.activity!!.resources.displayMetrics.density

                return taskAdapter.initialize(density, taskData, selectedTaskKeys, expandedTaskKeys)
            }
        }

        val taskWrappers = mutableListOf<TaskWrapper>()

        private lateinit var treeViewAdapter: TreeViewAdapter
        private lateinit var treeNodeCollection: TreeNodeCollection

        override val taskAdapter = this

        val expandedTaskKeys get() = taskWrappers.flatMap { it.expandedTaskKeys }

        private fun initialize(density: Float, taskData: TaskData, selectedTaskKeys: List<TaskKey>?, expandedTaskKeys: List<TaskKey>?): TreeViewAdapter {
            treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode>()

            if (!taskData.note.isNullOrEmpty()) {
                val noteNode = NoteNode(taskData.note)

                treeNodes.add(noteNode.initialize(treeNodeCollection))
            }

            for (childTaskData in taskData.childTaskDatas) {
                val taskWrapper = TaskWrapper(density, 0, this, childTaskData)

                treeNodes.add(taskWrapper.initialize(selectedTaskKeys, this.treeNodeCollection, expandedTaskKeys))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes

            return treeViewAdapter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskHolder(taskListFragment.activity!!.layoutInflater.inflate(R.layout.row_task_list, parent, false))

        override fun remove(taskWrapper: TaskWrapper) {
            check(taskWrappers.contains(taskWrapper))

            taskWrappers.remove(taskWrapper)

            val treeNodeCollection = this.treeNodeCollection

            val treeNode = taskWrapper.treeNode

            treeNodeCollection.remove(treeNode)
        }

        override val hasActionMode get() = taskListFragment.selectionCallback.hasActionMode

        override fun incrementSelected() {
            taskListFragment.selectionCallback.incrementSelected()
        }

        override fun decrementSelected() {
            taskListFragment.selectionCallback.decrementSelected()
        }

        class TaskWrapper(private val density: Float, private val indentation: Int, private val taskParent: TaskParent, val childTaskData: ChildTaskData) : ModelNode, TaskParent {

            lateinit var treeNode: TreeNode

            private val taskWrappers = mutableListOf<TaskWrapper>()

            override val taskAdapter get() = taskParent.taskAdapter

            private val taskListFragment get() = taskAdapter.taskListFragment

            val expandedTaskKeys: MutableList<TaskKey>
                get() = mutableListOf<TaskKey>().apply {
                    val treeNode = this@TaskWrapper.treeNode

                    if (treeNode.isExpanded) {
                        add(childTaskData.taskKey)

                        addAll(taskWrappers.flatMap { it.expandedTaskKeys })
                    }
                }

            fun initialize(selectedTaskKeys: List<TaskKey>?, nodeContainer: NodeContainer, expandedTaskKeys: List<TaskKey>?): TreeNode {
                val selected = if (selectedTaskKeys != null) {
                    check(!selectedTaskKeys.isEmpty())
                    selectedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                val expanded = if (expandedTaskKeys != null) {
                    check(!expandedTaskKeys.isEmpty())
                    expandedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                this.treeNode = TreeNode(this, nodeContainer, expanded, selected)

                val treeNodes = mutableListOf<TreeNode>()

                for (childTaskData in childTaskData.children) {
                    val taskWrapper = TaskWrapper(density, indentation + 1, this, childTaskData)

                    treeNodes.add(taskWrapper.initialize(selectedTaskKeys, this.treeNode, expandedTaskKeys))

                    taskWrappers.add(taskWrapper)
                }

                this.treeNode.setChildTreeNodes(treeNodes)

                return this.treeNode
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
                (viewHolder as TaskHolder).run {
                    itemView.run {
                        setBackgroundColor(if (treeNode.isSelected)
                            ContextCompat.getColor(taskListFragment.activity!!, R.color.selected)
                        else
                            Color.TRANSPARENT)

                        setOnLongClickListener {
                            if (taskListFragment.taskKey != null && treeNode.isSelected && taskAdapter.treeNodeCollection.selectedChildren.size == 1 && indentation == 0 && taskAdapter.treeNodeCollection.nodes.none { it.isExpanded }) {
                                taskListFragment.dragHelper.startDrag(viewHolder)
                                true
                            } else {
                                treeNode.onLongClickListener.onLongClick(it)
                            }
                        }
                        setOnClickListener(treeNode.onClickListener)
                    }

                    val padding = 48 * indentation

                    taskRowContainer.setPadding((padding * density + 0.5f).toInt(), 0, 0, 0)

                    taskRowImg.run {
                        visibility = if (!treeNode.expandVisible) {
                            View.INVISIBLE
                        } else {
                            check(!childTaskData.children.isEmpty())

                            setImageResource(if (treeNode.isExpanded)
                                R.drawable.ic_expand_less_black_36dp
                            else
                                R.drawable.ic_expand_more_black_36dp)

                            setOnClickListener(treeNode.expandListener)

                            View.VISIBLE
                        }
                    }

                    taskRowName.run {
                        text = childTaskData.name
                        setSingleLine(true)
                    }

                    taskRowDetails.run {
                        visibility = if (childTaskData.scheduleText.isNullOrEmpty()) {
                            View.GONE
                        } else {
                            text = childTaskData.scheduleText
                            View.VISIBLE
                        }
                    }

                    taskRowChildren.run {
                        visibility = if ((childTaskData.children.isEmpty() || treeNode.isExpanded) && childTaskData.note.isNullOrEmpty()) {
                            View.GONE
                        } else {
                            text = if (!childTaskData.children.isEmpty() && !treeNode.isExpanded) {
                                childTaskData.children.joinToString(", ") { it.name }
                            } else {
                                check(!childTaskData.note.isNullOrEmpty())

                                childTaskData.note
                            }.apply {
                                check(!isNullOrEmpty())
                            }

                            View.VISIBLE
                        }
                    }

                    taskRowSeparator.visibility = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE
                }
            }

            override val itemViewType = TYPE_TASK

            override val isSelectable = true

            override fun onClick() {
                taskListFragment.activity!!.startActivity(ShowTaskActivity.newIntent(childTaskData.taskKey))
            }

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override fun compareTo(other: ModelNode) = if (other is TaskWrapper) {
                val taskListFragment = taskListFragment

                var comparison = childTaskData.compareTo(other.childTaskData)
                if (taskListFragment.taskKey == null && indentation == 0)
                    comparison = -comparison

                comparison
            } else {
                check(other is NoteNode)

                1
            }

            fun removeFromParent() {
                taskParent.remove(this)
            }

            override fun remove(taskWrapper: TaskWrapper) {
                check(taskWrappers.contains(taskWrapper))

                taskWrappers.remove(taskWrapper)

                val childTreeNode = taskWrapper.treeNode

                treeNode.remove(childTreeNode)
            }

            override fun getOrdinal() = childTaskData.hierarchyData!!.ordinal

            override fun setOrdinal(ordinal: Double) {
                childTaskData.hierarchyData!!.ordinal = ordinal

                DomainFactory.getKotlinDomainFactory().setTaskHierarchyOrdinal(taskListFragment.dataId!!, childTaskData.hierarchyData)
            }

            override fun matchesSearch(query: String) = childTaskData.matchesSearch(query)
        }

        private class NoteNode(private val note: String) : ModelNode {

            lateinit var treeNode: TreeNode
                private set

            init {
                check(note.isNotEmpty())
            }

            fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
                treeNode = TreeNode(this, treeNodeCollection, false, false)
                treeNode.setChildTreeNodes(listOf())

                return treeNode
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
                (viewHolder as TaskHolder).run {
                    itemView.run {
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnLongClickListener(null)
                        setOnClickListener(null)
                    }

                    taskRowContainer.setPadding(0, 0, 0, 0)

                    taskRowImg.visibility = View.INVISIBLE

                    taskRowName.run {
                        text = note
                        setSingleLine(false)
                    }

                    taskRowDetails.visibility = View.GONE

                    taskRowChildren.visibility = View.GONE

                    taskRowSeparator.visibility = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE
                }
            }

            override val itemViewType = TYPE_NOTE

            override val isSelectable = false

            override fun onClick() = Unit

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = false

            override val isSeparatorVisibleWhenNotExpanded = true

            override fun compareTo(other: ModelNode): Int {
                check(other is TaskWrapper)

                return -1
            }
        }

        private inner class TaskHolder(view: View) : RecyclerView.ViewHolder(view) {

            val taskRowContainer = itemView.taskRowContainer!!
            val taskRowName = itemView.taskRowName!!
            val taskRowDetails = itemView.taskRowDetails!!
            val taskRowChildren = itemView.taskRowChildren!!
            val taskRowImg = itemView.taskRowImg!!
            val taskRowSeparator = itemView.taskRowSeparator!!
        }
    }

    private interface TaskParent {
        val taskAdapter: TaskAdapter

        fun remove(taskWrapper: TaskAdapter.TaskWrapper)
    }

    data class TaskData(val childTaskDatas: List<ChildTaskData>, val note: String?)

    data class ChildTaskData(
            val name: String,
            val scheduleText: String?,
            val children: List<ChildTaskData>,
            val note: String?,
            private val startExactTimeStamp: ExactTimeStamp,
            val taskKey: TaskKey,
            val hierarchyData: HierarchyData?) : Comparable<ChildTaskData> {

        override fun compareTo(other: ChildTaskData) = if (hierarchyData != null) {
            hierarchyData.ordinal.compareTo(other.hierarchyData!!.ordinal)
        } else {
            check(other.hierarchyData == null)

            startExactTimeStamp.compareTo(other.startExactTimeStamp)
        }

        fun matchesSearch(query: String): Boolean {
            if (query.isEmpty())
                return true

            if (name.toLowerCase().contains(query))
                return true

            return children.any { it.matchesSearch(query) }
        }
    }

    interface TaskListListener {

        fun onCreateTaskActionMode(actionMode: ActionMode)
        fun onDestroyTaskActionMode()
        fun setTaskSelectAllVisibility(selectAllVisible: Boolean)
    }
}