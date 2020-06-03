package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.*
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_task_list.*
import java.io.Serializable
import java.util.*

class TaskListFragment : AbstractFragment(), FabUser, ListItemAddedScroller {

    companion object {

        private const val SELECTED_TASK_KEYS_KEY = "selectedTaskKeys"
        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"
        private const val KEY_SEARCH_DATA = "searchData"
        private const val KEY_SHOW_IMAGE = "showImage"

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newInstance() = TaskListFragment()
    }

    private var rootTaskData: RootTaskData? = null

    private var data: Data? = null

    lateinit var treeViewAdapter: TreeViewAdapter<NodeHolder>
        private set

    private val dragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter() = treeViewAdapter

            override fun onSetNewItemPosition() = selectionCallback.actionMode!!.finish()
        }
    }

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        checkNotNull(data)

        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        taskListListener.showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override val bottomBarData by lazy { Triple(taskListListener.getBottomBar(), R.menu.menu_edit_tasks, taskListListener::initBottomBar) }

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder): Boolean {
            val selected = treeViewAdapter.selectedNodes
            check(selected.isNotEmpty())

            val taskWrappers = selected.map { it.modelNode as TaskAdapter.TaskWrapper }
            val childTaskDatas = taskWrappers.map { it.childTaskData }
            val taskKeys = childTaskDatas.map { it.taskKey }

            when (itemId) {
                R.id.action_task_share -> Utils.share(requireActivity(), getShareData(childTaskDatas))
                R.id.action_task_edit -> startActivity(EditActivity.getParametersIntent(EditParameters.Edit(childTaskDatas.single().taskKey)))
                R.id.action_task_join -> startActivity(EditActivity.getParametersIntent(EditParameters.Join(taskKeys, hint())))
                R.id.action_task_delete -> {
                    checkNotNull(data)

                    RemoveInstancesDialogFragment.newInstance(taskKeys.toSet())
                            .also { it.listener = deleteInstancesListener }
                            .show(childFragmentManager, TAG_REMOVE_INSTANCES)
                }
                R.id.action_task_add -> startActivity(EditActivity.getParametersIntent(EditParameters.Create(EditActivity.Hint.Task(taskKeys.single()))))
                R.id.action_task_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKeys.single()))
                R.id.actionTaskCopy -> startActivity(EditActivity.getParametersIntent(EditParameters.Copy(taskKeys.single())))
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility("onFirstAdded")

            (activity as TaskListListener).onCreateActionMode(actionMode!!)

            super.onFirstAdded(x)
        }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            val selectedNodes = treeViewAdapter.selectedNodes
            check(selectedNodes.isNotEmpty())

            val single = selectedNodes.size < 2

            val singleData = selectedNodes.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData }.singleOrNull()

            return listOf(
                    R.id.action_task_join to !single,
                    R.id.action_task_edit to single,
                    R.id.action_task_add to single,
                    R.id.action_task_show_instances to single,
                    R.id.actionTaskCopy to (singleData?.current == true)
            )
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility("onLastRemoved")

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

            searchData = getParcelable(KEY_SEARCH_DATA)
            showImage = getBoolean(KEY_SHOW_IMAGE)
        }

        (childFragmentManager.findFragmentByTag(TAG_REMOVE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_task_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskListRecycler.layoutManager = LinearLayoutManager(context)

        Observables.combineLatest(dataRelay, taskListListener.search)
                .subscribe { (_, searchWrapper) ->
                    treeViewAdapter.updateDisplayedNodes {
                        search(searchWrapper.value, TreeViewAdapter.Placeholder)
                    }

                    val hide = mutableListOf<View>(taskListProgress)
                    val show = mutableListOf<View>()

                    if (treeViewAdapter.displayedNodes.isEmpty()) {
                        hide.add(taskListRecycler)
                        show.add(emptyTextLayout)

                        val (emptyTextId, emptyDrawableId) = when {
                            searchWrapper.value != null -> Pair(R.string.noResults, R.drawable.search)
                            rootTaskData != null -> Pair(R.string.empty_child, R.drawable.empty)
                            else -> Pair(R.string.tasks_empty_root, R.drawable.empty)
                        }

                        emptyText.setText(emptyTextId)
                        emptyImage.setImageResource(emptyDrawableId)
                    } else {
                        show.add(taskListRecycler)
                        hide.add(emptyTextLayout)
                    }

                    animateVisibility(show, hide, immediate = data!!.immediate)
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

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as TaskAdapter).initialize(data!!.taskData, selectedTaskKeys, expandedTaskIds)

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

                search(searchData, TreeViewAdapter.Placeholder)
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(data!!.taskData, selectedTaskKeys, expandedTaskIds)
            treeViewAdapter = taskAdapter.treeViewAdapter
            taskListRecycler.adapter = treeViewAdapter
            taskListRecycler.itemAnimator = CustomItemAnimator()
            dragHelper.attachToRecyclerView(taskListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

                search(searchData, TreeViewAdapter.Placeholder)
            }
        }

        updateFabVisibility("initialize")

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
            startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint())))
        }

        updateFabVisibility("setFab")
    }

    private fun hint() = rootTaskData?.let { EditActivity.Hint.Task(it.taskKey) }

    private fun updateFabVisibility(source: String) {
        Preferences.tickLog.logLineHour("fab ${hashCode()} $source ${taskListFragmentFab != null}, ${data != null}, ${!selectionCallback.hasActionMode}")
        taskListFragmentFab?.run {
            if (data?.taskData?.showFab == true && !selectionCallback.hasActionMode) {
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

    private inner class TaskAdapter(val taskListFragment: TaskListFragment) : GroupHolderAdapter(), TaskParent {

        override val keyChain = listOf<TaskKey>()

        lateinit var taskWrappers: MutableList<TaskWrapper>
            private set

        val treeViewAdapter = TreeViewAdapter(this, Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress))

        override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
            private set

        override val taskAdapter = this

        val expandedTaskKeys get() = taskWrappers.flatMap { it.expandedTaskKeys }

        fun initialize(taskData: TaskData, selectedTaskKeys: List<TaskKey>?, expandedTaskKeys: List<TaskKey>?) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode<NodeHolder>>()

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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(taskListFragment.requireActivity().layoutInflater.inflate(R.layout.row_list, parent, false))

        override val hasActionMode get() = taskListFragment.selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = taskListFragment.selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = taskListFragment.selectionCallback.decrementSelected(x)

        override fun scrollToTop() = this@TaskListFragment.scrollToTop()

        inner class TaskWrapper(
                indentation: Int,
                private val taskParent: TaskParent,
                val childTaskData: ChildTaskData
        ) : GroupHolderNode(indentation), TaskParent, Sortable {

            override val keyChain = taskParent.keyChain + childTaskData.taskKey

            override val id = keyChain

            public override lateinit var treeNode: TreeNode<NodeHolder>
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

            fun initialize(
                    selectedTaskKeys: List<TaskKey>?,
                    nodeContainer: NodeContainer<NodeHolder>,
                    expandedTaskKeys: List<TaskKey>?
            ): TreeNode<NodeHolder> {
                val selected = if (selectedTaskKeys != null) {
                    check(selectedTaskKeys.isNotEmpty())
                    selectedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                val expanded = if (expandedTaskKeys != null && childTaskData.children.isNotEmpty()) {
                    check(expandedTaskKeys.isNotEmpty())
                    expandedTaskKeys.contains(childTaskData.taskKey)
                } else {
                    false
                }

                treeNode = TreeNode(this, nodeContainer, expanded, selected)

                val treeNodes = mutableListOf<TreeNode<NodeHolder>>()

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

            override fun onClick(holder: NodeHolder) = taskListFragment.startActivity(ShowTaskActivity.newIntent(childTaskData.taskKey))

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override val thumbnail = childTaskData.imageState

            override fun compareTo(other: ModelNode<NodeHolder>) = if (other is TaskWrapper) {
                var comparison = childTaskData.compareTo(other.childTaskData)
                if (taskListFragment.rootTaskData == null && indentation == 0)
                    comparison = -comparison

                comparison
            } else {
                1
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

        val keyChain: List<TaskKey>
    }

    data class Data(
            val dataId: Int,
            val immediate: Boolean,
            val taskData: TaskData
    )

    data class TaskData(
            val childTaskDatas: MutableList<ChildTaskData>,
            val note: String?,
            val showFab: Boolean
    )

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
            val alwaysShow: Boolean
    ) : Comparable<ChildTaskData> {

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

            if (name.toLowerCase(Locale.getDefault()).contains(query))
                return true

            if (note?.toLowerCase(Locale.getDefault())?.contains(query) == true)
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