package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.ShowNotificationGroupLoader
import com.krystianwsul.checkme.utils.InstanceKey
import junit.framework.Assert
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class ShowNotificationGroupActivity : AbstractActivity(), GroupListFragment.GroupListListener, LoaderManager.LoaderCallbacks<ShowNotificationGroupLoader.Data> {

    companion object {

        private val INSTANCES_KEY = "instanceKeys"

        fun getIntent(context: Context, instanceKeys: ArrayList<InstanceKey>) = Intent(context, ShowNotificationGroupActivity::class.java).apply {
            Assert.assertTrue(!instanceKeys.isEmpty())

            putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys)
        }
    }

    private lateinit var instanceKeys: Set<InstanceKey>

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

        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY))

        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        Assert.assertTrue(!instanceKeys.isEmpty())

        this.instanceKeys = HashSet(instanceKeys)

        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowNotificationGroupLoader(this, instanceKeys)

    override fun onLoadFinished(loader: Loader<ShowNotificationGroupLoader.Data>, data: ShowNotificationGroupLoader.Data) {
        groupListFragment.setInstanceKeys(instanceKeys, data.dataId, data.dataWrapper)
    }

    override fun onLoaderReset(loader: Loader<ShowNotificationGroupLoader.Data>) = Unit

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
        Assert.assertTrue(item.itemId == R.id.action_select_all)

        groupListFragment.selectAll()

        return true
    }
}