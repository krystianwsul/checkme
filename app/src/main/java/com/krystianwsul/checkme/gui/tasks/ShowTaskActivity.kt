package com.krystianwsul.checkme.gui.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowTaskBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxkotlin.plusAssign
import java.io.Serializable

class ShowTaskActivity : AbstractActivity(), TaskListFragment.Listener {

    companion object {

        const val TASK_KEY_KEY = "taskKey"

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private lateinit var taskKey: TaskKey

    private var data: ShowTaskViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTaskViewModel: ShowTaskViewModel

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        showTaskViewModel.stop()

        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(
                SaveService.Source.GUI,
                taskKeys as Set<TaskKey>,
                removeInstances
        )

        finish()

        setSnackbar(taskUndoData)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTaskViewModel.refresh()
    }

    override val taskSearch by lazy {
        binding.showTaskToolbarCollapseInclude
                .collapseAppBarLayout
                .filterCriteria
    }

    private lateinit var binding: ActivityShowTaskBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowTaskBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.showTaskToolbarCollapseInclude
                .collapseAppBarLayout
                .configureMenu(R.menu.show_task_menu_top, R.id.actionShowTaskSearch, R.id.actionTaskShowDeleted)

        updateTopMenu()

        initBottomBar()

        taskKey = (savedInstanceState ?: intent.extras!!).getParcelable(TASK_KEY_KEY)!!

        taskListFragment = getOrInitializeFragment(R.id.showTaskFragment) {
            TaskListFragment.newInstance()
        }.also { it.setFab(bottomBinding.bottomFab) }

        showTaskViewModel = getViewModel<ShowTaskViewModel>().apply {
            start(taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_REMOVE_INSTANCES)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    override fun onStart() {
        super.onStart()

        taskListFragment.checkCreatedTaskKey()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    private fun onLoadFinished(data: ShowTaskViewModel.Data) {
        this.data = data
        taskKey = data.newTaskKey

        val immediate = data.immediate

        Handler(Looper.getMainLooper()).post { // apparently included layout isn't immediately available in onCreate
            binding.showTaskToolbarCollapseInclude
                    .collapseAppBarLayout
                    .setText(data.name, data.collapseText, taskListFragment.emptyTextLayout, immediate)
        }

        updateTopMenu()
        updateBottomMenu()

        taskListFragment.setTaskKey(
                TaskListFragment.RootTaskData(taskKey, data.imageData),
                TaskListFragment.Data(
                        data.dataId,
                        immediate,
                        data.taskData,
                        false
                )
        )
    }

    private fun updateTopMenu() {
        val hasChildren = !data?.taskData
                ?.childTaskDatas
                .isNullOrEmpty()

        binding.showTaskToolbarCollapseInclude
                .collapseAppBarLayout
                .menu
                .apply {
                    findItem(R.id.actionShowTaskSearch).isVisible = hasChildren
                    findItem(R.id.actionTaskShowDeleted).isVisible = hasChildren
                }
    }

    override fun onCreateActionMode(actionMode: ActionMode) = binding.showTaskToolbarCollapseInclude
            .collapseAppBarLayout
            .collapse()

    override fun onDestroyActionMode() = binding.showTaskToolbarCollapseInclude
            .collapseAppBarLayout
            .expand()

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override val snackbarParent get() = binding.showTaskCoordinator

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(TASK_KEY_KEY, taskKey)
    }

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
                .menu
                .run {
                    if (findItem(R.id.task_menu_edit) == null) return

                    findItem(R.id.task_menu_edit).isVisible = data?.current == true
                    findItem(R.id.task_menu_share).isVisible = data != null
                    findItem(R.id.task_menu_delete).isVisible = data?.current == true
                    findItem(R.id.task_menu_select_all).isVisible = selectAllVisible
                    findItem(R.id.task_menu_show_instances).isVisible = data != null
                    findItem(R.id.taskMenuCopyTask).isVisible = data?.current == true
                    findItem(R.id.taskMenuWebSearch).isVisible = data != null
                }
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            animateReplaceMenu(R.menu.show_task_menu_bottom) { updateBottomMenu() }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.task_menu_edit -> startActivity(EditActivity.getParametersIntent(EditParameters.Edit(taskKey)))
                    R.id.task_menu_share -> {
                        check(data != null)

                        Utils.share(this@ShowTaskActivity, data!!.name + taskListFragment.shareData.let { "\n" + it })
                    }
                    R.id.task_menu_delete -> {
                        RemoveInstancesDialogFragment.newInstance(setOf(taskKey))
                                .also { it.listener = deleteInstancesListener }
                                .show(supportFragmentManager, TAG_REMOVE_INSTANCES)
                    }
                    R.id.task_menu_select_all -> taskListFragment.treeViewAdapter.selectAll()
                    R.id.task_menu_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKey))
                    R.id.taskMenuCopyTask -> startActivity(EditActivity.getParametersIntent(EditParameters.Copy(taskKey)))
                    R.id.taskMenuWebSearch -> startActivity(webSearchIntent(data!!.name))
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showTaskToolbarCollapseInclude
            .collapseAppBarLayout
            .setExpanded(expanded)

    override fun onBackPressed() {
        binding.showTaskToolbarCollapseInclude
                .collapseAppBarLayout
                .apply { if (isSearching) closeSearch() else super.onBackPressed() }
    }
}
