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
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.utils.getOrInitializeFragment
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_show_tasks.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowTasksActivity : ToolbarActivity(), TaskListFragment.TaskListListener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"
        private const val KEY_COPIED_TASK_KEY = "copiedTaskKey"

        private const val REQUEST_COPY = 347

        fun newIntent(parameters: Parameters) = Intent(MyApplication.instance, ShowTasksActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, parameters)
        }
    }

    private var data: ShowTasksViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTasksViewModel: ShowTasksViewModel

    override val search = Observable.just(NullableWrapper<TaskListFragment.SearchData>())

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTasksViewModel.refresh()
    }

    private lateinit var parameters: Parameters
    private var copiedTaskKey: TaskKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_tasks)

        parameters = (savedInstanceState ?: intent.extras!!).getParcelable(KEY_PARAMETERS)!!
        copiedTaskKey = savedInstanceState?.getParcelable(KEY_COPIED_TASK_KEY)

        toolbar.inflateMenu(R.menu.empty_menu)

        initBottomBar()

        taskListFragment = getOrInitializeFragment(R.id.showTasksFragment) {
            TaskListFragment.newInstance()
        }.also { it.setFab(bottomFab) }

        showTasksViewModel = getViewModel<ShowTasksViewModel>().apply {
            start(parameters.taskKeys)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

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

    private fun onLoadFinished(data: ShowTasksViewModel.Data) {
        this.data = data

        updateBottomMenu()

        taskListFragment.setAllTasks(TaskListFragment.Data(
                data.dataId,
                data.immediate,
                data.taskData,
                true
        ))
    }

    override fun onCreateActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override val snackbarParent get() = showTasksCoordinator!!

    private fun updateBottomMenu() {
        bottomAppBar.menu.run {
            if (findItem(R.id.action_select_all) == null)
                return

            findItem(R.id.action_select_all).isVisible = selectAllVisible
        }
    }

    override fun getBottomBar() = bottomAppBar!!

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_select_all -> {
                        taskListFragment.treeViewAdapter.updateDisplayedNodes {
                            taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                        }
                    }
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }

        updateBottomMenu()
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)

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
                showTasksViewModel.start(parameters.taskKeys)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(KEY_PARAMETERS, parameters)
        copiedTaskKey?.let { outState.putParcelable(KEY_COPIED_TASK_KEY, it) }
    }

    sealed class Parameters : Parcelable {

        open val taskKeys: List<TaskKey>? = null

        @Parcelize
        object Unscheduled : Parameters()

        @Parcelize
        data class Copy(override val taskKeys: List<TaskKey>) : Parameters()
    }
}
