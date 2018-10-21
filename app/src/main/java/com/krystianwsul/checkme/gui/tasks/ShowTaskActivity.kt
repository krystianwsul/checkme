package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.loaders.ShowTaskLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils

import kotlinx.android.synthetic.main.activity_show_task.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowTaskActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowTaskLoader.Data>, TaskListFragment.TaskListListener {

    companion object {

        const val TASK_KEY_KEY = "taskKey"

        const val REQUEST_EDIT_TASK = 1

        fun newIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private lateinit var taskKey: TaskKey

    private var data: ShowTaskLoader.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

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

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this)
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

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this) // does this work?
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
                @Suppress("DEPRECATION")
                supportLoaderManager.destroyLoader(0)

                startActivityForResult(CreateTaskActivity.getEditIntent(taskKey), REQUEST_EDIT_TASK)
            }
            R.id.task_menu_share -> {
                check(data != null)

                Utils.share(data!!.name + taskListFragment.shareData.let { "\n" + it })
            }
            R.id.task_menu_delete -> {
                @Suppress("DEPRECATION")
                supportLoaderManager.destroyLoader(0)

                DomainFactory.getDomainFactory().setTaskEndTimeStamp(this, data!!.dataId, SaveService.Source.GUI, taskKey)

                finish()
            }
            R.id.task_menu_select_all -> taskListFragment.selectAll()
            R.id.task_menu_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKey))
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowTaskLoader(this, taskKey)

    override fun onLoadFinished(loader: Loader<ShowTaskLoader.Data>, data: ShowTaskLoader.Data) {
        this.data = data

        supportActionBar!!.run {
            title = data.name
            subtitle = data.scheduleText
        }

        invalidateOptionsMenu()

        taskListFragment.setTaskKey(taskKey, data.dataId, data.taskData)
    }

    override fun onLoaderReset(loader: Loader<ShowTaskLoader.Data>) = Unit

    override fun onCreateTaskActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyTaskActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(TASK_KEY_KEY, taskKey)
    }
}
