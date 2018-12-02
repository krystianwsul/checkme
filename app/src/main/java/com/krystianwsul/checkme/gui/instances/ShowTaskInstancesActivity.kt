package com.krystianwsul.checkme.gui.instances

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_notification_group.*

import kotlinx.android.synthetic.main.toolbar.*

class ShowTaskInstancesActivity : AbstractActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val TASK_KEY = "taskKey"

        fun getIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(TASK_KEY, taskKey as Parcelable)
        }
    }

    private lateinit var taskKey: TaskKey

    private var selectAllVisible = false

    private lateinit var showTaskInstancesViewModel: ShowTaskInstancesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_notification_group)

        setSupportActionBar(toolbar)

        supportActionBar!!.title = null

        check(intent.hasExtra(TASK_KEY))

        taskKey = intent.getParcelableExtra(TASK_KEY)!!

        showTaskInstancesViewModel = getViewModel<ShowTaskInstancesViewModel>().apply {
            start(taskKey)

            createDisposable += data.subscribe { groupListFragment.setTaskKey(taskKey, it.dataId, it.dataWrapper) }
        }
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override val bottomActionModeId = R.id.showNotificationGroupBottomActionBar

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

        groupListFragment.treeViewAdapter.updateDisplayedNodes {
            groupListFragment.selectAll(TreeViewAdapter.Placeholder)
        }

        return true
    }
}