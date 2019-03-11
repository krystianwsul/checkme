package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_group.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowGroupActivity : ToolbarActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val TIME_KEY = "time"

        fun getIntent(exactTimeStamp: ExactTimeStamp, context: Context) = Intent(context, ShowGroupActivity::class.java).apply {
            putExtra(TIME_KEY, exactTimeStamp.long)
        }
    }

    private lateinit var timeStamp: TimeStamp

    private var selectAllVisible = false

    private lateinit var showGroupViewModel: ShowGroupViewModel

    override val snackbarParent get() = showGroupCoordinator!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_group)

        check(intent.hasExtra(TIME_KEY))

        val time = intent.getLongExtra(TIME_KEY, -1)
        check(time != -1L)

        timeStamp = TimeStamp.fromMillis(time)

        groupListFragment.setFab(bottomFab)

        showGroupViewModel = getViewModel<ShowGroupViewModel>().apply {
            start(timeStamp)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        initBottomBar()
    }

    private fun onLoadFinished(data: ShowGroupViewModel.Data) {
        toolbar.title = data.displayText

        if (data.dataWrapper == null) {
            finish()

            return
        }

        groupListFragment.setTimeStamp(timeStamp, data.dataId, data.immediate, data.dataWrapper)
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean, addHourVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                check(item.itemId == R.id.action_select_all)

                groupListFragment.treeViewAdapter.updateDisplayedNodes {
                    groupListFragment.selectAll(TreeViewAdapter.Placeholder)
                }

                true
            }
        }
    }

    override fun getBottomBar() = bottomAppBar!!

    private fun updateBottomMenu() {
        bottomAppBar.menu.findItem(R.id.action_select_all)?.isVisible = selectAllVisible
    }
}