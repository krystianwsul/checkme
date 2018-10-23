package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.plusAssign

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

    private lateinit var groupListFragment: GroupListFragment

    private var selectAllVisible = false

    private lateinit var showNotificationGroupViewModel: ShowNotificationGroupViewModel

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

        check(intent.hasExtra(INSTANCES_KEY))

        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        check(!instanceKeys.isEmpty())

        this.instanceKeys = HashSet(instanceKeys)

        showNotificationGroupViewModel = getViewModel<ShowNotificationGroupViewModel>().apply {
            start(this@ShowNotificationGroupActivity.instanceKeys)

            createDisposable += data.subscribe { groupListFragment.setInstanceKeys(this@ShowNotificationGroupActivity.instanceKeys, it.dataId, it.dataWrapper) }
        }
    }

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