package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowNotificationGroupBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListParameters
import com.krystianwsul.checkme.gui.instances.list.GroupListViewModel
import com.krystianwsul.checkme.gui.projects.ShowProjectActivity
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.CopyAllRemindersDelegate
import com.krystianwsul.checkme.gui.utils.connectInstanceSearch
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class ShowTaskInstancesActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_PAGE = "page"
        private const val KEY_BOTTOM_FAB_MENU_DELEGATE_STATE = "bottomFabMenuDelegateState"

        fun newIntent(parameters: Parameters) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, parameters)
        }
    }

    private lateinit var parameters: Parameters

    private var selectAllVisible = false

    private lateinit var showTaskInstancesViewModel: ShowTaskInstancesViewModel

    override val snackbarParent get() = binding.showNotificationGroupCoordinator

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setTaskEndTimeStamps(
            showTaskInstancesViewModel.dataId.toFirst(),
            taskKeys as Set<TaskKey>,
            removeInstances,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
            .flatMapCompletable {
                AndroidDomainUpdater.clearTaskEndTimeStamps(showTaskInstancesViewModel.dataId.toFirst(), it)
            }
            .subscribe()
            .addTo(createDisposable)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTaskInstancesViewModel.refresh()
    }

    private var page = 0

    private lateinit var binding: ActivityShowNotificationGroupBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

    private var data: ShowTaskInstancesViewModel.Data? = null

    override val groupListViewModel by lazy { getViewModel<GroupListViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowNotificationGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.showNotificationGroupCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE),
        )

        binding.groupListFragment.listener = this

        parameters = intent.getParcelableExtra(KEY_PARAMETERS)!!

        savedInstanceState?.apply { page = getInt(KEY_PAGE) }

        binding.groupListFragment.setVisible(bottomFabMenuDelegate.fabDelegate)

        binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .apply {
                setSearchMenuOptions(false, parameters is Parameters.Project, false)

                configureMenu(
                    R.menu.show_group_menu_top,
                    R.id.actionShowGroupSearch,
                    showAssignedToOthersId = R.id.actionShowGroupAssigned,
                )
            }

        showTaskInstancesViewModel = getViewModel<ShowTaskInstancesViewModel>().apply {
            val instanceSearch = binding.showNotificationGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .searchParamsObservable
                .map { it.toSearchCriteria(true, setOf()) }

            connectInstanceSearch(
                instanceSearch,
                { page },
                { page = it },
                binding.groupListFragment.progressShown,
                createDisposable,
                this,
                {
                    this@ShowTaskInstancesActivity.data = it

                    val immediate = it.immediate

                    binding.showNotificationGroupToolbarCollapseInclude
                        .collapseAppBarLayout
                        .setText(it.title, null, binding.groupListFragment.emptyTextLayout, immediate)

                    binding.groupListFragment.setParameters(
                        GroupListParameters.Parent(
                            dataId,
                            immediate,
                            it.groupListDataWrapper,
                            it.showLoader,
                            parameters.projectKey,
                            parameters.doneBeforeNotDone,
                        )
                    )

                    updateTopMenu()
                },
                { searchCriteria, page -> start(parameters, page, searchCriteria) },
            )
        }

        updateTopMenu()
        initBottomBar()

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(receiver)

        CopyAllRemindersDelegate(this, groupListViewModel, createDisposable)
    }

    override fun onStart() {
        super.onStart()

        binding.groupListFragment.checkCreatedTaskKey()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_PAGE, page)

        outState.putParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onCreateGroupActionMode(
        actionMode: ActionMode,
        treeViewAdapter: TreeViewAdapter<AbstractHolder>,
        initial: Boolean,
    ) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            replaceMenu(R.menu.menu_project_bottom)

            setOnMenuItemClickListener { item ->
                val projectKey by lazy { (parameters as Parameters.Project).projectKey }

                when (item.itemId) {
                    R.id.projectMenuShowTasks -> startActivity(
                        ShowTasksActivity.newIntent(
                            ShowTasksActivity.Parameters.Project(projectKey)
                        )
                    )
                    R.id.projectMenuEdit ->
                        startActivity(ShowProjectActivity.newIntent(this@ShowTaskInstancesActivity, projectKey))
                    R.id.projectMenuSelectAll -> binding.groupListFragment
                        .treeViewAdapter
                        .selectAll()
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
    }

    override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
            .also { it.listener = deleteInstancesListener }
            .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) = bottomFabMenuDelegate.showMenu(menuDelegate)

    private fun updateTopMenu() {
        val showProjectOptions = parameters is Parameters.Project &&
                !data?.groupListDataWrapper
                    ?.allInstanceDatas
                    .isNullOrEmpty()

        binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .menu
            .apply {
                findItem(R.id.actionShowGroupSearch).isVisible = showProjectOptions
                findItem(R.id.actionShowGroupAssigned).isVisible = showProjectOptions
            }
    }

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
            .menu
            .apply {
                val showProjectOptions = parameters.projectKey != null

                findItem(R.id.projectMenuShowTasks)?.isVisible = showProjectOptions
                findItem(R.id.projectMenuEdit)?.isVisible = showProjectOptions
                findItem(R.id.projectMenuSelectAll)?.isVisible = selectAllVisible
            }
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showNotificationGroupToolbarCollapseInclude
        .collapseAppBarLayout
        .setExpanded(expanded)

    sealed class Parameters : Parcelable {

        abstract val projectKey: ProjectKey.Shared?

        abstract val groupingMode: GroupType.GroupingMode

        abstract val doneBeforeNotDone: Boolean

        @Parcelize
        data class Task(val taskKey: TaskKey) : Parameters() {

            override val projectKey: ProjectKey.Shared? get() = null

            override val groupingMode get() = GroupType.GroupingMode.None

            override val doneBeforeNotDone get() = false
        }

        @Parcelize
        data class Project(override val projectKey: ProjectKey.Shared) : Parameters() {

            override val groupingMode get() = GroupType.GroupingMode.Time(projectKey)

            override val doneBeforeNotDone get() = true
        }
    }
}