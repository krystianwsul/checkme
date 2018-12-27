package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_task.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowTaskActivity : AbstractActivity(), TaskListFragment.TaskListListener {

    companion object {

        const val TASK_KEY_KEY = "taskKey"

        const val REQUEST_EDIT_TASK = 1

        fun newIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private lateinit var taskKey: TaskKey

    private var data: ShowTaskViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTaskViewModel: ShowTaskViewModel

    override val search = Observable.never<String>()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_task)

        setSupportActionBar(toolbar)

        supportActionBar!!.title = null

        taskKey = if (savedInstanceState != null) {
            check(savedInstanceState.containsKey(TASK_KEY_KEY))

            savedInstanceState.getParcelable(TASK_KEY_KEY)!!
        } else {
            check(intent.hasExtra(TASK_KEY_KEY))

            intent.getParcelableExtra(TASK_KEY_KEY)!!
        }

        taskListFragment = (supportFragmentManager.findFragmentById(R.id.show_task_fragment) as? TaskListFragment) ?: TaskListFragment.newInstance().also {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.show_task_fragment, it)
                    .commit()
        }.also { it.setFab(showTaskFab) }

        showTaskViewModel = getViewModel<ShowTaskViewModel>().apply {
            start(taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == REQUEST_EDIT_TASK)

        if (resultCode == Activity.RESULT_OK) {
            check(data!!.hasExtra(TASK_KEY_KEY))

            taskKey = data.getParcelableExtra(TASK_KEY_KEY)!!

            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable)
            })
        }

        showTaskViewModel.start(taskKey)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_task_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.run {
            findItem(R.id.task_menu_edit).isVisible = data != null
            findItem(R.id.task_menu_share).isVisible = data != null
            findItem(R.id.task_menu_delete).isVisible = data != null
            findItem(R.id.task_menu_select_all).isVisible = selectAllVisible
            findItem(R.id.task_menu_show_instances).isVisible = data?.hasInstances == true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.task_menu_edit -> {
                showTaskViewModel.stop()

                startActivityForResult(CreateTaskActivity.getEditIntent(taskKey), REQUEST_EDIT_TASK)
            }
            R.id.task_menu_share -> {
                check(data != null)

                Utils.share(this, data!!.name + taskListFragment.shareData.let { "\n" + it })
            }
            R.id.task_menu_delete -> {
                showTaskViewModel.stop()

                DomainFactory.getInstance().setTaskEndTimeStamp(data!!.dataId, SaveService.Source.GUI, taskKey)

                finish()

                // todo snackbar
            }
            R.id.task_menu_select_all -> {
                taskListFragment.treeViewAdapter.updateDisplayedNodes {
                    taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                }
            }
            R.id.task_menu_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKey))
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    private fun onLoadFinished(data: ShowTaskViewModel.Data) {
        this.data = data

        supportActionBar!!.run {
            title = data.name
            subtitle = data.scheduleText
        }

        invalidateOptionsMenu()

        taskListFragment.setTaskKey(taskKey, data.dataId, data.taskData)
    }

    override fun onCreateActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(TASK_KEY_KEY, taskKey)
    }
}
