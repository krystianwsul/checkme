package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_task.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.appBarLayout
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.android.synthetic.main.toolbar_collapse.*
import java.io.Serializable

class ShowTaskActivity : ToolbarActivity(), TaskListFragment.TaskListListener {

    companion object {

        const val TASK_KEY_KEY = "taskKey"

        const val REQUEST_EDIT_TASK = 1
        const val RESULT_DELETE = 65

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        fun newIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private lateinit var taskKey: TaskKey

    private var data: ShowTaskViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTaskViewModel: ShowTaskViewModel

    override val search = Observable.just(NullableWrapper<TaskListFragment.SearchData>())

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        showTaskViewModel.stop()

        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        setResult(RESULT_DELETE)

        finish()

        setSnackbar(taskUndoData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_task)

        initBottomBar()

        taskKey = (savedInstanceState ?: intent.extras!!).getParcelable(TASK_KEY_KEY)!!

        taskListFragment = ((supportFragmentManager.findFragmentById(R.id.showTaskFragment) as? TaskListFragment)
                ?: TaskListFragment.newInstance().also {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.showTaskFragment, it)
                    .commit()
                }).also { it.setFab(bottomFab) }

        showTaskViewModel = getViewModel<ShowTaskViewModel>().apply {
            start(taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        (supportFragmentManager.findFragmentByTag(TAG_REMOVE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    override fun onStart() {
        super.onStart()

        taskListFragment.checkCreatedTaskKey()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EDIT_TASK) {
            if (resultCode == Activity.RESULT_OK) {
                check(data!!.hasExtra(TASK_KEY_KEY))

                taskKey = data.getParcelableExtra(TASK_KEY_KEY)!!

                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(TASK_KEY_KEY, taskKey as Parcelable)
                })
            }

            showTaskViewModel.start(taskKey)
        }
    }

    private fun onLoadFinished(data: ShowTaskViewModel.Data) {
        this.data = data

        toolbar.run {
            title = data.name
            subtitle = data.subtitle
        }

        toolbarCollapseText.apply {
            visibility = if (data.collapseText.isNullOrEmpty()) View.GONE else View.VISIBLE
            text = data.collapseText
        }

        updateBottomMenu()

        taskListFragment.setTaskKey(
                TaskListFragment.RootTaskData(taskKey, data.imageData),
                TaskListFragment.Data(data.dataId, data.immediate, data.taskData))
    }

    override fun onCreateActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override val snackbarParent get() = showTaskCoordinator!!

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(TASK_KEY_KEY, taskKey)
    }

    private fun updateBottomMenu() {
        bottomAppBar.menu.run {
            if (findItem(R.id.task_menu_edit) == null)
                return

            findItem(R.id.task_menu_edit).isVisible = data?.current == true
            findItem(R.id.task_menu_share).isVisible = data != null
            findItem(R.id.task_menu_delete).isVisible = data?.current == true
            findItem(R.id.task_menu_select_all).isVisible = selectAllVisible
            findItem(R.id.task_menu_show_instances).isVisible = data?.hasInstances == true
        }
    }

    override fun getBottomBar() = bottomAppBar!!

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.show_task_menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.task_menu_edit -> {
                        showTaskViewModel.stop()

                        startActivityForResult(CreateTaskActivity.getEditIntent(taskKey), REQUEST_EDIT_TASK)
                    }
                    R.id.task_menu_share -> {
                        check(data != null)

                        Utils.share(this@ShowTaskActivity, data!!.name + taskListFragment.shareData.let { "\n" + it })
                    }
                    R.id.task_menu_delete -> {
                        RemoveInstancesDialogFragment.newInstance(setOf(taskKey))
                                .also { it.listener = deleteInstancesListener }
                                .show(supportFragmentManager, TAG_REMOVE_INSTANCES)
                    }
                    R.id.task_menu_select_all -> {
                        taskListFragment.treeViewAdapter.updateDisplayedNodes {
                            taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                        }
                    }
                    R.id.task_menu_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKey))
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)
}
