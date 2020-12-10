package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Bundle
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
import com.krystianwsul.checkme.databinding.FragmentTaskListBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.*
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.utils.webSearchIntent
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.QueryMatch
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.*
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
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

    lateinit var treeViewAdapter: TreeViewAdapter<AbstractHolder>
        private set

    private val dragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter() = treeViewAdapter
        }
    }

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        checkNotNull(data)

        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        listener.showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_edit_tasks, listener::initBottomBar) }

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
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
                R.id.action_task_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKeys.single()))
                R.id.actionTaskCopy -> startActivity(getCopyTasksIntent(taskKeys))
                R.id.actionTaskWebSearch -> startActivity(webSearchIntent(childTaskDatas.single().name))
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            listener.onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder, initial)
        }

        override fun updateMenu() {
            super.updateMenu()

            updateFabVisibility("updateMenu")
        }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            val selectedNodes = treeViewAdapter.selectedNodes
            check(selectedNodes.isNotEmpty())

            val single = selectedNodes.size == 1

            val datas = selectedNodes.map { (it.modelNode as TaskAdapter.TaskWrapper).childTaskData }

            val current = datas.all { it.current }

            return listOf(
                    R.id.action_task_edit to (single && current),
                    R.id.action_task_join to (!single && current),
                    R.id.action_task_delete to current,
                    R.id.action_task_show_instances to single,
                    R.id.actionTaskCopy to current,
                    R.id.actionTaskWebSearch to single
            )
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            listener.onDestroyActionMode()
            updateFabVisibility("onLastRemoved")
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

    private val listener get() = activity as Listener

    private var showImage = false
    private var imageViewerData: Pair<ImageState, StfalconImageViewer<ImageState>>? = null

    override var scrollToTaskKey: TaskKey? = null
    override val listItemAddedListener get() = listener
    override val recyclerView: RecyclerView get() = binding.taskListRecycler

    private val initializedRelay = BehaviorRelay.create<Unit>()

    private val bindingProperty = ResettableProperty<FragmentTaskListBinding>()
    private var binding by bindingProperty

    val emptyTextLayout get() = binding.taskListEmptyTextInclude.emptyTextLayout

    private fun getShareData(childTaskDatas: List<ChildTaskData>) = mutableListOf<String>().also {
        check(childTaskDatas.isNotEmpty())

        mutableListOf<ChildTaskData>().apply {
            childTaskDatas.filterNot { inTree(this, it) }.forEach { this.add(it) }
        }.forEach { childTaskData ->
            printTree(it, 0, childTaskData)
        }
    }.joinToString("\n")

    private fun inTree(shareTree: List<ChildTaskData>, childTaskData: ChildTaskData): Boolean = when {
        shareTree.isEmpty() -> false
        shareTree.contains(childTaskData) -> true
        else -> shareTree.any { inTree(it.children, childTaskData) }
    }

    private fun printTree(lines: MutableList<String>, indentation: Int, childTaskData: ChildTaskData) {
        lines.add("-".repeat(indentation) + childTaskData.name)

        childTaskData.children.forEach { printTree(lines, indentation + 1, it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is Listener)
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

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_REMOVE_INSTANCES)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentTaskListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.taskListRecycler.layoutManager = LinearLayoutManager(context)

        viewCreatedDisposable += observeEmptySearchState(
                initializedRelay,
                listener.taskSearch,
                { treeViewAdapter },
                ::search,
                binding.taskListRecycler,
                binding.taskListProgress,
                binding.taskListEmptyTextInclude.emptyTextLayout,
                { data!!.immediate },
                { rootTaskData?.let { R.string.empty_child } ?: R.string.tasks_empty_root }
        )

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
        if (view == null) return
        if (data == null) return

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
                (treeViewAdapter.treeModelAdapter as TaskAdapter).initialize(
                        data!!.taskData,
                        selectedTaskKeys,
                        expandedTaskIds,
                        data!!.copying
                )

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, false)
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(data!!.taskData, selectedTaskKeys, expandedTaskIds, data!!.copying)
            treeViewAdapter = taskAdapter.treeViewAdapter

            binding.taskListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.taskListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, true)

                search(searchData, it)
            }
        }

        updateFabVisibility("initialize")

        initializedRelay.accept(Unit)

        updateSelectAll()

        tryScroll()
    }

    private fun getAllChildTaskDatas(childTaskData: ChildTaskData): List<ChildTaskData> = listOf(
            listOf(listOf(childTaskData)),
            childTaskData.children.map(::getAllChildTaskDatas)
    ).flatten().flatten()

    private val ChildTaskData.allTaskKeys get() = getAllChildTaskDatas(this).map { it.taskKey }

    override fun findItem(): Int? {
        if (!this::treeViewAdapter.isInitialized) return null

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
                }
                ?.let(treeViewAdapter.getTreeNodeCollection()::getPosition)
    }

    private fun updateSelectAll() {
        val taskAdapter = treeViewAdapter.treeModelAdapter as TaskAdapter

        (activity as Listener).setTaskSelectAllVisibility(taskAdapter.taskWrappers.isNotEmpty())
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

    override fun setFab(floatingActionButton: FloatingActionButton) {
        taskListFragmentFab = floatingActionButton

        updateFabVisibility("setFab")
    }

    private fun hint() = rootTaskData?.let { EditActivity.Hint.Task(it.taskKey) }

    private fun updateFabVisibility(source: String) {
        Preferences.tickLog.logLineHour("fab ${hashCode()} $source ${taskListFragmentFab != null}, ${data != null}, ${!selectionCallback.hasActionMode}")

        taskListFragmentFab?.run {
            fun edit(editParameters: EditParameters.Create, closeActionMode: Boolean = false) = setOnClickListener {
                if (closeActionMode) selectionCallback.actionMode!!.finish()

                startActivity(EditActivity.getParametersIntent(editParameters))
            }

            if (data == null) {
                hide()
            } else if (selectionCallback.hasActionMode) {
                val selectedNodes = treeViewAdapter.selectedNodes
                check(selectedNodes.isNotEmpty())

                val childTaskData = selectedNodes.singleOrNull()?.let {
                    (it.modelNode as TaskAdapter.TaskWrapper).childTaskData
                }

                if (childTaskData?.isVisible == true) {
                    show()

                    edit(EditParameters.Create(EditActivity.Hint.Task(childTaskData.taskKey)), true)
                } else {
                    hide()
                }
            } else {
                if (data!!.taskData.showFab) {
                    show()

                    edit(EditParameters.Create(hint(), showFirstSchedule = data!!.showFirstSchedule))
                } else {
                    hide()
                }
            }
        }
    }

    override fun clearFab() {
        taskListFragmentFab = null
    }

    private fun search(searchData: SearchData?, placeholder: TreeViewAdapter.Placeholder) {
        this.searchData = searchData
        treeViewAdapter.setFilterCriteria(searchData, placeholder)
        updateFabVisibility("search")
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class TaskAdapter(val taskListFragment: TaskListFragment) :
            BaseAdapter(),
            TaskParent,
            ActionModeCallback by taskListFragment.selectionCallback {

        override val keyChain = listOf<TaskKey>()

        lateinit var taskWrappers: MutableList<TaskWrapper>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
                viewCreatedDisposable
        )

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        override val taskAdapter = this

        val expandedTaskKeys get() = taskWrappers.flatMap { it.expandedTaskKeys }

        fun initialize(
                taskData: TaskData,
                selectedTaskKeys: List<TaskKey>?,
                expandedTaskKeys: List<TaskKey>?,
                copying: Boolean,
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            if (taskData.assignedTo.isNotEmpty())
                treeNodes += AssignedNode(taskData.assignedTo).initialize(treeNodeCollection)

            if (!taskData.note.isNullOrEmpty())
                treeNodes += NoteNode(taskData.note).initialize(treeNodeCollection)

            taskListFragment.rootTaskData
                    ?.imageState
                    ?.let {
                        treeNodes += ImageNode( // todo create some sort of imageViewerHost, merge code wtih GroupListFragment
                                ImageNode.ImageData(
                                        it,
                                        { viewer ->
                                            check(taskListFragment.imageViewerData == null)

                                            taskListFragment.imageViewerData = Pair(it, viewer)
                                        },
                                        {
                                            checkNotNull(taskListFragment.imageViewerData)

                                            taskListFragment.imageViewerData = null
                                        },
                                        taskListFragment.showImage
                                ),
                                null
                        ).initialize(treeNodeCollection)
                    }

            taskListFragment.showImage = false

            taskWrappers = mutableListOf()
            for (childTaskData in taskData.childTaskDatas) {
                val taskWrapper = TaskWrapper(0, this, childTaskData, copying, null)

                treeNodes += taskWrapper.initialize(
                        selectedTaskKeys,
                        treeNodeCollection,
                        expandedTaskKeys
                )

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes
        }

        override fun scrollToTop() = this@TaskListFragment.scrollToTop()

        inner class TaskWrapper(
                override val indentation: Int,
                private val taskParent: TaskParent,
                val childTaskData: ChildTaskData,
                private val copying: Boolean,
                override val parentNode: ModelNode<AbstractHolder>?,
        ) :
                AbstractModelNode(),
                TaskParent,
                Sortable,
                MultiLineModelNode,
                InvisibleCheckboxModelNode,
                ThumbnailModelNode,
                IndentationModelNode {

            override val holderType = HolderType.EXPANDABLE_MULTILINE

            override val keyChain = taskParent.keyChain + childTaskData.taskKey

            override val id = keyChain

            public override lateinit var treeNode: TreeNode<AbstractHolder>
                private set

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

            override val delegates by lazy {
                listOf(
                        ExpandableDelegate(treeNode),
                        MultiLineDelegate(this),
                        InvisibleCheckboxDelegate(this),
                        ThumbnailDelegate(this),
                        IndentationDelegate(this)
                )
            }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                        indentation,
                        false,
                        thumbnail != null,
                        true
                )

            override val checkBoxInvisible = false

            fun initialize(
                    selectedTaskKeys: List<TaskKey>?,
                    nodeContainer: NodeContainer<AbstractHolder>,
                    expandedTaskKeys: List<TaskKey>?,
            ): TreeNode<AbstractHolder> {
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

                val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

                for (childTaskData in childTaskData.children) {
                    val taskWrapper = TaskWrapper(
                            indentation + 1,
                            this,
                            childTaskData,
                            copying,
                            this
                    )

                    treeNodes.add(taskWrapper.initialize(selectedTaskKeys, treeNode, expandedTaskKeys))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            private val disabledOverride = R.color.textDisabled.takeUnless { childTaskData.isVisible }

            override val children: Pair<String, Int>?
                get() {
                    val text = treeNode.takeIf { !it.isExpanded }
                            ?.allChildren
                            ?.filter { it.modelNode is TaskWrapper && it.canBeShown() }
                            ?.map { it.modelNode as TaskWrapper }
                            ?.takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") { it.childTaskData.name }
                            ?: childTaskData.note.takeIf { !it.isNullOrEmpty() }

                    return text?.let { Pair(it, disabledOverride ?: R.color.textSecondary) }
                }

            override val details
                get() = if (childTaskData.scheduleText.isNullOrEmpty()) {
                    null
                } else {
                    Pair(childTaskData.scheduleText, disabledOverride ?: R.color.textSecondary)
                }

            override val name
                get() = MultiLineNameData.Visible(
                        childTaskData.name,
                        disabledOverride ?: R.color.textPrimary
                )

            override val isSelectable = !copying

            override val isDraggable = true

            override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
                val treeNodeCollection = taskAdapter.treeNodeCollection

                return if (taskListFragment.rootTaskData != null
                        && treeNodeCollection.selectedChildren.isEmpty()
                        && indentation == 0
                        && treeNodeCollection.nodes.none { it.isExpanded }
                ) {
                    taskListFragment.dragHelper.startDrag(viewHolder)

                    true
                } else {
                    false
                }
            }

            override fun onClick(holder: AbstractHolder) {
                if (!copying)
                    taskListFragment.startActivity(ShowTaskActivity.newIntent(childTaskData.taskKey))
                else if (indentation == 0)
                    taskListFragment.listener.startCopy(childTaskData.taskKey)
            }

            override val thumbnail = childTaskData.imageState

            override fun compareTo(other: ModelNode<AbstractHolder>) = if (other is TaskWrapper) {
                var comparison = childTaskData.compareTo(other.childTaskData)
                if (taskListFragment.data!!.reverseOrderForTopLevelNodes && indentation == 0)
                    comparison = -comparison

                comparison
            } else {
                1
            }

            override fun getOrdinal() = childTaskData.ordinal

            override fun setOrdinal(ordinal: Double) {
                childTaskData.ordinal = ordinal

                DomainFactory.instance.setOrdinal(
                        taskListFragment.data!!.dataId,
                        childTaskData.taskKey,
                        ordinal
                )
            }

            override fun normalize() = childTaskData.normalize()

            override fun matches(filterCriteria: Any?) = childTaskData.matchesSearch(filterCriteria as? SearchData)

            override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false

            override fun parentHierarchyMatches(filterCriteria: Any?) =
                    super.parentHierarchyMatches(filterCriteria)
                            && childTaskData.showIfParentShown(filterCriteria as? SearchData)
        }
    }

    private interface TaskParent {

        val taskAdapter: TaskAdapter

        val keyChain: List<TaskKey>
    }

    data class Data(
            val dataId: Int,
            val immediate: Boolean,
            val taskData: TaskData,
            val reverseOrderForTopLevelNodes: Boolean,
            val copying: Boolean = false,
            val showFirstSchedule: Boolean = true,
    )

    data class TaskData(
            val childTaskDatas: List<ChildTaskData>,
            val note: String?, // todo assigned
            val showFab: Boolean,
            val assignedTo: List<AssignedNode.User>,
    )

    data class ChildTaskData(
            val name: String,
            val scheduleText: String?,
            val children: List<ChildTaskData>,
            val note: String?,
            val taskKey: TaskKey,
            val taskHierarchyKey: TaskHierarchyKey?,
            val imageState: ImageState?,
            val current: Boolean,
            val isVisible: Boolean,
            val alwaysShow: Boolean,
            var ordinal: Double,
    ) : Comparable<ChildTaskData>, QueryMatch {

        override fun compareTo(other: ChildTaskData) = ordinal.compareTo(other.ordinal)

        override val normalizedName by lazy { name.normalized() }
        override val normalizedNote by lazy { note?.normalized() }

        fun normalize() {
            normalizedName
            normalizedNote
        }

        fun matchesSearch(searchData: SearchData?): Boolean {
            if (searchData == null) return isVisible

            if (!searchData.showDeleted && !isVisible) return false

            if (matchesQuery(searchData.query)) return true

            return false
        }

        fun showIfParentShown(searchData: SearchData?): Boolean {
            if (searchData == null) return isVisible

            if (!searchData.showDeleted && !isVisible) return false

            return true
        }
    }

    interface Listener : ActionModeListener, SnackbarListener, ListItemAddedListener {

        val taskSearch: Observable<NullableWrapper<SearchData>>

        fun setTaskSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()

        fun startCopy(taskKey: TaskKey): Unit = throw UnsupportedOperationException()
    }

    data class RootTaskData(val taskKey: TaskKey, val imageState: ImageState?)
}