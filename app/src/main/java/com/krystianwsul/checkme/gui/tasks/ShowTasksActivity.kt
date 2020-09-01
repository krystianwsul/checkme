package com.krystianwsul.checkme.gui.tasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.utils.getOrInitializeFragment
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ShowTasksViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_tasks.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowTasksActivity : ToolbarActivity(), TaskListFragment.TaskListListener {

    companion object {

        fun newIntent() = Intent(MyApplication.instance, ShowTasksActivity::class.java)
    }

    private var data: ShowTasksViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTasksViewModel: ShowTasksViewModel

    override val search = Observable.just(NullableWrapper<TaskListFragment.SearchData>())

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTasksViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_tasks)

        toolbar.inflateMenu(R.menu.empty_menu)

        initBottomBar()

        taskListFragment = getOrInitializeFragment(R.id.showTasksFragment) {
            TaskListFragment.newInstance()
        }.also { it.setFab(bottomFab) }
        // todo don't add reminder when adding through fab

        showTasksViewModel = getViewModel<ShowTasksViewModel>().apply {
            start()

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
                false
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
}
