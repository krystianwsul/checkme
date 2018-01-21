package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBar
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.ShowGroupLoader
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp
import junit.framework.Assert
import kotlinx.android.synthetic.main.toolbar.*

class ShowGroupActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowGroupLoader.Data>, GroupListFragment.GroupListListener {

    companion object {

        private val TIME_KEY = "time"

        fun getIntent(exactTimeStamp: ExactTimeStamp, context: Context) = Intent(context, ShowGroupActivity::class.java).apply {
            putExtra(TIME_KEY, exactTimeStamp.long)
        }
    }

    private lateinit var timeStamp: TimeStamp

    private lateinit var actionBar: ActionBar

    private lateinit var groupListFragment: GroupListFragment

    private var selectAllVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_group)

        setSupportActionBar(toolbar)

        actionBar = supportActionBar!!

        actionBar.title = null

        val showGroupFab = findViewById<FloatingActionButton>(R.id.show_group_fab)
        Assert.assertTrue(showGroupFab != null)

        Assert.assertTrue(intent.hasExtra(TIME_KEY))

        val time = intent.getLongExtra(TIME_KEY, -1)
        Assert.assertTrue(time != -1L)

        timeStamp = TimeStamp(time)

        groupListFragment = (supportFragmentManager.findFragmentById(R.id.show_group_list) as? GroupListFragment) ?: GroupListFragment.newInstance().also {
            supportFragmentManager.beginTransaction()
                    .add(R.id.show_group_list, it)
                    .commit()
        }

        groupListFragment.setFab(showGroupFab!!)

        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowGroupLoader(this, timeStamp)

    override fun onLoadFinished(loader: Loader<ShowGroupLoader.Data>, data: ShowGroupLoader.Data) {
        actionBar.title = data.displayText

        if (data.dataWrapper == null) {
            finish()

            return
        }

        groupListFragment.setTimeStamp(timeStamp, data.dataId, data.dataWrapper)
    }

    override fun onLoaderReset(loader: Loader<ShowGroupLoader.Data>) = Unit

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