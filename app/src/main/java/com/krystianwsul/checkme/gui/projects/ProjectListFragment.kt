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
import com.krystianwsul.checkme.databinding.FragmentProjectListBinding
import com.krystianwsul.checkme.databinding.RowListBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearProjectEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setProjectEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxkotlin.plusAssign
import java.io.Serializable
import java.util.*

class ProjectListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_PROJECT_IDS = "selectedProjectIds"

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newInstance() = ProjectListFragment()
    }

    private var projectListFab: FloatingActionButton? = null

    lateinit var treeViewAdapter: TreeViewAdapter<BaseHolder>
        private set

    private var data: ProjectListViewModel.Data? = null

    private val mainActivity get() = activity as MainActivity

    private val listener get() = activity as ProjectListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_projects, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
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

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            mainActivity.onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
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

    private val bindingProperty = ResettableProperty<FragmentProjectListBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(SELECTED_PROJECT_IDS) == true) {
            val selectedProjectIds = savedInstanceState.getParcelableArrayList<ProjectKey.Shared>(SELECTED_PROJECT_IDS)!!

            this.selectedProjectIds = HashSet(selectedProjectIds)
        }

        (childFragmentManager.findFragmentByTag(TAG_REMOVE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentProjectListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.projectListRecycler.layoutManager = LinearLayoutManager(activity)

        projectListViewModel = getViewModel<ProjectListViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ProjectListViewModel.Data) {
        this.data = data

        val hide = mutableListOf<View>(binding.projectListProgress)
        val show: View

        if (data.projectDatas.isEmpty()) {
            hide += binding.projectListRecycler
            show = binding.projectListEmptyTextInclude.emptyTextLayout

            binding.projectListEmptyTextInclude
                    .emptyText
                    .setText(R.string.projects_empty)
        } else {
            show = binding.projectListRecycler
            hide += binding.projectListEmptyTextInclude.emptyTextLayout
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

            binding.projectListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

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

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class ProjectListAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        val treeViewAdapter = TreeViewAdapter(
                this,
                Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
                viewCreatedDisposable
        )

        private lateinit var projectNodes: MutableList<ProjectNode>

        override lateinit var treeNodeCollection: TreeNodeCollection<BaseHolder>
            private set

        fun initialize(projectDatas: SortedMap<ProjectKey.Shared, ProjectListViewModel.ProjectData>) {
            projectNodes = projectDatas.values
                    .map(::ProjectNode)
                    .toMutableList()

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = projectNodes.map { it.initialize(treeNodeCollection) }
        }

        inner class ProjectNode(val projectData: ProjectListViewModel.ProjectData) :
                GroupHolderNode(0),
                MultiLineModelNode<BaseHolder> {

            override val nodeType = NodeType.PROJECT

            override val ripple = true

            override val id = projectData.id

            public override lateinit var treeNode: TreeNode<BaseHolder>
                private set

            fun initialize(treeNodeCollection: TreeNodeCollection<BaseHolder>): TreeNode<BaseHolder> {
                treeNode = TreeNode(this, treeNodeCollection, false, selectedProjectIds.contains(projectData.id))
                treeNode.setChildTreeNodes(ArrayList())
                return treeNode
            }

            override val name get() = MultiLineNameData.Visible(projectData.name)

            override val details get() = Pair(projectData.users, colorSecondary)

            override val isSelectable = true

            override val parentNode: ModelNode<BaseHolder>? = null

            override val delegates by lazy { listOf(MultiLineDelegate(this)) }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                        indentation,
                        checkBoxState.visibility == View.GONE,
                        hasAvatar,
                        thumbnail != null
                )

            override fun onClick(holder: BaseHolder) = startActivity(ShowProjectActivity.newIntent(activity!!, projectData.id))

            override fun compareTo(other: ModelNode<BaseHolder>): Int {
                check(other is ProjectNode)

                return projectData.id.compareTo(other.projectData.id)
            }

            override fun matches(filterCriteria: Any?) = false

            override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true
        }
    }

    class Holder(rowListBinding: RowListBinding) : RegularNodeHolder(rowListBinding)

    interface ProjectListListener : SnackbarListener, ActionModeListener {

        fun setProjectSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
