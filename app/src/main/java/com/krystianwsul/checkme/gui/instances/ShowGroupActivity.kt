package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.view.ActionMode
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_group.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowGroupActivity : AbstractActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val TIME_KEY = "time"

        fun getIntent(exactTimeStamp: ExactTimeStamp, context: Context) = Intent(context, ShowGroupActivity::class.java).apply {
            putExtra(TIME_KEY, exactTimeStamp.long)
        }
    }

    private lateinit var timeStamp: TimeStamp

    private lateinit var actionBar: ActionBar

    private var selectAllVisible = false

    private lateinit var showGroupViewModel: ShowGroupViewModel

    override val snackbarParent get() = showGroupCoordinator!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_group)

        setSupportActionBar(toolbar)

        actionBar = supportActionBar!!

        actionBar.title = null

        val showGroupFab = findViewById<FloatingActionButton>(R.id.show_group_fab)!!

        check(intent.hasExtra(TIME_KEY))

        val time = intent.getLongExtra(TIME_KEY, -1)
        check(time != -1L)

        timeStamp = TimeStamp.fromMillis(time)

        groupListFragment.setFab(showGroupFab)

        showGroupViewModel = getViewModel<ShowGroupViewModel>().apply {
            start(timeStamp)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ShowGroupViewModel.Data) {
        actionBar.title = data.displayText

        if (data.dataWrapper == null) {
            finish()

            return
        }

        groupListFragment.setTimeStamp(timeStamp, data.dataId, data.dataWrapper)
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean, addHourVisible: Boolean) {
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

        groupListFragment.treeViewAdapter.updateDisplayedNodes {
            groupListFragment.selectAll(TreeViewAdapter.Placeholder)
        }

        return true
    }
}