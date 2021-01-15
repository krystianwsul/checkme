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
import com.jakewharton.rxrelay3.BehaviorRelay
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
import com.krystianwsul.checkme.utils.FilterParamsMatchable
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.utils.webSearchIntent
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.*
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.*
import com.krystianwsul.checkme.gui.instances.tree.TaskNode as InstanceTreeTaskNode

class TaskListFragment : AbstractFragment(), FabUser, ListItemAddedScroller {

    companion object {

        private const val KEY_SEARCH_DATA = "searchData"
        private const val KEY_SHOW_IMAGE = "showImage"
        private const val KEY_ADAPTER_STATE = "adapterState"

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

            val taskWrappers = selected.map { it.modelNode as TaskNode }
            val childTaskDatas = taskWrappers.map { it.childTaskData }
            val taskKeys = childTaskDatas.map { it.taskKey }

            when (itemId) {
                R.id.action_task_share -> Utils.share(requireActivity(), getShareData(childTaskDatas))
                R.id.action_task_edit -> startActivity(EditActivity.getParametersIntent(EditParameters.Edit(childTaskDatas.single().taskKey)))
                R.id.action_task_join -> startActivity(EditActivity.getParametersIntent(EditParameters.Join(
                        taskKeys.map(EditParameters.Join.Joinable::Task),
                        hint()
                )))
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

            val current = selectedNodes.map { (it.modelNode as TaskNode).childTaskData }.all { it.current }

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

            for (entryData in data!!.taskData.entryDatas) // todo project
                printTree(it, 1, entryData)

        }.joinToString("\n")

    private lateinit var adapterState: AdapterState

    private var filterCriteria: FilterCriteria = FilterCriteria.None

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

    override val scrollDisposable = viewCreatedDisposable

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

    private fun printTree(lines: MutableList<String>, indentation: Int, entryData: EntryData) {
        lines.add("-".repeat(indentation) + entryData.name)

        entryData.children.forEach { printTree(lines, indentation + 1, it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is Listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterState = savedInstanceState?.getParcelable(KEY_ADAPTER_STATE) ?: AdapterState()

        savedInstanceState?.run {
            filterCriteria = getParcelable(KEY_SEARCH_DATA)!!
            showImage = getBoolean(KEY_SHOW_IMAGE)
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_REMOVE_INSTANCES)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentTaskListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.taskListRecycler.layoutManager = LinearLayoutManager(context)

        viewCreatedDisposable += observeEmptySearchState(
                initializedRelay,
                listener.taskSearch.cast(),
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

    private fun initialize(placeholder: TreeViewAdapter.Placeholder? = null) {
        if (view == null) return
        if (data == null) return

        val showProjects = (filterCriteria as? FilterCriteria.Full)?.filterParams?.showProjects == true

        if (this::treeViewAdapter.isInitialized) {
            val adapterState = getAdapterState()
            check(selectionCallback.hasActionMode == adapterState.selectedTaskKeys.isNotEmpty())

            fun initializeTaskAdapter(placeholder: TreeViewAdapter.Placeholder) {
                (treeViewAdapter.treeModelAdapter as TaskAdapter).initialize(
                        data!!.taskData,
                        adapterState,
                        data!!.copying,
                        showProjects,
                )

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, placeholder, false)
            }

            if (placeholder != null)
                initializeTaskAdapter(placeholder)
            else
                treeViewAdapter.updateDisplayedNodes(::initializeTaskAdapter)
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(data!!.taskData, adapterState, data!!.copying, showProjects)
            treeViewAdapter = taskAdapter.treeViewAdapter

            binding.taskListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.taskListRecycler)

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, true)

                search(filterCriteria, it)
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

                    if (modelNode is TaskNode) {
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

        (activity as Listener).setTaskSelectAllVisibility(taskAdapter.nodes.isNotEmpty())
    }

    private fun getAdapterState() = treeViewAdapter.run {
        AdapterState(
                selectedNodes.map { (it.modelNode as TaskNode).childTaskData.taskKey }.toSet(),
                (treeModelAdapter as TaskAdapter).expandedTaskKeys,
                (treeModelAdapter as TaskAdapter).expandedProjectKeys,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (this@TaskListFragment::treeViewAdapter.isInitialized)
                putParcelable(KEY_ADAPTER_STATE, getAdapterState())

            putParcelable(KEY_SEARCH_DATA, filterCriteria)

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

                val childTaskData = selectedNodes.singleOrNull()?.let { (it.modelNode as TaskNode).childTaskData }

                if (childTaskData?.canAddSubtask == true) {
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

    private fun search(filterCriteria: FilterCriteria, placeholder: TreeViewAdapter.Placeholder?) {
        fun FilterCriteria.showProjects() = (this as? FilterCriteria.Full)?.filterParams?.showProjects

        val showProjectsChanged = rootTaskData == null &&
                this.filterCriteria.showProjects() != filterCriteria.showProjects()

        this.filterCriteria = filterCriteria
        updateFabVisibility("search")

        if (placeholder != null) {
            check(this::treeViewAdapter.isInitialized)

            if (showProjectsChanged)
                initialize(placeholder)
            else
                treeViewAdapter.setFilterCriteria(filterCriteria, placeholder)
        } else {
            check(!this::treeViewAdapter.isInitialized)
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class TaskAdapter(val taskListFragment: TaskListFragment) :
            BaseAdapter(),
            NodeParent,
            ActionModeCallback by taskListFragment.selectionCallback {

        lateinit var nodes: MutableList<Node>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
        )

        public override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        override val taskAdapter = this

        val expandedTaskKeys get() = nodes.flatMap { it.expandedTaskKeys }.toSet()
        val expandedProjectKeys get() = nodes.flatMap { it.expandedProjectKeys }.toSet()

        fun initialize(
                taskData: TaskData,
                adapterState: AdapterState,
                copying: Boolean,
                showProjects: Boolean,
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            if (taskData.projectInfo != null || !taskData.note.isNullOrEmpty()) {
                treeNodes += DetailsNode(
                        taskData.projectInfo,
                        taskData.note,
                        null,
                        0
                ).initialize(treeNodeCollection)
            }

            taskListFragment.rootTaskData
                    ?.imageState
                    ?.let {
                        treeNodes += ImageNode( // todo create some sort of imageViewerHost, merge code with GroupListFragment
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

            nodes = taskData.entryDatas
                    .flatMap {
                        fun ChildTaskData.toRootWrapper() = TaskNode(
                                0,
                                true,
                                this@TaskAdapter,
                                this,
                                copying,
                                null,
                        )

                        when (it) {
                            is ProjectData -> {
                                if (showProjects)
                                    listOf(ProjectNode(this, it, copying))
                                else
                                    it.children.map { it.toRootWrapper() }
                            }
                            is ChildTaskData -> listOf(it.toRootWrapper())
                            else -> throw IllegalArgumentException()
                        }
                    }
                    .toMutableList()

            treeNodeCollection.nodes = nodes.map { it.initialize(adapterState, treeNodeCollection, showProjects) }
        }

        override fun scrollToTop() = this@TaskListFragment.scrollToTop()
    }

    private abstract class Node(
            private val entryData: EntryData,
            private val nodeParent: NodeParent,
            private val reverseSortOrder: Boolean,
    ) : AbstractModelNode(), NodeParent, MultiLineModelNode, InvisibleCheckboxModelNode, IndentationModelNode {

        override val holderType = HolderType.EXPANDABLE_MULTILINE

        override val id = entryData.id

        public override lateinit var treeNode: TreeNode<AbstractHolder>
            protected set

        protected lateinit var taskNodes: List<TaskNode>

        override val taskAdapter get() = nodeParent.taskAdapter

        protected val taskListFragment get() = taskAdapter.taskListFragment

        abstract val expandedTaskKeys: Set<TaskKey>
        abstract val expandedProjectKeys: Set<ProjectKey<*>>

        override val delegates by lazy {
            listOf(
                    ExpandableDelegate(treeNode),
                    MultiLineDelegate(this),
                    InvisibleCheckboxDelegate(this),
                    IndentationDelegate(this),
            )
        }

        override val checkBoxInvisible = false

        protected val disabledOverride = R.color.textDisabled.takeUnless { entryData.canAddSubtask }

        override val name
            get() = MultiLineNameData.Visible(entryData.name, disabledOverride ?: R.color.textPrimary)

        override fun compareTo(other: ModelNode<AbstractHolder>) = if (other is Node) {
            var comparison = entryData.compareTo(other.entryData)
            if (taskListFragment.data!!.reverseOrderForTopLevelNodes && reverseSortOrder)
                comparison = -comparison

            comparison
        } else {
            1
        }

        override fun normalize() = entryData.normalize()

        override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
                entryData.matchesFilterParams(filterParams)

        override fun getMatchResult(query: String) =
                ModelNode.MatchResult.fromBoolean(entryData.matchesQuery(query))

        abstract fun initialize(
                adapterState: AdapterState,
                nodeContainer: NodeContainer<AbstractHolder>,
                showProjects: Boolean,
        ): TreeNode<AbstractHolder>
    }

    private inner class ProjectNode(
            taskAdapter: TaskAdapter,
            val projectData: ProjectData,
            private val copying: Boolean,
    ) : Node(projectData, taskAdapter, true) {

        override val parentNode: ModelNode<AbstractHolder>? = null

        override val indentation = 0

        override val expandedTaskKeys: Set<TaskKey>
            get() {
                if (!treeNode.isExpanded) return setOf()

                return taskNodes.flatMap { it.expandedTaskKeys }.toSet()
            }

        override val expandedProjectKeys: Set<ProjectKey<*>>
            get() {
                if (!treeNode.isExpanded) return setOf()

                return setOf(projectData.projectKey)
            }

        override val widthKey
            get() = MultiLineDelegate.WidthKey(
                    indentation,
                    false,
                    false,
                    true
            )

        override val children
            get() = InstanceTreeTaskNode.getTaskChildren(treeNode, null) {
                (it.modelNode as? TaskNode)?.childTaskData?.name
            }?.let { Pair(it, disabledOverride ?: R.color.textSecondary) }

        override fun initialize(
                adapterState: AdapterState,
                nodeContainer: NodeContainer<AbstractHolder>,
                showProjects: Boolean,
        ): TreeNode<AbstractHolder> {

            val selected = false // todo project

            val expanded = adapterState.expandedProjectKeys.contains(projectData.projectKey)

            treeNode = TreeNode(this, nodeContainer, expanded, selected)

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            taskNodes = projectData.children.map {
                TaskNode(
                        indentation + 1,
                        false,
                        this,
                        it,
                        copying,
                        this
                ).also { treeNodes += it.initialize(adapterState, treeNode, showProjects) }
            }

            treeNode.setChildTreeNodes(treeNodes)

            return treeNode
        }

        override fun onClick(holder: AbstractHolder) {
            startActivity(ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Project(projectData.projectKey)))
        }
    }

    private inner class TaskNode(
            override val indentation: Int,
            reverseSortOrder: Boolean,
            nodeParent: NodeParent,
            val childTaskData: ChildTaskData,
            private val copying: Boolean,
            override val parentNode: ModelNode<AbstractHolder>?,
    ) :
            Node(childTaskData, nodeParent, reverseSortOrder),
            Sortable,
            ThumbnailModelNode {

        override val expandedTaskKeys: Set<TaskKey>
            get() {
                if (!treeNode.isExpanded) return setOf()

                return setOf(childTaskData.taskKey) + taskNodes.flatMap { it.expandedTaskKeys }
            }

        override val expandedProjectKeys = setOf<ProjectKey<*>>()

        override val delegates by lazy {
            super.delegates + listOf(
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

        override fun initialize(
                adapterState: AdapterState,
                nodeContainer: NodeContainer<AbstractHolder>,
                showProjects: Boolean,
        ): TreeNode<AbstractHolder> {
            val projectInfo = childTaskData.projectInfo?.let {
                if (showProjects)
                    it.takeIf { it.assignedTo.isNotEmpty() }?.copy(name = "")
                else
                    it
            }

            val detailsNode = if (projectInfo != null || !childTaskData.note.isNullOrEmpty()) {
                DetailsNode(
                        projectInfo,
                        childTaskData.note,
                        this,
                        indentation + 1
                )
            } else {
                null
            }

            val selected = adapterState.selectedTaskKeys.contains(childTaskData.taskKey)

            val expanded = if (detailsNode != null || childTaskData.children.isNotEmpty())
                adapterState.expandedTaskKeys.contains(childTaskData.taskKey)
            else
                false

            treeNode = TreeNode(this, nodeContainer, expanded, selected)

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            detailsNode?.let { treeNodes += it.initialize(nodeContainer) }

            taskNodes = childTaskData.children.map {
                TaskNode(
                        indentation + 1,
                        false,
                        this,
                        it,
                        copying,
                        this
                ).also { treeNodes += it.initialize(adapterState, treeNode, showProjects) }
            }

            treeNode.setChildTreeNodes(treeNodes)

            return treeNode
        }

        override val children
            get() = InstanceTreeTaskNode.getTaskChildren(treeNode, childTaskData.note) {
                (it.modelNode as? TaskNode)?.childTaskData?.name
            }?.let { Pair(it, disabledOverride ?: R.color.textSecondary) }

        override val details
            get() = if (childTaskData.scheduleText.isNullOrEmpty()) {
                null
            } else {
                Pair(childTaskData.scheduleText, disabledOverride ?: R.color.textSecondary)
            }

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

        override fun getOrdinal() = childTaskData.ordinal

        override fun setOrdinal(ordinal: Double) {
            childTaskData.ordinal = ordinal

            DomainFactory.instance.setOrdinal(
                    taskListFragment.data!!.dataId,
                    childTaskData.taskKey,
                    ordinal
            )
        }
    }

    private interface NodeParent {

        val taskAdapter: TaskAdapter
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
            val entryDatas: List<EntryData>,
            val note: String?,
            val showFab: Boolean,
            val projectInfo: DetailsNode.ProjectInfo?,
    )

    interface EntryData : Comparable<EntryData>, QueryMatchable, FilterParamsMatchable {

        val name: String
        val children: List<EntryData>
        val id: Any
        val canAddSubtask: Boolean

        fun normalize() {
            normalizedFields
        }
    }

    data class ProjectData(
            override val name: String,
            override val children: List<ChildTaskData>,
            val projectKey: ProjectKey<*>,
            val current: Boolean,
            val startExactTimeStamp: Long,
    ) : EntryData {

        override val id = projectKey

        override val canAddSubtask = current
        override val isDeleted = !canAddSubtask

        override val normalizedFields by lazy { listOf(name.normalized()) }

        override val isAssignedToMe = true

        override fun compareTo(other: EntryData): Int {
            check(other is ProjectData)

            return startExactTimeStamp.compareTo(other.startExactTimeStamp)
        }
    }

    data class ChildTaskData(
            override val name: String,
            val scheduleText: String?,
            override val children: List<ChildTaskData>,
            val note: String?,
            val taskKey: TaskKey,
            val imageState: ImageState?,
            val current: Boolean,
            override val canAddSubtask: Boolean,
            var ordinal: Double,
            val projectInfo: DetailsNode.ProjectInfo?,
            override val isAssignedToMe: Boolean,
    ) : EntryData {

        override val id = taskKey

        override val isDeleted = !canAddSubtask

        override fun compareTo(other: EntryData): Int {
            check(other is ChildTaskData)

            return ordinal.compareTo(other.ordinal)
        }

        override val normalizedFields by lazy { listOfNotNull(name, note).map { it.normalized() } }

        override fun normalize() {
            normalizedFields
        }
    }

    interface Listener : ActionModeListener, SnackbarListener, ListItemAddedListener {

        val taskSearch: Observable<FilterCriteria.Full>

        fun setTaskSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()

        fun startCopy(taskKey: TaskKey): Unit = throw UnsupportedOperationException()
    }

    data class RootTaskData(val taskKey: TaskKey, val imageState: ImageState?)

    @Parcelize
    private data class AdapterState(
            val selectedTaskKeys: Set<TaskKey>,
            val expandedTaskKeys: Set<TaskKey>,
            val expandedProjectKeys: Set<ProjectKey<*>>,
    ) : Parcelable {

        constructor() : this(setOf(), setOf(), setOf())
    }
}