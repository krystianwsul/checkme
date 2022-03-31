package com.krystianwsul.checkme.gui.tasks

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentTaskListBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.updates.SetTaskOrdinalDomainUpdate
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditParentHint
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.*
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.utils.webSearchIntent
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.*
import com.krystianwsul.treeadapter.*
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.parcelize.Parcelize
import java.io.Serializable
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

    private val topLevelTaskData get() = parameters?.topLevelTaskData
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
        AndroidDomainUpdater.setTaskEndTimeStamps(
            DomainListenerManager.NotificationType.First(data!!.dataId),
            taskKeys as Set<TaskKey>,
            removeInstances,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { listener.showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
            .flatMapCompletable {
                AndroidDomainUpdater.clearTaskEndTimeStamps(
                    DomainListenerManager.NotificationType.First(data!!.dataId),
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
                R.id.action_task_join -> startActivity(
                    EditActivity.getParametersIntent(
                        EditParameters.Join(
                            taskKeys.map(EditParameters.Join.Joinable::Task),
                            parameters!!.hint,
                        )
                    )
                )
                R.id.action_task_delete -> {
                    checkNotNull(data)

                    RemoveInstancesDialogFragment.newInstance(taskKeys.toSet())
                        .also { it.listener = deleteInstancesListener }
                        .show(childFragmentManager, TAG_REMOVE_INSTANCES)
                }
                R.id.action_task_show_instances -> startActivity(
                    ShowTaskInstancesActivity.newIntent(
                        ShowTaskInstancesActivity.Parameters.Task(taskKeys.single())
                    )
                )
                R.id.actionTaskCopy -> startActivity(getCopyTasksIntent(taskKeys))
                R.id.actionTaskWebSearch -> startActivity(webSearchIntent(selectedNodes.single().entryData.name))
                R.id.actionTaskMigrateDescription -> startActivity(
                    EditActivity.getParametersIntent(EditParameters.MigrateDescription(taskKeys.single()))
                )
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
                val singleTask = selectedNodes.singleOrNull()
                    ?.let { it as TaskNode }
                    ?.childTaskData

                val single = selectedNodes.size == 1

                val current = selectedNodes.map { (it as TaskNode).childTaskData }.all { it.current }

                listOf(
                    R.id.action_task_edit to (single && current),
                    R.id.action_task_join to (!single && current),
                    R.id.action_task_delete to current,
                    R.id.action_task_show_instances to single,
                    R.id.actionTaskCopy to current,
                    R.id.actionTaskWebSearch to single,
                    R.id.actionTaskMigrateDescription to (singleTask?.canMigrateDescription == true),
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
                    R.id.actionTaskMigrateDescription to false,
                )
            }
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            listener.onDestroyActionMode()
            updateFabVisibility("onLastRemoved")
        }
    }

    private var taskListFragmentFabDelegate: BottomFabMenuDelegate.FabDelegate? = null

    val treeViewAdapter: TreeViewAdapter<AbstractHolder> get() = searchDataManager.treeViewAdapter

    val shareData get() = getShareData(treeViewAdapter.displayableNodes)

    private lateinit var adapterState: AdapterState

    lateinit var listener: Listener

    private var showImage = false
    private var imageViewerData: Pair<ImageState, StfalconImageViewer<ImageState>>? = null

    private var scrollTargetMatcher: ListItemAddedScroller.ScrollTargetMatcher.Task? = null

    override fun setTaskScrollTargetMatcher(scrollTargetMatcher: ListItemAddedScroller.ScrollTargetMatcher.Task?) {
        this.scrollTargetMatcher = scrollTargetMatcher
    }

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

        override val emptyTextResId get() = topLevelTaskData?.let { R.string.empty_child } ?: R.string.tasks_empty_root

        override val compositeDisposable = viewCreatedDisposable

        override val filterCriteriaObservable get() = listener.taskSearch

        override fun dataIsImmediate(data: Data) = data.immediate

        override fun getFilterCriteriaFromData(data: Data): FilterCriteria.AllowedFilterCriteria? = null

        override fun instantiateAdapters(filterCriteria: FilterCriteria.AllowedFilterCriteria) =
            TaskAdapter(this@TaskListFragment, filterCriteria).let { it to it.treeViewAdapter }

        override fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>) {
            binding.taskListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            dragHelper.attachToRecyclerView(binding.taskListRecycler)
        }

        override fun initializeModelAdapter(modelAdapter: TaskAdapter, data: Data) {
            if (treeViewAdapterInitialized) adapterState = getAdapterState()

            modelAdapter.initialize(data.taskData, adapterState, data.copying)
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

        val scrollTargetMatcher = this.scrollTargetMatcher ?: return null

        return treeViewAdapter.displayedNodes
            .firstOrNull {
                val modelNode = it.modelNode

                if (modelNode is TaskNode) {
                    if (it.isExpanded) {
                        modelNode.childTaskData.taskKey == scrollTargetMatcher.taskKey
                    } else {
                        modelNode.childTaskData
                            .allTaskKeys
                            .contains(scrollTargetMatcher.taskKey)
                    }
                } else {
                    false
                }
            }
            ?.let(treeViewAdapter.getTreeNodeCollection()::getPosition)
    }

    private fun updateSelectAll() {
        val taskAdapter = treeViewAdapter.treeModelAdapter as TaskAdapter

        listener.setTaskSelectAllVisibility(taskAdapter.nodes.isNotEmpty())
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

    override fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        taskListFragmentFabDelegate = fabDelegate

        updateFabVisibility("setFab")
    }

    private fun updateFabVisibility(source: String) {
        Preferences.tickLog.logLineHour("fab ${hashCode()} $source ${taskListFragmentFabDelegate != null}, ${data != null}, ${!selectionCallback.hasActionMode}")

        taskListFragmentFabDelegate?.run {
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

                        edit(EditParameters.Create(EditParentHint.Task(childTaskData.taskKey)), true)
                    }
                    projectData?.canAddSubtask == true -> {
                        show()

                        val hint = (projectData.projectKey as? ProjectKey.Shared)?.let(EditParentHint::Project)

                        setOnClickListener {
                            selectionCallback.actionMode!!.finish()

                            listener.showFabMenu(ReminderOrNoteMenuDelegate(hint))
                        }
                    }
                    else -> hide()
                }
            } else {
                if (data!!.taskData.showFab) {
                    show()

                    val hint = parameters!!.hint

                    if (data!!.showFirstSchedule) {
                        setOnClickListener { listener.showFabMenu(ReminderOrNoteMenuDelegate(hint)) }
                    } else {
                        edit(EditParameters.Create(hint, showFirstSchedule = data!!.showFirstSchedule))
                    }
                } else {
                    hide()
                }
            }
        }
    }

    override fun clearFab() {
        taskListFragmentFabDelegate = null
    }

    override fun onDestroyView() {
        viewCreatedObservable.accept(false)

        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class TaskAdapter(
        val taskListFragment: TaskListFragment,
        filterCriteria: FilterCriteria.AllowedFilterCriteria
    ) :
        BaseAdapter(),
        NodeParent,
        ActionModeCallback by taskListFragment.selectionCallback {

        lateinit var nodes: MutableList<Node>
            private set

        val treeViewAdapter = TreeViewAdapter(
            this,
            TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
            filterCriteria,
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
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            treeNodes += DetailsNode(
                taskData.projectInfo,
                taskData.note,
                null,
                0,
            ).initialize(treeNodeCollection)

            taskListFragment.topLevelTaskData
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
                        is ProjectData -> listOf(ProjectNode(this, it, copying))
                        is ChildTaskData -> listOf(it.toRootWrapper())
                        else -> throw IllegalArgumentException()
                    }
                }
                .toMutableList()

            treeNodes += nodes.map { it.initialize(adapterState, treeNodeCollection) }

            treeNodeCollection.nodes = treeNodes
        }
    }

    private abstract class Node(
        val entryData: EntryData,
        private val nodeParent: NodeParent,
        reverseSortOrder: Boolean,
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
                InvisibleCheckboxDelegate(this),
                IndentationDelegate(this),
                MultiLineDelegate(this), // this one always has to be last, because it depends on layout changes from prev
            )
        }

        override val checkBoxInvisible = false

        protected val disabledOverride = R.color.textDisabled.takeUnless { entryData.canAddSubtask }

        protected val name = MultiLineRow.Visible(entryData.name, disabledOverride ?: R.color.textPrimary)

        protected val reverseSortOrderFinal = taskListFragment.data!!.reverseOrderForTopLevelNodes && reverseSortOrder

        override fun compareTo(other: ModelNode<AbstractHolder>) = if (other is Node) {
            var comparison = entryData.compareTo(other.entryData)
            if (reverseSortOrderFinal)
                comparison = -comparison

            comparison
        } else {
            1
        }

        override fun normalize() = entryData.normalize()

        override fun getMatchResult(search: SearchCriteria.Search) =
            ModelNode.MatchResult.fromBoolean(entryData.matchesSearch(search))

        abstract fun initialize(
            adapterState: AdapterState,
            nodeContainer: NodeContainer<AbstractHolder>,
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

        override val projectExpansionStates
            get() =
                mapOf(projectData.projectKey to treeNode.getSaveExpansionState()).filterValuesNotNull()

        override val widthKey
            get() = MultiLineDelegate.WidthKey(
                indentation,
                false,
                false,
                treeNode.expandVisible,
            )

        override val rowsDelegate = object : MultiLineModelNode.RowsDelegate {

            override fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
                val children = InstanceTreeTaskNode.getTaskChildren<TaskNode>(isExpanded, allChildren, null) {
                    it.childTaskData.name
                }?.let { MultiLineRow.Visible(it, disabledOverride ?: R.color.textSecondary) }

                return listOfNotNull(name, children)
            }
        }

        override val isSelectable = true

        override fun initialize(
            adapterState: AdapterState,
            nodeContainer: NodeContainer<AbstractHolder>,
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
                ).also { treeNodes += it.initialize(adapterState, treeNode) }
            }

            treeNode.setChildTreeNodes(treeNodes)

            return treeNode
        }

        override fun onClick(holder: AbstractHolder) {
            startActivity(
                ShowTasksActivity.newIntent(
                    if (parameters is Parameters.Notes) {
                        ShowTasksActivity.Parameters.Unscheduled(projectData.projectKey)
                    } else {
                        ShowTasksActivity.Parameters.Project(projectData.projectKey)
                    }
                )
            )
        }

        override fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean) = hasVisibleChildren
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
        ThumbnailModelNode,
        DetailsNode.Parent {

        override val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>
            get() = mapOf(childTaskData.taskKey to treeNode.getSaveExpansionState()).filterValuesNotNull() +
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
                treeNode.expandVisible,
            )

        override fun initialize(
            adapterState: AdapterState,
            nodeContainer: NodeContainer<AbstractHolder>,
        ): TreeNode<AbstractHolder> {
            val detailsNode = DetailsNode(
                childTaskData.projectInfo,
                childTaskData.note,
                this,
                indentation + 1,
            )

            treeNode = TreeNode(
                this,
                nodeContainer,
                adapterState.selectedTaskKeys.contains(childTaskData.taskKey),
                adapterState.taskExpansionStates[childTaskData.taskKey],
            )

            val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

            treeNodes += detailsNode.initialize(nodeContainer)

            taskNodes = childTaskData.children.map {
                TaskNode(
                    indentation + 1,
                    false,
                    this,
                    it,
                    copying,
                    this
                ).also { treeNodes += it.initialize(adapterState, treeNode) }
            }

            treeNode.setChildTreeNodes(treeNodes)

            return treeNode
        }

        override val rowsDelegate = object : DetailsNode.ProjectRowsDelegate(
            childTaskData.projectInfo,
            disabledOverride ?: R.color.textSecondary,
        ) {

            private val details = childTaskData.scheduleText
                .takeIf { !it.isNullOrEmpty() }
                ?.let { MultiLineRow.Visible(it, disabledOverride ?: R.color.textSecondary) }

            override fun getRowsWithoutProject(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
                val children = InstanceTreeTaskNode.getTaskChildren<TaskNode>(isExpanded, allChildren, childTaskData.note) {
                    it.childTaskData.name
                }?.let { MultiLineRow.Visible(it, disabledOverride ?: R.color.textSecondary) }

                return listOfNotNull(name, details, children)
            }
        }

        override val isSelectable = !copying

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

        override val reversedOrdinal get() = reverseSortOrderFinal

        override fun getOrdinal() = childTaskData.ordinal

        override fun setOrdinal(ordinal: Ordinal) {
            SetTaskOrdinalDomainUpdate(
                taskListFragment.data!!
                    .dataId
                    .toFirst(),
                childTaskData.taskKey,
                ordinal,
            ).perform(AndroidDomainUpdater).subscribe()
        }

        override fun canDropOn(other: Sortable): Boolean {
            val otherTaskNode = other as? TaskNode ?: return false

            return treeNode.parent == otherTaskNode.treeNode.parent
        }
    }

    private interface NodeParent {

        val taskAdapter: TaskAdapter
    }

    data class Data(
        val dataId: DataId,
        val immediate: Boolean,
        val taskData: TaskData,
        val reverseOrderForTopLevelNodes: Boolean, // todo this is stupid and should be combined with the other param it's used with
        val copying: Boolean = false,
        val showFirstSchedule: Boolean = true,
    )

    data class TaskData(
        val entryDatas: List<EntryData>,
        val note: String?,
        val showFab: Boolean,
        val projectInfo: DetailsNode.ProjectInfo?,
    )

    interface EntryData : Comparable<EntryData>, QueryMatchable {

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
        override val matchesSearch: Boolean,
    ) : EntryData {

        override val id = projectKey

        override val canAddSubtask = current

        override val normalizedFields by lazy { listOf(name.normalized()) }

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
        val canMigrateDescription: Boolean,
        val ordinal: Ordinal,
        val projectInfo: DetailsNode.ProjectInfo?,
        override val matchesSearch: Boolean,
    ) : EntryData {

        override val id = taskKey

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

        val taskSearch: Observable<FilterCriteria.AllowedFilterCriteria>

        fun setTaskSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()

        fun startCopy(taskKey: TaskKey): Unit = throw UnsupportedOperationException()

        fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate)
    }

    data class TopLevelTaskData(val taskKey: TaskKey, val imageState: ImageState?)

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
        open val topLevelTaskData: TopLevelTaskData? = null

        abstract val hint: EditParentHint?

        open val canDrag = true

        data class All(override val data: Data, override val canDrag: Boolean) : Parameters() {

            override val hint: EditParentHint? = null
        }

        data class Notes(
            override val data: Data,
            override val canDrag: Boolean,
            override val hint: EditParentHint.Project? = null,
        ) : Parameters()

        data class Project(override val data: Data, val projectKey: ProjectKey<*>) : Parameters() {

            override val hint get() = (projectKey as? ProjectKey.Shared)?.let { EditParentHint.Project(it, false) }
        }

        data class Task(override val data: Data, override val topLevelTaskData: TopLevelTaskData) : Parameters() {

            override val hint = EditParentHint.Task(topLevelTaskData.taskKey)
        }
    }
}