package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_notification_group.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class ShowNotificationGroupActivity : AbstractActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val INSTANCES_KEY = "instanceKeys"

        fun getIntent(context: Context, instanceKeys: ArrayList<InstanceKey>) = Intent(context, ShowNotificationGroupActivity::class.java).apply {
            check(!instanceKeys.isEmpty())

            putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys)
        }
    }

    private lateinit var instanceKeys: Set<InstanceKey>

    private var selectAllVisible = false
    private var addHourVisible = false

    private lateinit var showNotificationGroupViewModel: ShowNotificationGroupViewModel

    override val snackbarParent get() = showNotificationGroupCoordinator!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_notification_group)

        setSupportActionBar(toolbar)

        supportActionBar!!.title = null

        check(intent.hasExtra(INSTANCES_KEY))

        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        check(!instanceKeys.isEmpty())

        this.instanceKeys = HashSet(instanceKeys)

        showNotificationGroupViewModel = getViewModel<ShowNotificationGroupViewModel>().apply {
            start(this@ShowNotificationGroupActivity.instanceKeys)

            createDisposable += data.subscribe { groupListFragment.setInstanceKeys(it.dataId, it.dataWrapper) }
        }
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean, addHourVisible: Boolean) {
        val invalidate = (this.selectAllVisible != selectAllVisible) || (this.addHourVisible != addHourVisible)

        this.selectAllVisible = selectAllVisible
        this.addHourVisible = addHourVisible

        if (invalidate)
            invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_show_notification_group, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.apply {
            findItem(R.id.action_notification_group_select_all).isVisible = selectAllVisible
            findItem(R.id.action_notification_group_hour).isVisible = addHourVisible
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_notification_group_hour -> {
                groupListFragment.treeViewAdapter.updateDisplayedNodes {
                    groupListFragment.addHour(TreeViewAdapter.Placeholder)
                }
            }
            R.id.action_notification_group_select_all -> {
                groupListFragment.treeViewAdapter.updateDisplayedNodes {
                    groupListFragment.selectAll(TreeViewAdapter.Placeholder)
                }
            }
            else -> throw IllegalArgumentException()
        }

        return true
    }
}