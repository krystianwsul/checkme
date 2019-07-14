package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.removeFromGetter
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.treeadapter.*
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_task_list.*
import java.util.*

class TaskListFragment : AbstractFragment(), FabUser, ListItemAddedScroller {

    companion object {

        private const val SELECTED_TASK_KEYS_KEY = "selectedTaskKeys"
        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"
        private const val KEY_SEARCH_DATA = "searchData"
        private const val KEY_SHOW_IMAGE = "showImage"

        fun newInstance() = TaskListFragment()
    }

    private var rootTaskData: RootTaskData? = null

    private var data: Data? = null

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private val dragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter() = treeViewAdapter

            override fun onSetNewItemPosition() = selectionCallback.actionMode!!.finish()
        }
    }

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override val bottomBarData by lazy { Triple(taskListListener.getBottomBar(), R.menu.menu_edit_tasks, taskListListener::initBottomBar) }

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val selected = treeViewAdapter.selectedNodes
            check(selected.isNotEmpty())

            val taskWrappers = selected.map { it.modelNode as TaskAdapter.TaskWrapper }
            val childTaskDatas = taskWrappers.map { it.childTaskData }
            val taskKeys = childTaskDatas.map { it.taskKey }

            when (itemId) {
                R.id.action_task_share -> Utils.share(requireActivity(), getShareData(childTaskDatas))
                R.id.action_task_edit -> startActivity(CreateTaskActivity.getEditIntent(childTaskDatas.single().taskKey))
                R.id.action_task_join -> startActivity(CreateTaskActivity.getJoinIntent(taskKeys, hint()))
                R.id.action_task_delete -> {
                    checkNotNull(data)

                    removeFromGetter({ treeViewAdapter.selectedNodes.sortedByDescending { it.indentation } }) {
                        val taskWrapper = it.modelNode as TaskAdapter.TaskWrapper

                        taskWrapper.removeFromParent(x)

                        decrementSelected(x)
                    }

                    val removeNodes = selected.toMutableList()

                    fun parentPresent(treeNode: TreeNode): Boolean {
                        return (treeNode.parent as? TreeNode)?.let {
                            removeNodes.contains(it) || parentPresent(it)
                        } ?: false
                    }

                    removeNodes.toMutableList().forEach {
                        if (parentPresent(it))
                            removeNodes.remove(it)
                    }

                    val removeTaskDatas = removeNodes.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData }

                    val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(data!!.dataId, SaveService.Source.GUI, taskKeys.toSet())
                    data!!.taskData
                            .childTaskDatas
                            .removeAll(removeTaskDatas)

                    updateSelectAll()

                    taskListListener.showSnackbarRemoved(taskUndoData.taskKeys.size) {
                        DomainFactory.instance.clearTaskEndTimeStamps(data!!.dataId, SaveService.Source.GUI, taskUndoData)

                        data!!.taskData
                                .childTaskDatas
                                .apply {
                                    addAll(removeTaskDatas)

                                    if (rootTaskData != null)
                                        sort()
                                    else
                                        sortDescending()
                                }

                        initialize()
                    }
                }
                R.id.action_task_add -> startActivity(CreateTaskActivity.getCreateIntent(CreateTaskActivity.Hint.Task(taskKeys.single())))
                R.id.action_task_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKeys.single()))
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as TaskListListener).onCreateActionMode(actionMode!!)

            super.onFirstAdded(x)
        }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            val selectedNodes = treeViewAdapter.selectedNodes
            check(selectedNodes.isNotEmpty())

            val single = selectedNodes.size < 2

            val childTaskDatas = selectedNodes.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData }

            val projectIdCount = childTaskDatas.map { it.taskKey.remoteProjectId }
                    .distinct()
                    .count()

            check(projectIdCount > 0)

            return listOf(
                    R.id.action_task_join to (!single && projectIdCount == 1),
                    R.id.action_task_edit to single,
                    R.id.action_task_add to single,
                    R.id.action_task_show_instances to (childTaskDatas.singleOrNull()?.hasInstances == true)
            )
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as TaskListListener).onDestroyActionMode()
        }
    }

    private var taskListFragmentFab: FloatingActionButton? = null

    val shareData
        get() = mutableListOf<String>().also {
            checkNotNull(data)

            for (childTaskData in data!!.taskData.childTaskDatas)
                printTree(it, 1, childTaskData)

        }.joinToString("\n")

    private var selectedTaskKeys: List<TaskKey>? = null
    private var expandedTaskIds: List<TaskKey>? = null

    private var searchData: SearchData? = null

    private val initializeDisposable = CompositeDisposable()

    private val taskListListener get() = activity as TaskListListener

    private var showImage = false
    private var imageViewerData: Pair<ImageState, StfalconImageViewer<ImageState>>? = null

    override var scrollToTaskKey: TaskKey? = null
    override val listItemAddedListener get() = taskListListener
    override val recyclerView: RecyclerView get() = taskListRecycler

    private val dataRelay = BehaviorRelay.create<Unit>()

    private fun getShareData(childTaskDatas: List<ChildTaskData>) = mutableListOf<String>().also {
        check(childTaskDatas.isNotEmpty())

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
                check(selectedTaskKeys!!.isNotEmpty())
            }

            if (containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expandedTaskIds = getParcelableArrayList(EXPANDED_TASK_KEYS_KEY)!!
                check(expandedTaskIds!!.isNotEmpty())
            }

            searchData = getParcelable<SearchData>(KEY_SEARCH_DATA)

            showImage = getBoolean(KEY_SHOW_IMAGE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_task_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskListRecycler.layoutManager = LinearLayoutManager(context)

        dataRelay.subscribe {
            val hide = mutableListOf<View>(taskListProgress)
            val show: View

            if (treeViewAdapter.displayedNodes.isEmpty()) {
                hide.add(taskListRecycler)
                show = emptyTextLayout

                emptyText.setText(if (rootTaskData != null) {
                    R.string.empty_child
                } else {
                    R.string.tasks_empty_root
                })
            } else {
                show = taskListRecycler
                hide.add(emptyTextLayout)
            }

            animateVisibility(listOf(show), hide, immediate = data!!.immediate)
        }.addTo(viewCreatedDisposable)

        initialize()
    }

    fun setAllTasks(data: Data) {
        check(rootTaskData == null)

        rootTaskData = null
        this.data = data

        initialize()
    }

    fun setTaskKey(rootTaskData: RootTaskData, data: Data) {
        this.rootTaskData = rootTaskData
        this.data = data

        initialize()
    }

    private fun initialize() {
        if (view == null)
            return

        if (data == null)
            return

        initializeDisposable.clear()

        if (this::treeViewAdapter.isInitialized) {
            val selected = treeViewAdapter.selectedNodes

            selectedTaskKeys = if (selected.isEmpty()) {
                check(!selectionCallback.hasActionMode)
                null
            } else {
                check(selectionCallback.hasActionMode)
                selected.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey }
            }

            val expanded = (treeViewAdapter.treeModelAdapter as TaskAdapter).expandedTaskKeys

            expandedTaskIds = if (expanded.isEmpty()) null else expanded

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as TaskAdapter).initialize(data!!.taskData, selectedTaskKeys, expandedTaskIds)

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

                search(searchData, TreeViewAdapter.Placeholder)
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(data!!.taskData, selectedTaskKeys, expandedTaskIds)
            treeViewAdapter = taskAdapter.treeViewAdapter
            taskListRecycler.adapter = treeViewAdapter
            dragHelper.attachToRecyclerView(taskListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

                search(searchData, TreeViewAdapter.Placeholder)
            }
        }

        (context as TaskListListener).search
                .subscribe {
                    treeViewAdapter.updateDisplayedNodes {
                        search(it.value, TreeViewAdapter.Placeholder)
                    }
                }
                .addTo(initializeDisposable)

        updateFabVisibility()

        dataRelay.accept(Unit)

        updateSelectAll()

        tryScroll()
    }

    private fun getAllChildTaskDatas(childTaskData: ChildTaskData): List<ChildTaskData> {
        return listOf(listOf(listOf(childTaskData)), childTaskData.children.map { getAllChildTaskDatas(it) }).flatten().flatten()
    }

    private val ChildTaskData.allTaskKeys get() = getAllChildTaskDatas(this).map { it.taskKey }

    override fun findItem(): Int? {
        if (!this::treeViewAdapter.isInitialized)
            return null

        return treeViewAdapter.displayedNodes
                .firstOrNull {
                    val modelNode = it.modelNode

                    if (modelNode is TaskAdapter.TaskWrapper) {
                        if (it.isExpanded) {
                            modelNode.childTaskData.taskKey == scrollToTaskKey
                        } else {
                            modelNode.childTaskData
                                    .allTaskKeys
                                    .contains(scrollToTaskKey)
                        }
                    } else {
                        false
                    }
                }?.let { treeViewAdapter.getTreeNodeCollection().getPosition(it) }
    }

    private fun updateSelectAll() {
        val taskAdapter = treeViewAdapter.treeModelAdapter as TaskAdapter

        (activity as TaskListListener).setTaskSelectAllVisibility(taskAdapter.taskWrappers.isNotEmpty())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (this@TaskListFragment::treeViewAdapter.isInitialized) {
                val selected = treeViewAdapter.selectedNodes

                if (selected.isNotEmpty()) {
                    check(selectionCallback.hasActionMode)

                    val taskKeys = ArrayList(selected.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData.taskKey })
                    check(taskKeys.isNotEmpty())

                    putParcelableArrayList(SELECTED_TASK_KEYS_KEY, taskKeys)
                }

                val expandedTaskKeys = (treeViewAdapter.treeModelAdapter as TaskAdapter).expandedTaskKeys

                if (expandedTaskKeys.isNotEmpty())
                    putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, ArrayList(expandedTaskKeys))
            }

            putParcelable(KEY_SEARCH_DATA, searchData)

            putBoolean(KEY_SHOW_IMAGE, imageViewerData != null)
        }
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        taskListFragmentFab = floatingActionButton

        taskListFragmentFab!!.setOnClickListener {
            startActivity(CreateTaskActivity.getCreateIntent(hint()))
        }

        updateFabVisibility()
    }

    private fun hint() = rootTaskData?.let { CreateTaskActivity.Hint.Task(it.taskKey) }

    private fun updateFabVisibility() {
        taskListFragmentFab?.run {
            if (data != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        taskListFragmentFab = null
    }

    private fun search(searchData: SearchData?, @Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
        this.searchData = searchData
    }

    override fun onDestroyView() {
        super.onDestroyView()

        initializeDisposable.clear()
    }

    private inner class TaskAdapter(val taskListFragment: TaskListFragment) : TreeModelAdapter, TaskParent {

        lateinit var taskWrappers: MutableList<TaskWrapper>
            private set

        val treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)
        private lateinit var treeNodeCollection: TreeNodeCollection

        override val taskAdapter = this

        val expandedTaskKeys get() = taskWrappers.flatMap { it.expandedTaskKeys }

        fun initialize(taskData: TaskData, selectedTaskKeys: List<TaskKey>?, expandedTaskKeys: List<TaskKey>?) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode>()

            if (!taskData.note.isNullOrEmpty()) {
                val noteNode = NoteNode(taskData.note, false)

                treeNodes.add(noteNode.initialize(treeNodeCollection))
            }

            taskListFragment.rootTaskData
                    ?.imageState
                    ?.let {
                        treeNodes.add(ImageNode(ImageNode.ImageData(
                                it,
                                { viewer ->
                                    check(taskListFragment.imageViewerData == null)

                                    taskListFragment.imageViewerData = Pair(it, viewer)
                                },
                                {
                                    checkNotNull(taskListFragment.imageViewerData)

                                    taskListFragment.imageViewerData = null
                                },
                                taskListFragment.showImage)).initialize(treeNodeCollection))
                    }

            taskListFragment.showImage = false

            taskWrappers = mutableListOf()
            for (childTaskData in taskData.childTaskDatas) {
                val taskWrapper = TaskWrapper(0, this, childTaskData)

                treeNodes.add(taskWrapper.initialize(selectedTaskKeys, this.treeNodeCollection, expandedTaskKeys))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(taskListFragment.activity!!.layoutInflater.inflate(R.layout.row_list, parent, false))

        override fun remove(taskWrapper: TaskWrapper, x: TreeViewAdapter.Placeholder) {
            check(taskWrappers.contains(taskWrapper))

            taskWrappers.remove(taskWrapper)

            val treeNodeCollection = this.treeNodeCollection

            val treeNode = taskWrapper.treeNode

            treeNodeCollection.remove(treeNode, x)
        }

        override val hasActionMode get() = taskListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = taskListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = taskListFragment.selectionCallback.decrementSelected(x)

        inner class TaskWrapper(indentation: Int, private val taskParent: TaskParent, val childTaskData: ChildTaskData) : GroupHolderNode(indentation), TaskParent, Sortable {

            override val id = childTaskData.taskKey

            public override lateinit var treeNode: TreeNode
                private set

            override val ripple = true

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
                    check(selectedTaskKeys.isNotEmpty())
                    selectedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                val expanded = if (expandedTaskKeys != null) {
                    check(expandedTaskKeys.isNotEmpty())
                    expandedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                treeNode = TreeNode(this, nodeContainer, expanded, selected)

                val treeNodes = mutableListOf<TreeNode>()

                for (childTaskData in childTaskData.children) {
                    val taskWrapper = TaskWrapper(indentation + 1, this, childTaskData)

                    treeNodes.add(taskWrapper.initialize(selectedTaskKeys, treeNode, expandedTaskKeys))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            private val disabledOverride get() = colorDisabled.takeUnless { childTaskData.current }

            override val children
                get() = if ((childTaskData.children.isEmpty() || treeNode.isExpanded) && childTaskData.note.isNullOrEmpty()) {
                    null
                } else {
                    val text = if (childTaskData.children.isNotEmpty() && !treeNode.isExpanded) {
                        childTaskData.children.joinToString(", ") { it.name }
                    } else {
                        check(!childTaskData.note.isNullOrEmpty())

                        childTaskData.note
                    }

                    Pair(text, disabledOverride ?: colorSecondary)
                }

            override val details
                get() = if (childTaskData.scheduleText.isNullOrEmpty()) {
                    null
                } else {
                    Pair(childTaskData.scheduleText, disabledOverride ?: colorSecondary)
                }

            override val name get() = NameData(childTaskData.name, disabledOverride ?: colorPrimary)

            override fun onLongClick(viewHolder: RecyclerView.ViewHolder) {
                val treeNodeCollection = taskAdapter.treeNodeCollection

                if (taskListFragment.rootTaskData != null && treeNodeCollection.selectedChildren.isEmpty() && indentation == 0 && treeNodeCollection.nodes.none { it.isExpanded }) {
                    taskListFragment.dragHelper.startDrag(viewHolder)
                    treeNode.onLongClickSelect(viewHolder, true)
                } else {
                    treeNode.onLongClickSelect(viewHolder, false)
                }
            }

            override val isSelectable = true

            override fun onClick() = taskListFragment.activity!!.startActivity(ShowTaskActivity.newIntent(childTaskData.taskKey))

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override val thumbnail = childTaskData.imageState

            override fun compareTo(other: ModelNode) = if (other is TaskWrapper) {
                val taskListFragment = taskListFragment

                var comparison = childTaskData.compareTo(other.childTaskData)
                if (taskListFragment.rootTaskData == null && indentation == 0)
                    comparison = -comparison

                comparison
            } else {
                1
            }

            fun removeFromParent(x: TreeViewAdapter.Placeholder) = taskParent.remove(this, x)

            override fun remove(taskWrapper: TaskWrapper, x: TreeViewAdapter.Placeholder) {
                check(taskWrappers.contains(taskWrapper))

                taskWrappers.remove(taskWrapper)

                val childTreeNode = taskWrapper.treeNode

                treeNode.remove(childTreeNode, x)
            }

            override fun getOrdinal() = childTaskData.hierarchyData!!.ordinal

            override fun setOrdinal(ordinal: Double) {
                childTaskData.hierarchyData!!.ordinal = ordinal

                DomainFactory.instance.setTaskHierarchyOrdinal(taskListFragment.data!!.dataId, childTaskData.hierarchyData)
            }

            override fun filter() = childTaskData.matchesSearch(searchData)
        }
    }

    private interface TaskParent {
        val taskAdapter: TaskAdapter

        fun remove(taskWrapper: TaskAdapter.TaskWrapper, x: TreeViewAdapter.Placeholder)
    }

    data class Data(val dataId: Int, val immediate: Boolean, val taskData: TaskData)

    data class TaskData(val childTaskDatas: MutableList<ChildTaskData>, val note: String?)

    data class ChildTaskData(
            val name: String,
            val scheduleText: String?,
            val children: List<ChildTaskData>,
            val note: String?,
            private val startExactTimeStamp: ExactTimeStamp,
            val taskKey: TaskKey,
            val hierarchyData: HierarchyData?,
            val imageState: ImageState?,
            val current: Boolean,
            val hasInstances: Boolean,
            val alwaysShow: Boolean) : Comparable<ChildTaskData> {

        override fun compareTo(other: ChildTaskData) = if (hierarchyData != null) {
            hierarchyData.ordinal.compareTo(other.hierarchyData!!.ordinal)
        } else {
            check(other.hierarchyData == null)

            startExactTimeStamp.compareTo(other.startExactTimeStamp)
        }

        fun matchesSearch(searchData: SearchData?): Boolean {
            if (searchData == null)
                return alwaysShow || current

            if (!searchData.showDeleted && !current)
                return false

            val query = searchData.query

            if (query.isEmpty())
                return false

            if (name.toLowerCase().contains(query))
                return true

            if (note?.toLowerCase()?.contains(query) == true)
                return true

            return children.any { it.matchesSearch(searchData) }
        }
    }

    interface TaskListListener : ActionModeListener, SnackbarListener, ListItemAddedListener {

        fun setTaskSelectAllVisibility(selectAllVisible: Boolean)

        val search: Observable<NullableWrapper<SearchData>>

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }

    data class RootTaskData(val taskKey: TaskKey, val imageState: ImageState?)

    @Parcelize
    data class SearchData(val query: String, val showDeleted: Boolean) : Parcelable
}