package com.krystianwsul.checkme.gui.projects


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearProjectEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setProjectEndTimeStamps
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderAdapter
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NameData
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_project_list.*
import java.io.Serializable
import java.util.*

class ProjectListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_PROJECT_IDS = "selectedProjectIds"

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newInstance() = ProjectListFragment()
    }

    private var projectListFab: FloatingActionButton? = null

    lateinit var treeViewAdapter: TreeViewAdapter<NodeHolder>
        private set

    private var data: ProjectListViewModel.Data? = null

    private val mainActivity get() = activity as MainActivity

    private val listener get() = activity as ProjectListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_projects, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder): Boolean {
            val projectIds = treeViewAdapter.selectedNodes
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toHashSet()

            check(projectIds.isNotEmpty())

            when (itemId) {
                R.id.action_project_delete -> {
                    checkNotNull(data)

                    RemoveInstancesDialogFragment.newInstance(projectIds)
                            .also { it.listener = deleteInstancesListener }
                            .show(childFragmentManager, TAG_REMOVE_INSTANCES)
                }
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            mainActivity.onCreateActionMode(actionMode!!)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            mainActivity.onDestroyActionMode()
        }
    }

    private var selectedProjectIds: Set<ProjectKey.Shared> = setOf()

    private lateinit var projectListViewModel: ProjectListViewModel

    private val deleteInstancesListener = { projectIds: Serializable, removeInstances: Boolean ->
        checkNotNull(data)

        @Suppress("UNCHECKED_CAST")
        val projectUndoData = DomainFactory.instance.setProjectEndTimeStamps(
                0,
                SaveService.Source.GUI,
                projectIds as Set<ProjectKey.Shared>,
                removeInstances
        )

        mainActivity.showSnackbarRemoved(projectIds.size) {
            DomainFactory.instance.clearProjectEndTimeStamps(0, SaveService.Source.GUI, projectUndoData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(SELECTED_PROJECT_IDS) == true) {
            val selectedProjectIds = savedInstanceState.getParcelableArrayList<ProjectKey.Shared>(SELECTED_PROJECT_IDS)!!

            this.selectedProjectIds = HashSet(selectedProjectIds)
        }

        (childFragmentManager.findFragmentByTag(TAG_REMOVE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_project_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectListRecycler.layoutManager = LinearLayoutManager(activity)

        projectListViewModel = getViewModel<ProjectListViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ProjectListViewModel.Data) {
        this.data = data

        val hide = mutableListOf<View>(projectListProgress)
        val show: View

        if (data.projectDatas.isEmpty()) {
            hide.add(projectListRecycler)
            show = emptyTextLayout

            emptyText.setText(R.string.projects_empty)
        } else {
            show = projectListRecycler
            hide.add(emptyTextLayout)
        }

        animateVisibility(listOf(show), hide, data.immediate)

        if (this::treeViewAdapter.isInitialized) {
            selectedProjectIds = treeViewAdapter.selectedNodes
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toSet()

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as ProjectListAdapter).initialize(data.projectDatas)
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        } else {
            val projectListAdapter = ProjectListAdapter()
            projectListAdapter.initialize(data.projectDatas)
            treeViewAdapter = projectListAdapter.treeViewAdapter
            projectListRecycler.adapter = treeViewAdapter
            projectListRecycler.itemAnimator = CustomItemAnimator()

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        }

        updateFabVisibility()
        updateSelectAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized)
            selectedProjectIds = treeViewAdapter.selectedNodes
                    .asSequence()
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toSet()

        outState.putParcelableArrayList(SELECTED_PROJECT_IDS, ArrayList(selectedProjectIds))
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        projectListFab = floatingActionButton

        projectListFab!!.setOnClickListener { startActivity(ShowProjectActivity.newIntent(requireActivity())) }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        projectListFab?.run {
            if (data != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    private fun updateSelectAll() = mainActivity.setProjectSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    override fun clearFab() {
        projectListFab = null
    }

    private inner class ProjectListAdapter : GroupHolderAdapter() {

        val treeViewAdapter = TreeViewAdapter(this, Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress))

        private lateinit var projectNodes: MutableList<ProjectNode>

        override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
            private set

        fun initialize(projectDatas: SortedMap<ProjectKey.Shared, ProjectListViewModel.ProjectData>) {
            projectNodes = projectDatas.values
                    .map(::ProjectNode)
                    .toMutableList()

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = projectNodes.map { it.initialize(treeNodeCollection) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(requireActivity().layoutInflater.inflate(R.layout.row_list, parent, false)!!)

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)

        inner class ProjectNode(val projectData: ProjectListViewModel.ProjectData) : GroupHolderNode(0) {

            override val ripple = true

            override val id = projectData.id

            public override lateinit var treeNode: TreeNode<NodeHolder>
                private set

            fun initialize(treeNodeCollection: TreeNodeCollection<NodeHolder>): TreeNode<NodeHolder> {
                treeNode = TreeNode(this, treeNodeCollection, false, selectedProjectIds.contains(projectData.id))
                treeNode.setChildTreeNodes(ArrayList())
                return treeNode
            }

            override val name get() = NameData(projectData.name)

            override val details get() = Pair(projectData.users, colorSecondary)

            override val children: Pair<String, Int>? = null

            override val itemViewType = 0

            override val isSelectable = true

            override fun onClick(holder: NodeHolder) = startActivity(ShowProjectActivity.newIntent(activity!!, projectData.id))

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override fun compareTo(other: ModelNode<NodeHolder>): Int {
                check(other is ProjectNode)

                return projectData.id.compareTo(other.projectData.id)
            }
        }
    }

    interface ProjectListListener : SnackbarListener, ActionModeListener {

        fun setProjectSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
