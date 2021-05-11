package com.krystianwsul.checkme.gui.projects


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentProjectListBinding
import com.krystianwsul.checkme.domainmodel.extensions.clearProjectEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setProjectEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import java.io.Serializable
import java.util.*

class ProjectListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_PROJECT_IDS = "selectedProjectIds"

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newInstance() = ProjectListFragment()
    }

    private var projectListFab: FloatingActionButton? = null

    lateinit var treeViewAdapter: TreeViewAdapter<AbstractHolder>
        private set

    private var data: ProjectListViewModel.Data? = null

    private val mainActivity get() = activity as MainActivity

    private val listener get() = activity as ProjectListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override val bottomBarData by lazy {
            Triple(listener.getBottomBar(), R.menu.menu_projects, listener::initBottomBar)
        }

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            checkNotNull(data)

            val projectIds = treeViewAdapter.selectedNodes
                    .map { (it.modelNode as ProjectListAdapter.ProjectNode).projectData.id }
                    .toHashSet()

            check(projectIds.isNotEmpty())

            val projectKey by lazy { projectIds.single() }

            when (itemId) {
                R.id.projectsMenuShowInstances -> startActivity(ShowTaskInstancesActivity.getIntent(
                        ShowTaskInstancesActivity.Parameters.Project(projectKey),
                ))
                R.id.projectsMenuShowTasks ->
                    startActivity(ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Project(projectKey)))
                R.id.projectsMenuEdit ->
                    startActivity(ShowProjectActivity.newIntent(requireContext(), projectKey))
                R.id.projectsMenuDelete -> RemoveInstancesDialogFragment.newInstance(projectIds)
                        .also { it.listener = deleteInstancesListener }
                        .show(childFragmentManager, TAG_REMOVE_INSTANCES)
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            mainActivity.onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder, initial)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            mainActivity.onDestroyActionMode()
        }

        override fun getItemVisibilities(): List<Pair<Int, Boolean>> {
            val selectedNodes = getTreeViewAdapter().selectedNodes
            check(selectedNodes.isNotEmpty())

            val single = selectedNodes.size == 1

            return listOf(
                    R.id.projectsMenuShowInstances,
                    R.id.projectsMenuShowTasks,
                    R.id.projectsMenuEdit,
            ).map { it to single }
        }
    }

    private var selectedProjectIds: Set<ProjectKey.Shared> = setOf()

    private val isVisible = BehaviorRelay.createDefault(false)
    private lateinit var projectListViewModel: ProjectListViewModel

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { projectIds, removeInstances ->
        checkNotNull(data)

        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setProjectEndTimeStamps(
                projectListViewModel.dataId.toFirst(),
                projectIds as Set<ProjectKey.Shared>,
                removeInstances,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapMaybe { mainActivity.showSnackbarRemovedMaybe(projectIds.size).map { _ -> it } }
                .flatMapCompletable {
                    AndroidDomainUpdater.clearProjectEndTimeStamps(projectListViewModel.dataId.toFirst(), it)
                }
                .subscribe()
                .addTo(createDisposable)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentProjectListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.projectListRecycler.layoutManager = LinearLayoutManager(activity)

        projectListViewModel = getViewModel<ProjectListViewModel>().apply {
            viewCreatedDisposable += isVisible.subscribe { if (it) start() else stop() }

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
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, false)
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
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, true)
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

        isVisible.accept(true)
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
        isVisible.accept(false)

        projectListFab = null
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class ProjectListAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
        )

        private lateinit var projectNodes: MutableList<ProjectNode>

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
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
                AbstractModelNode(),
                MultiLineModelNode {

            override val holderType = HolderType.MULTILINE

            override val id = projectData.id

            public override lateinit var treeNode: TreeNode<AbstractHolder>
                private set

            fun initialize(treeNodeCollection: TreeNodeCollection<AbstractHolder>): TreeNode<AbstractHolder> {
                treeNode = TreeNode(this, treeNodeCollection, selectedProjectIds.contains(projectData.id))
                treeNode.setChildTreeNodes(ArrayList())
                return treeNode
            }

            override val name get() = MultiLineRow.Visible(projectData.name)

            override val details get() = MultiLineRow.Visible(projectData.users, R.color.textSecondary)

            override val isSelectable = true

            override val parentNode: ModelNode<AbstractHolder>? = null

            override val delegates by lazy { listOf(MultiLineDelegate(this)) }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                        0,
                        false,
                        false,
                        false
                )

            override fun onClick(holder: AbstractHolder) =
                    startActivity(ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Project(projectData.id)))

            override fun compareTo(other: ModelNode<AbstractHolder>): Int {
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
