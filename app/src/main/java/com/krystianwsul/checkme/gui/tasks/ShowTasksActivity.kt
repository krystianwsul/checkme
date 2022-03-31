package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowTasksBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditParentHint
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.utils.exhaustive
import com.krystianwsul.checkme.utils.getOrInitializeFragment
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.parcelize.Parcelize

class ShowTasksActivity : AbstractActivity(), TaskListFragment.Listener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"
        private const val KEY_COPIED_TASK_KEY = "copiedTaskKey"

        private const val REQUEST_COPY = 347

        private const val TAG_CONFIRM = "confirm"

        private const val KEY_BOTTOM_FAB_MENU_DELEGATE_STATE = "bottomFabMenuDelegateState"

        fun newIntent(parameters: Parameters) = Intent(MyApplication.instance, ShowTasksActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, parameters)
        }
    }

    private var data: ShowTasksViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private val showTasksViewModel by lazy { getViewModel<ShowTasksViewModel>() }

    private val filterCriteria by lazy {
        binding.showTasksToolbarCollapseInclude
            .collapseAppBarLayout
            .filterCriteria
    }

    override val taskSearch by lazy {
        filterCriteria.map { it.toExpandOnly() }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTasksViewModel.refresh()
    }

    private lateinit var parameters: Parameters
    private var copiedTaskKey: TaskKey? = null

    private lateinit var binding: ActivityShowTasksBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowTasksBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.showTasksCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE),
        )

        parameters = (savedInstanceState ?: intent.extras)?.getParcelable(KEY_PARAMETERS) ?: return
        copiedTaskKey = savedInstanceState?.getParcelable(KEY_COPIED_TASK_KEY)

        binding.showTasksToolbarCollapseInclude
            .collapseAppBarLayout
            .configureMenu(
                R.menu.show_task_menu_top,
                R.id.actionShowTaskSearch,
                R.id.actionTaskShowDeleted,
                R.id.actionTaskShowAssignedToOthers,
                R.id.actionTaskShowProjects,
            )

        updateTopMenu()

        initBottomBar()

        taskListFragment = getOrInitializeFragment(R.id.showTasksFragment) {
            TaskListFragment.newInstance()
        }.also {
            it.setFab(bottomFabMenuDelegate.fabDelegate)
            it.listener = this
        }

        showTasksViewModel.apply {
            start(this@ShowTasksActivity.parameters)

            createDisposable += filterCriteria.map { it.search }.subscribe(searchRelay)

            createDisposable += data.subscribe(::onLoadFinished)
        }

        startDate(receiver)

        (supportFragmentManager.findFragmentByTag(TAG_CONFIRM) as? ConfirmDialogFragment)?.listener = this::onConfirm
    }

    override fun onStart() {
        super.onStart()

        taskListFragment.checkCreatedTaskKey()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    private fun onLoadFinished(data: ShowTasksViewModel.Data) {
        this.data = data

        binding.showTasksToolbarCollapseInclude
            .collapseAppBarLayout
            .setText(data.title, data.subtitle, null, true)

        updateTopMenu()
        updateBottomMenu()

        taskListFragment.parameters = parameters.mapDataToTaskListFragmentParameters(showTasksViewModel.dataId, data)
    }

    override fun onCreateActionMode(actionMode: ActionMode) = binding.showTasksToolbarCollapseInclude
        .collapseAppBarLayout
        .collapse()

    override fun onDestroyActionMode() = binding.showTasksToolbarCollapseInclude
        .collapseAppBarLayout
        .expand()

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible && !parameters.copying

        updateBottomMenu()
    }

    override val snackbarParent get() = binding.showTasksCoordinator

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
            .menu
            .findItem(R.id.action_select_all)
            ?.isVisible = selectAllVisible
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            animateReplaceMenu(R.menu.menu_select_all, onEnd = ::updateBottomMenu)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_select_all -> taskListFragment.treeViewAdapter.selectAll()
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }
    }

    override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) = bottomFabMenuDelegate.showMenu(menuDelegate)

    override fun setToolbarExpanded(expanded: Boolean) = binding.showTasksToolbarCollapseInclude
        .collapseAppBarLayout
        .setExpanded(expanded)

    override fun startCopy(taskKey: TaskKey) {
        copiedTaskKey = taskKey

        @Suppress("DEPRECATION")
        startActivityForResult(
            EditActivity.getParametersIntent(EditParameters.Copy(taskKey)),
            REQUEST_COPY,
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_COPY && resultCode == Activity.RESULT_OK) {
            val oldParameters = parameters as Parameters.Copy
            val newTaskKeys = oldParameters.taskKeys - copiedTaskKey!!

            if (newTaskKeys.isEmpty()) {
                finish()
            } else {
                parameters = oldParameters.copy(taskKeys = newTaskKeys)

                copiedTaskKey = null

                showTasksViewModel.stop()
                showTasksViewModel.start(parameters)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.showTasksToolbarCollapseInclude.collapseAppBarLayout.isSearching) {
            binding.showTasksToolbarCollapseInclude
                .collapseAppBarLayout
                .closeSearch()
        } else {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            when (parameters) {
                is Parameters.Copy -> ConfirmDialogFragment.newInstance(
                    ConfirmDialogFragment.Parameters(
                        R.string.stopCopyingMessage,
                        R.string.stopCopyingYes
                    )
                ).also {
                    it.listener = this::onConfirm
                    it.show(supportFragmentManager, TAG_CONFIRM)
                }
                else -> super.onBackPressed()
            }.exhaustive()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onConfirm(parcelable: Parcelable?) = finish()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(KEY_PARAMETERS, parameters)
        copiedTaskKey?.let { outState.putParcelable(KEY_COPIED_TASK_KEY, it) }

        outState.putParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
    }

    private fun updateTopMenu() {
        val showSearch: Boolean
        val showDeleted: Boolean
        val showAssignedToOthers: Boolean
        val showProjects: Boolean

        val data = data
        if (data == null) {
            showSearch = false
            showDeleted = false
            showAssignedToOthers = false
            showProjects = false
        } else {
            val hasTasks = data.taskData
                .entryDatas
                .isNotEmpty()

            showSearch = hasTasks
            showDeleted = hasTasks && parameters.showDeleted
            showAssignedToOthers = hasTasks && parameters.getShowAssignedToOthers(data)
            showProjects = hasTasks && parameters.showProjects
        }

        binding.showTasksToolbarCollapseInclude
            .collapseAppBarLayout
            .apply {
                setSearchMenuOptions(showDeleted, showAssignedToOthers, showProjects)

                menu.apply {
                    findItem(R.id.actionShowTaskSearch).isVisible = showSearch
                    findItem(R.id.actionTaskShowDeleted).isVisible = showDeleted
                    findItem(R.id.actionTaskShowAssignedToOthers).isVisible = showAssignedToOthers
                    findItem(R.id.actionTaskShowProjects).isVisible = showProjects
                }
            }
    }

    sealed class Parameters : Parcelable {

        open val reverseOrderForTopLevelNodes = true

        open val copying = false

        open val showDeleted = true
        open val showProjects = false

        abstract fun getShowAssignedToOthers(data: ShowTasksViewModel.Data): Boolean

        protected fun mapDataToTaskListFragmentData(
            dataId: DataId,
            data: ShowTasksViewModel.Data,
        ) = TaskListFragment.Data(
            dataId,
            data.immediate,
            data.taskData,
            reverseOrderForTopLevelNodes,
            copying,
            false
        )

        abstract fun mapDataToTaskListFragmentParameters(
            dataId: DataId,
            data: ShowTasksViewModel.Data,
        ): TaskListFragment.Parameters

        @Parcelize
        data class Unscheduled(val projectKey: ProjectKey<*>?) : Parameters() {

            override val showProjects get() = projectKey == null

            override fun getShowAssignedToOthers(data: ShowTasksViewModel.Data): Boolean {
                check(data.isSharedProject == null)

                return false
            }

            override fun mapDataToTaskListFragmentParameters(dataId: DataId, data: ShowTasksViewModel.Data) =
                TaskListFragment.Parameters.Notes(
                    mapDataToTaskListFragmentData(dataId, data),
                    true,
                    (projectKey as? ProjectKey.Shared)?.let(EditParentHint::Project),
                )
        }

        @Parcelize
        data class Copy(val taskKeys: List<TaskKey>) : Parameters() {

            override val reverseOrderForTopLevelNodes get() = false

            override val copying get() = true

            override val showDeleted get() = false

            override fun getShowAssignedToOthers(data: ShowTasksViewModel.Data): Boolean {
                check(data.isSharedProject == null)

                return false
            }

            override fun mapDataToTaskListFragmentParameters(dataId: DataId, data: ShowTasksViewModel.Data) =
                TaskListFragment.Parameters.All(mapDataToTaskListFragmentData(dataId, data), false)
        }

        @Parcelize
        data class Project(val projectKey: ProjectKey<*>) : Parameters() {

            override fun getShowAssignedToOthers(data: ShowTasksViewModel.Data) = data.isSharedProject!!

            override fun mapDataToTaskListFragmentParameters(dataId: DataId, data: ShowTasksViewModel.Data) =
                TaskListFragment.Parameters.Project(mapDataToTaskListFragmentData(dataId, data), projectKey)
        }
    }
}
