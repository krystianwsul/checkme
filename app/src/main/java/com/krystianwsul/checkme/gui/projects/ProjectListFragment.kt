package com.krystianwsul.checkme.gui.projects


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.*
import io.reactivex.rxkotlin.plusAssign
import java.util.*

class ProjectListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_PROJECT_IDS = "selectedProjectIds"

        fun newInstance() = ProjectListFragment()
    }

    private lateinit var projectListProgress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var projectListRecycler: RecyclerView

    private var projectListFab: FloatingActionButton? = null

    private lateinit var treeViewAdapter: TreeViewAdapter

    private var dataId: Int? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val selected = treeViewAdapter.selectedNodes
            check(!selected.isEmpty())

            val projectNodes = selected.map { it.modelNode as ProjectListAdapter.ProjectNode }

            val projectIds = projectNodes.asSequence()
                    .map { it.projectData.id }
                    .toSet()

            when (itemId) {
                R.id.action_project_delete -> {
                    check(dataId != null)

                    for (treeNode in selected) {
                        val projectNode = treeNode.modelNode as ProjectListAdapter.ProjectNode

                        projectNode.remove(x)

                        decrementSelected(x)

                        DomainFactory.getKotlinDomainFactory().setProjectEndTimeStamps(dataId!!, SaveService.Source.GUI, projectIds)
                    }
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_projects, actionMode!!.menu)

            updateFabVisibility()
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) = updateFabVisibility()

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var selectedProjectIds: Set<String> = setOf()

    private lateinit var projectListViewModel: ProjectListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(SELECTED_PROJECT_IDS) == true) {
            val selectedProjectIds = savedInstanceState.getStringArrayList(SELECTED_PROJECT_IDS)!!

            this.selectedProjectIds = HashSet(selectedProjectIds)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_project_list, container, false)

        projectListProgress = view.findViewById(R.id.projectListProgress)

        emptyText = view.findViewById(R.id.emptyText)

        projectListRecycler = view.findViewById(R.id.projectListRecycler)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectListRecycler.layoutManager = LinearLayoutManager(activity)

        projectListViewModel = getViewModel<ProjectListViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ProjectListViewModel.Data) {
        dataId = data.dataId

        val hide = mutableListOf<View>(projectListProgress)
        val show: View

        if (data.projectDatas.isEmpty()) {
            hide.add(projectListRecycler)
            show = emptyText

            emptyText.setText(R.string.projects_empty)
        } else {
            show = projectListRecycler
            hide.add(emptyText)
        }

        animateVisibility(listOf(show), hide)

        if (this::treeViewAdapter.isInitialized) {
            selectedProjectIds = treeViewAdapter.selectedNodes
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toSet()

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as ProjectListAdapter).initialize(data.projectDatas)
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        } else {
            val projectListAdapter = ProjectListAdapter()
            projectListAdapter.initialize(data.projectDatas)
            treeViewAdapter = projectListAdapter.treeViewAdapter
            projectListRecycler.adapter = treeViewAdapter

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)
            }
        }

        updateFabVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized)
            selectedProjectIds = treeViewAdapter.selectedNodes
                    .asSequence()
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toSet()

        outState.putStringArrayList(SELECTED_PROJECT_IDS, ArrayList(selectedProjectIds))
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        projectListFab = floatingActionButton

        projectListFab!!.setOnClickListener { startActivity(ShowProjectActivity.newIntent(activity!!)) }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        projectListFab?.run {
            if (dataId != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        projectListFab = null
    }

    private inner class ProjectListAdapter : TreeModelAdapter {

        val treeViewAdapter = TreeViewAdapter(this)

        private lateinit var projectNodes: MutableList<ProjectNode>
        private lateinit var treeNodeCollection: TreeNodeCollection

        fun initialize(projectDatas: SortedMap<String, ProjectListViewModel.ProjectData>) {
            projectNodes = projectDatas.values
                    .map { ProjectNode(this, it) }
                    .toMutableList()

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = projectNodes.map { it.initialize(treeNodeCollection) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(requireActivity().layoutInflater.inflate(R.layout.row_list, parent, false)!!)

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)

        private fun remove(projectNode: ProjectNode, x: TreeViewAdapter.Placeholder) {
            check(projectNodes.contains(projectNode))

            projectNodes.remove(projectNode)

            treeNodeCollection.remove(projectNode.treeNode, x)
        }

        inner class ProjectNode(private val projectListAdapter: ProjectListAdapter, val projectData: ProjectListViewModel.ProjectData) : GroupHolderNode(0) {

            override val id = projectData.id

            public override lateinit var treeNode: TreeNode
                private set

            fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
                treeNode = TreeNode(this, treeNodeCollection, false, selectedProjectIds.contains(projectData.id))
                treeNode.setChildTreeNodes(ArrayList())
                return treeNode
            }

            override val name get() = Triple(projectData.name, colorPrimary, true)

            override val details get() = Pair(projectData.users, colorSecondary)

            override val children: Pair<String, Int>? = null

            override val itemViewType = 0

            override val isSelectable = true

            override fun onClick() = activity!!.startActivity(ShowProjectActivity.newIntent(activity!!, projectData.id))

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override fun compareTo(other: ModelNode): Int {
                check(other is ProjectNode)

                return projectData.id.compareTo(other.projectData.id)
            }

            fun remove(x: TreeViewAdapter.Placeholder) = projectListAdapter.remove(this, x)
        }
    }
}
