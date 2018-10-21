package com.krystianwsul.checkme.gui.instances

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
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.ShowTaskInstancesLoader
import com.krystianwsul.checkme.utils.TaskKey

import kotlinx.android.synthetic.main.toolbar.*

class ShowTaskInstancesActivity : AbstractActivity(), GroupListFragment.GroupListListener, LoaderManager.LoaderCallbacks<ShowTaskInstancesLoader.Data> {

    companion object {

        private const val TASK_KEY = "taskKey"

        fun getIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(TASK_KEY, taskKey as Parcelable)
        }
    }

    private lateinit var taskKey: TaskKey

    private lateinit var groupListFragment: GroupListFragment

    private var selectAllVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_notification_group)

        setSupportActionBar(toolbar)

        supportActionBar!!.title = null

        groupListFragment = (supportFragmentManager.findFragmentById(R.id.show_notification_group_list) as? GroupListFragment) ?: GroupListFragment.newInstance().also {
            supportFragmentManager.beginTransaction()
                    .add(R.id.show_notification_group_list, it)
                    .commit()
        }

        check(intent.hasExtra(TASK_KEY))

        taskKey = intent.getParcelableExtra<TaskKey>(TASK_KEY)!!

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowTaskInstancesLoader(this, taskKey)

    override fun onLoadFinished(loader: Loader<ShowTaskInstancesLoader.Data>, data: ShowTaskInstancesLoader.Data) {
        groupListFragment.setTaskKey(taskKey, data.dataId, data.dataWrapper)
    }

    override fun onLoaderReset(loader: Loader<ShowTaskInstancesLoader.Data>) = Unit

    override fun onCreateGroupActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_all, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_select_all).isVisible = selectAllVisible
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(item.itemId == R.id.action_select_all)

        groupListFragment.selectAll()

        return true
    }
}