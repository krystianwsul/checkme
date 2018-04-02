package com.krystianwsul.checkme.gui.projects


import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.loaders.ProjectListLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.treeadapter.*
import junit.framework.Assert
import kotlinx.android.synthetic.main.row_project.view.*
import java.util.*

class ProjectListFragment : AbstractFragment(), LoaderManager.LoaderCallbacks<ProjectListLoader.Data>, FabUser {

    companion object {

        private const val SELECTED_PROJECT_IDS = "selectedProjectIds"

        fun newInstance() = ProjectListFragment()
    }

    private lateinit var projectListProgress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var projectListRecycler: RecyclerView

    private var projectListFab: FloatingActionButton? = null

    private var treeViewAdapter: TreeViewAdapter? = null

    private var dataId: Int? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun unselect() {
            treeViewAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            Assert.assertTrue(treeViewAdapter != null)

            val selected = treeViewAdapter!!.selectedNodes
            Assert.assertTrue(!selected.isEmpty())

            val projectNodes = selected.map { it.modelNode as ProjectListAdapter.ProjectNode }

            val projectIds = projectNodes.map { it.projectData.id }.toSet()

            when (menuItem.itemId) {
                R.id.action_project_delete -> {
                    Assert.assertTrue(dataId != null)

                    for (treeNode in selected) {
                        val projectNode = treeNode.modelNode as ProjectListAdapter.ProjectNode

                        projectNode.remove()

                        decrementSelected()
                    }

                    DomainFactory.getDomainFactory().setProjectEndTimeStamps(activity!!, dataId!!, SaveService.Source.GUI, projectIds)
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded() {
            Assert.assertTrue(treeViewAdapter != null)

            treeViewAdapter!!.updateDisplayedNodes {
                (activity as AppCompatActivity).startSupportActionMode(this)
            }

            actionMode!!.menuInflater.inflate(R.menu.menu_projects, actionMode!!.menu)

            updateFabVisibility()
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(action: () -> Unit) {
            Assert.assertTrue(treeViewAdapter != null)

            treeViewAdapter!!.updateDisplayedNodes(action)

            updateFabVisibility()
        }

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var selectedProjectIds: Set<String> = setOf()

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        projectListRecycler.layoutManager = LinearLayoutManager(activity)

        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ProjectListLoader(activity!!)

    override fun onLoadFinished(loader: Loader<ProjectListLoader.Data>, data: ProjectListLoader.Data) {
        dataId = data.dataId

        projectListProgress.visibility = View.GONE
        if (data.projectDatas.isEmpty()) {
            projectListRecycler.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.setText(R.string.projects_empty)
        } else {
            projectListRecycler.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

        if (treeViewAdapter != null)
            selectedProjectIds = treeViewAdapter!!.selectedNodes
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toSet()

        treeViewAdapter = ProjectListAdapter().initialize(data.projectDatas)
        projectListRecycler.adapter = treeViewAdapter

        selectionCallback.setSelected(treeViewAdapter!!.selectedNodes.size)

        updateFabVisibility()
    }

    override fun onLoaderReset(loader: Loader<ProjectListLoader.Data>) = Unit

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (treeViewAdapter != null)
            selectedProjectIds = treeViewAdapter!!.selectedNodes
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
        if (projectListFab == null)
            return

        if (dataId != null && !selectionCallback.hasActionMode()) {
            projectListFab!!.show()
        } else {
            projectListFab!!.hide()
        }
    }

    override fun clearFab() {
        if (projectListFab == null)
            return

        projectListFab!!.setOnClickListener(null)

        projectListFab = null
    }

    private inner class ProjectListAdapter : TreeModelAdapter {

        private lateinit var projectNodes: MutableList<ProjectNode>
        private lateinit var treeViewAdapter: TreeViewAdapter
        private lateinit var treeNodeCollection: TreeNodeCollection

        fun initialize(projectDatas: TreeMap<String, ProjectListLoader.ProjectData>): TreeViewAdapter {
            projectNodes = projectDatas.values
                    .map { ProjectNode(this, it) }
                    .toMutableList()

            treeViewAdapter = TreeViewAdapter(this)
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = projectNodes.map { it.initialize(treeNodeCollection) }

            return treeViewAdapter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(activity!!.layoutInflater.inflate(R.layout.row_project, parent, false)!!)

        override fun hasActionMode() = selectionCallback.hasActionMode()

        override fun incrementSelected() {
            selectionCallback.incrementSelected()
        }

        override fun decrementSelected() {
            selectionCallback.decrementSelected()
        }

        private fun remove(projectNode: ProjectNode) {
            Assert.assertTrue(projectNodes.contains(projectNode))

            projectNodes.remove(projectNode)

            treeNodeCollection.remove(projectNode.treeNode)
        }

        inner class ProjectNode(private val projectListAdapter: ProjectListAdapter, val projectData: ProjectListLoader.ProjectData) : ModelNode {

            lateinit var treeNode: TreeNode
                private set

            fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
                treeNode = TreeNode(this, treeNodeCollection, false, selectedProjectIds.contains(projectData.id))
                treeNode.setChildTreeNodes(ArrayList())
                return treeNode
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
                (viewHolder as Holder).run {
                    projectName.text = projectData.name

                    projectUsers.text = projectData.users

                    itemView.setOnClickListener(treeNode.onClickListener)

                    itemView.setOnLongClickListener(treeNode.onLongClickListener)

                    if (treeNode.isSelected)
                        itemView.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.selected))
                    else
                        itemView.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            override val itemViewType = 0

            override val isSelectable = true

            override fun onClick() {
                activity!!.startActivity(ShowProjectActivity.newIntent(activity!!, projectData.id))
            }

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override fun compareTo(other: ModelNode): Int {
                Assert.assertTrue(other is ProjectNode)

                val projectNode = other as ProjectNode

                return projectData.id.compareTo(projectNode.projectData.id)
            }

            fun remove() {
                projectListAdapter.remove(this)
            }
        }
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view) {

        val projectName = itemView.projectName!!
        val projectUsers = itemView.projectUsers!!
    }
}
