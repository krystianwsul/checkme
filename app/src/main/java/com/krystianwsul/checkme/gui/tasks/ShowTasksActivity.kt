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
import com.krystianwsul.checkme.utils.getOrInitializeFragment
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.parcelize.Parcelize

class ShowTasksActivity : AbstractActivity(), TaskListFragment.Listener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"
        private const val KEY_COPIED_TASK_KEY = "copiedTaskKey"

        private const val REQUEST_COPY = 347

        private const val TAG_CONFIRM = "confirm"

        fun newIntent(parameters: Parameters) = Intent(MyApplication.instance, ShowTasksActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, parameters)
        }
    }

    private var data: ShowTasksViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTasksViewModel: ShowTasksViewModel

    override val taskSearch by lazy {
        binding.showTasksToolbarCollapseInclude
                .collapseAppBarLayout
                .filterCriteria
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTasksViewModel.refresh()
    }

    private lateinit var parameters: Parameters
    private var copiedTaskKey: TaskKey? = null

    private lateinit var binding: ActivityShowTasksBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowTasksBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        parameters = (savedInstanceState ?: intent.extras!!).getParcelable(KEY_PARAMETERS)!!
        copiedTaskKey = savedInstanceState?.getParcelable(KEY_COPIED_TASK_KEY)

        binding.showTasksToolbarCollapseInclude
                .collapseAppBarLayout
                .apply {
                    if (parameters.copying) setSearchMenuOptions(false, false, true)

                    setText(getString(parameters.title), null, null, true)

                    configureMenu(
                            R.menu.show_task_menu_top,
                            R.id.actionShowTaskSearch,
                            R.id.actionTaskShowDeleted,
                            showProjectsId = R.id.actionTaskShowProjects,
                    )
                }

        updateTopMenu()

        initBottomBar()

        taskListFragment = getOrInitializeFragment(R.id.showTasksFragment) {
            TaskListFragment.newInstance()
        }.also { it.setFab(bottomBinding.bottomFab) }

        showTasksViewModel = getViewModel<ShowTasksViewModel>().apply {
            start(parameters)

            createDisposable += data.subscribe { onLoadFinished(it) }
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

        updateTopMenu()
        updateBottomMenu()

        taskListFragment.setAllTasks(TaskListFragment.Data(
                data.dataId,
                data.immediate,
                data.taskData,
                parameters.reverseOrderForTopLevelNodes,
                parameters.copying,
                false
        ))
    }

    override fun onCreateActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible && !parameters.copying

        updateBottomMenu()
    }

    override val snackbarParent get() = binding.showTasksCoordinator

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
                .menu
                .run {
                    if (findItem(R.id.action_select_all) == null) return

                    findItem(R.id.action_select_all).isVisible = selectAllVisible
                }
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            animateReplaceMenu(R.menu.menu_select_all, ::updateBottomMenu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_select_all -> taskListFragment.treeViewAdapter.selectAll()
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showTasksToolbarCollapseInclude
            .collapseAppBarLayout
            .setExpanded(expanded)

    override fun startCopy(taskKey: TaskKey) {
        copiedTaskKey = taskKey

        startActivityForResult(
                EditActivity.getParametersIntent(EditParameters.Copy(taskKey)),
                REQUEST_COPY
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
            when (parameters) {
                Parameters.Unscheduled -> super.onBackPressed()
                is Parameters.Copy -> ConfirmDialogFragment.newInstance(ConfirmDialogFragment.Parameters(
                        R.string.stopCopyingMessage,
                        R.string.stopCopyingYes
                )).also {
                    it.listener = this::onConfirm
                    it.show(supportFragmentManager, TAG_CONFIRM)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onConfirm(parcelable: Parcelable?) = finish()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(KEY_PARAMETERS, parameters)
        copiedTaskKey?.let { outState.putParcelable(KEY_COPIED_TASK_KEY, it) }
    }

    private fun updateTopMenu() {
        val hasTasks = !data?.taskData
                ?.entryDatas
                .isNullOrEmpty()

        binding.showTasksToolbarCollapseInclude
                .collapseAppBarLayout
                .menu
                .apply {
                    findItem(R.id.actionShowTaskSearch).isVisible = hasTasks
                    findItem(R.id.actionTaskShowProjects).isVisible = hasTasks
                }
    }

    sealed class Parameters : Parcelable {

        open val taskKeys: List<TaskKey>? = null

        abstract val title: Int

        abstract val reverseOrderForTopLevelNodes: Boolean

        abstract val copying: Boolean

        @Parcelize
        object Unscheduled : Parameters() {

            override val title get() = R.string.noReminder

            override val reverseOrderForTopLevelNodes get() = true

            override val copying get() = false
        }

        @Parcelize
        data class Copy(override val taskKeys: List<TaskKey>) : Parameters() {

            override val title get() = R.string.copyingTasksTitle

            override val reverseOrderForTopLevelNodes get() = false

            override val copying get() = true
        }
    }
}
