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
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
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

    private val parametersRelay = BehaviorRelay.create<Parameters>()

    var parameters: Parameters?
        get() = parametersRelay.value
        set(value) {
            checkNotNull(value)

            parametersRelay.accept(value)
        }

    private val rootTaskData get() = parameters?.rootTaskData
    private val data get() = parameters?.data

    private val dragHelper by lazy {
        object : DragHelper() {

            override fun getTreeViewAdapter(): TreeViewAdapter<AbstractHolder> = treeViewAdapter

            override val recyclerView get() = binding.taskListRecycler
        }
    }

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        checkNotNull(data)

        @Suppress("UNCHECKED_CAST")
        DomainFactory.instance
                .setTaskEndTimeStamps(
                        DomainListenerManager.NotificationType.First(data!!.dataId),
                        SaveService.Source.GUI,
                        taskKeys as Set<TaskKey>,
                        removeInstances,
                )
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapMaybe { listener.showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
                .flatMapCompletable {
                    DomainFactory.instance.clearTaskEndTimeStamps(
                            DomainListenerManager.NotificationType.First(data!!.dataId),
                            SaveService.Source.GUI,
                            it,
                    )
                }
                .subscribe()
                .addTo(createDisposable)
    }

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override val bottomBarData by lazy {
            Triple(listener.getBottomBar(), R.menu.menu_edit_tasks, listener::initBottomBar)
        }

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            val selectedTreeNodes = treeViewAdapter.selectedNodes
            check(selectedTreeNodes.isNotEmpty())

            val selectedNodes = selectedTreeNodes.map { it.modelNode as Node }

            val childTaskDatas by lazy { selectedNodes.map { it as TaskNode }.map { it.childTaskData } }
            val taskKeys by lazy { childTaskDatas.map { it.taskKey } }

            when (itemId) {
                R.id.action_task_share -> Utils.share(requireActivity(), getShareData(selectedTreeNodes))
                R.id.action_task_edit -> startActivity(
                        EditActivity.getParametersIntent(EditParameters.Edit(childTaskDatas.single().taskKey))
                )
                R.id.action_task_join -> startActivity(EditActivity.getParametersIntent(EditParameters.Join(
                        taskKeys.map(EditParameters.Join.Joinable::Task),
                        parameters!!.hint,
                )))
                R.id.action_task_delete -> {
                    checkNotNull(data)

                    RemoveInstancesDialogFragment.newInstance(taskKeys.toSet())
                            .also { it.listener = deleteInstancesListener }
                            .show(childFragmentManager, TAG_REMOVE_INSTANCES)
                }
                R.id.action_task_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKeys.single()))
                R.id.actionTaskCopy -> startActivity(getCopyTasksIntent(taskKeys))
                R.id.actionTaskWebSearch -> startActivity(webSearchIntent(selectedNodes.single().entryData.name))
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
            val selectedNodes = treeViewAdapter.selectedNodes.map { it.modelNode as Node }
            check(selectedNodes.isNotEmpty())

            val allTasks = selectedNodes.all { it is TaskNode }

            return if (allTasks) {
                val single = selectedNodes.size == 1

                val current = selectedNodes.map { (it as TaskNode).childTaskData }.all { it.current }

                listOf(
                        R.id.action_task_edit to (single && current),
                        R.id.action_task_join to (!single && current),
                        R.id.action_task_delete to current,
                        R.id.action_task_show_instances to single,
                        R.id.actionTaskCopy to current,
                        R.id.actionTaskWebSearch to single,
                )
            } else {
                val single = selectedNodes.size == 1

                listOf(
                        R.id.action_task_edit to false,
                        R.id.action_task_join to false,
                        R.id.action_task_delete to false,
                        R.id.action_task_show_instances to false,
                        R.id.actionTaskCopy to false,
                        R.id.actionTaskWebSearch to single,
                )
            }
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            listener.onDestroyActionMode()
            updateFabVisibility("onLastRemoved")
        }
    }

    private var taskListFragmentFab: FloatingActionButton? = null

    val treeViewAdapter: TreeViewAdapter<AbstractHolder> get() = searchDataManager.treeViewAdapter

    val shareData get() = getShareData(treeViewAdapter.displayableNodes)

    private lateinit var adapterState: AdapterState

    private val listener get() = activity as Listener

    private var showImage = false
    private var imageViewerData: Pair<ImageState, StfalconImageViewer<ImageState>>? = null

    override var scrollToTaskKey: TaskKey? = null
    override val listItemAddedListener get() = listener
    override val recyclerView: RecyclerView get() = binding.taskListRecycler

    private val bindingProperty = ResettableProperty<FragmentTaskListBinding>()
    private var binding by bindingProperty

    val emptyTextLayout get() = binding.taskListEmptyTextInclude.emptyTextLayout

    override val scrollDisposable = viewCreatedDisposable

    private val viewCreatedObservable = BehaviorRelay.createDefault(false)

    private val searchDataManager = object : SearchDataManager<Data, TaskAdapter>(
            viewCreatedObservable,
            parametersRelay.map { it.data },
    ) {

        override val recyclerView get() = binding.taskListRecycler
        override val progressView get() = binding.taskListProgress
        override val emptyTextBinding get() = binding.taskListEmptyTextInclude

        override val emptyTextResId get() = rootTaskData?.let { R.string.empty_child } ?: R.string.tasks_empty_root

        override val compositeDisposable = viewCreatedDisposable

        override val filterCriteriaObservable get() = listener.taskSearch.cast<FilterCriteria>()

        override fun dataIsImmediate(data: Data) = data.immediate

        override fun getFilterCriteriaFromData(data: Data): FilterCriteria? = null

        override fun filterDataChangeRequiresReinitializingModelAdapter(
                oldFilterCriteria: FilterCriteria,
                newFilterCriteria: FilterCriteria,
        ): Boolean {
            fun FilterCriteria.showProjects() = (this as? FilterCriteria.Full)?.filterParams?.showProjects

            return rootTaskData == null && oldFilterCriteria.showProjects() != newFilterCriteria.showProjects()
        }

        override fun instantiateAdapters(filterCriteria: FilterCriteria) =
                TaskAdapter(this@TaskListFragment, filterCriteria).let { it to it.treeViewAdapter }

        override fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>) {
            binding.taskListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.taskListRecycler)
        }

        override fun initializeModelAdapter(modelAdapter: TaskAdapter, data: Data, filterCriteria: FilterCriteria) {
            val showProjects = (filterCriteria as? FilterCriteria.Full)?.filterParams?.showProjects == true

            if (treeViewAdapterInitialized) adapterState = getAdapterState()

            modelAdapter.initialize(data.taskData, adapterState, data.copying, showProjects)
        }

        override fun updateTreeViewAdapterAfterModelAdapterInitialization(
                treeViewAdapter: TreeViewAdapter<AbstractHolder>,
                data: Data,
                initial: Boolean,
                placeholder: TreeViewAdapter.Placeholder,
        ) = selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, placeholder, initial)

        override fun onDataChanged() {
            updateFabVisibility("initialize")

            updateSelectAll()

            tryScroll()
        }

        override fun onFilterCriteriaChanged() = updateFabVisibility("search")
    }

    private fun getShareData(treeNodes: List<TreeNode<AbstractHolder>>): String {
        // we can assume that parent nodes will come before child nodes, so distinct will just take the first one
        return treeNodes.asSequence()
                .flatMap { it.displayableNodes }
                .filter { it.modelNode is Node }
                .map { it.modelNode as Node }
                .distinct()
                .map { "-".repeat(it.indentation) + it.entryData.name }
                .joinToString("\n")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is Listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterState = savedInstanceState?.getParcelable(KEY_ADAPTER_STATE) ?: AdapterState()

        savedInstanceState?.run {
            searchDataManager.setInitialFilterCriteria(getParcelable(KEY_SEARCH_DATA)!!)

            showImage = getBoolean(KEY_SHOW_IMAGE)
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_REMOVE_INSTANCES)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentTaskListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.taskListRecycler.layoutManager = LinearLayoutManager(context)

        viewCreatedObservable.accept(true)

        searchDataManager.subscribe()
    }

    private fun getAllChildTaskDatas(childTaskData: ChildTaskData): List<ChildTaskData> = listOf(
            listOf(listOf(childTaskData)),
            childTaskData.children.map(::getAllChildTaskDatas)
    ).flatten().flatten()

    private val ChildTaskData.allTaskKeys get() = getAllChildTaskDatas(this).map { it.taskKey }

    override fun findItem(): Int? {
        if (!searchDataManager.treeViewAdapterInitialized) return null

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
        val taskAdapter = treeModelAdapter as TaskAdapter

        AdapterState(
                selectedNodes.mapNotNull { (it.modelNode as? TaskNode)?.childTaskData?.taskKey }.toSet(),
                taskAdapter.taskExpansionStates,
                selectedNodes.mapNotNull { (it.modelNode as? ProjectNode)?.projectData?.projectKey }.toSet(),
                taskAdapter.projectExpansionStates,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (searchDataManager.treeViewAdapterInitialized)
                putParcelable(KEY_ADAPTER_STATE, getAdapterState())

            putParcelable(KEY_SEARCH_DATA, searchDataManager.filterCriteria)

            putBoolean(KEY_SHOW_IMAGE, imageViewerData != null)
        }
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        taskListFragmentFab = floatingActionButton

        updateFabVisibility("setFab")
    }

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

                val childTaskData = selectedNodes.singleOrNull()?.let { (it.modelNode as? TaskNode)?.childTaskData }
                val projectData = selectedNodes.singleOrNull()?.let { (it.modelNode as? ProjectNode)?.projectData }

                when {
                    childTaskData?.canAddSubtask == true -> {
                        show()

                        edit(EditParameters.Create(EditActivity.Hint.Task(childTaskData.taskKey)), true)
                    }
                    projectData?.canAddSubtask == true -> {
                        show()

                        val hint = (projectData.projectKey as? ProjectKey.Shared)?.let { EditActivity.Hint.Project(it) }

                        edit(EditParameters.Create(hint))
                    }
                    else -> hide()
                }
            } else {
                if (data!!.taskData.showFab) {
                    show()

                    edit(EditParameters.Create(parameters!!.hint, showFirstSchedule = data!!.showFirstSchedule))
                } else {
                    hide()
                }
            }
        }
    }

    override fun clearFab() {
        taskListFragmentFab = null
    }

    override fun onDestroyView() {
        viewCreatedObservable.accept(false)

        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class TaskAdapter(val taskListFragment: TaskListFragment, filterCriteria: FilterCriteria) :
            BaseAdapter(),
            NodeParent,
            ActionModeCallback by taskListFragment.selectionCallback {

        lateinit var nodes: MutableList<Node>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
                filterCriteria
        )

        public override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        override val taskAdapter = this

        val taskExpansionStates get() = nodes.map { it.taskExpansionStates }.flatten()
        val projectExpansionStates get() = nodes.map { it.projectExpansionStates }.flatten()

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

            treeNodes += nodes.map { it.initialize(adapterState, treeNodeCollection, showProjects) }

            treeNodeCollection.nodes = treeNodes
        }

        override fun scrollToTop() = this@TaskListFragment.scrollToTop()
    }

    private abstract class Node(
            val entryData: EntryData,
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

        abstract val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>
        abstract val projectExpansionStates: Map<ProjectKey<*>, TreeNode.ExpansionState>

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

        override val taskExpansionStates get() = taskNodes.map { it.taskExpansionStates }.flatten()
        override val projectExpansionStates get() = mapOf(projectData.projectKey to treeNode.expansionState)

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

        override val isSelectable = true

        override fun initialize(
                adapterState: AdapterState,
                nodeContainer: NodeContainer<AbstractHolder>,
                showProjects: Boolean,
        ): TreeNode<AbstractHolder> {
            treeNode = TreeNode(
                    this,
                    nodeContainer,
                    adapterState.selectedProjectKeys.contains(projectData.projectKey),
                    adapterState.projectExpansionStates[projectData.projectKey],
            )

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            taskNodes = projectData.children.map {
                TaskNode(
                        indentation + 1,
                        true,
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

        override val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>
            get() = mapOf(childTaskData.taskKey to treeNode.expansionState) +
                    taskNodes.map { it.taskExpansionStates }.flatten()

        override val projectExpansionStates = emptyMap<ProjectKey<*>, TreeNode.ExpansionState>()

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

            treeNode = TreeNode(
                    this,
                    nodeContainer,
                    adapterState.selectedTaskKeys.contains(childTaskData.taskKey),
                    adapterState.taskExpansionStates[childTaskData.taskKey],
            )

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

            return if (taskListFragment.parameters?.canDrag == true
                    && treeNodeCollection.selectedChildren.isEmpty()
                    && treeNode.parent.displayedChildNodes.none { it.isExpanded }
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

            DomainFactory.instance
                    .setOrdinal(
                            DomainListenerManager.NotificationType.Skip(taskListFragment.data!!.dataId),
                            childTaskData.taskKey,
                            ordinal,
                    )
                    .subscribe()
                    .addTo(createDisposable)
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
            val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
            val selectedProjectKeys: Set<ProjectKey<*>>,
            val projectExpansionStates: Map<ProjectKey<*>, TreeNode.ExpansionState>,
    ) : Parcelable {

        constructor() : this(setOf(), mapOf(), setOf(), mapOf())
    }

    sealed class Parameters {

        abstract val data: Data
        open val rootTaskData: RootTaskData? = null

        abstract val hint: EditActivity.Hint?

        open val canDrag = true

        data class All(override val data: Data, override val canDrag: Boolean) : Parameters() {

            override val hint: EditActivity.Hint? = null
        }

        data class Project(override val data: Data, val projectKey: ProjectKey<*>) : Parameters() {

            override val hint get() = (projectKey as? ProjectKey.Shared)?.let(EditActivity.Hint::Project)
        }

        data class Task(override val data: Data, override val rootTaskData: RootTaskData) : Parameters() {

            override val hint = EditActivity.Hint.Task(rootTaskData.taskKey)
        }
    }
}