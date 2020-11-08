package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_group.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.toolbar_collapse.*
import java.io.Serializable

class ShowGroupActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val TIME_KEY = "time"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(exactTimeStamp: ExactTimeStamp.Local, context: Context) = Intent(context, ShowGroupActivity::class
                .java).apply {
            putExtra(TIME_KEY, exactTimeStamp.long)
        }
    }

    private lateinit var timeStamp: TimeStamp

    private var selectAllVisible = false

    private lateinit var showGroupViewModel: ShowGroupViewModel

    override val snackbarParent get() = showGroupCoordinator!!

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showGroupViewModel.refresh()
    }

    override val instanceSearch by lazy { collapseAppBarLayout.searchData }

    private var data: ShowGroupViewModel.Data? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_group)

        groupListFragment.listener = this

        check(intent.hasExtra(TIME_KEY))

        val time = intent.getLongExtra(TIME_KEY, -1)
        check(time != -1L)

        timeStamp = TimeStamp.fromMillis(time)

        groupListFragment.setFab(bottomFab)

        collapseAppBarLayout.apply {
            hideShowDeleted()

            inflateMenu(R.menu.show_task_menu_top)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.actionShowTaskSearch -> collapseAppBarLayout.startSearch()
                    else -> throw IllegalArgumentException()
                }
            }
        }

        updateTopMenu()
        initBottomBar()

        showGroupViewModel = getViewModel<ShowGroupViewModel>().apply {
            start(timeStamp)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    private fun updateTopMenu() {
        collapseAppBarLayout.menu.apply {
            findItem(R.id.actionShowTaskSearch).isVisible = !data?.groupListDataWrapper
                    ?.instanceDatas
                    .isNullOrEmpty()
        }
    }

    override fun onStart() {
        super.onStart()

        groupListFragment.checkCreatedTaskKey()
    }

    private fun onLoadFinished(data: ShowGroupViewModel.Data) {
        this.data = data

        val immediate = data.immediate

        collapseAppBarLayout.setText(data.displayText, null, emptyTextLayout, immediate)

        if (data.groupListDataWrapper == null) {
            finish()

            return
        }

        groupListFragment.setTimeStamp(timeStamp, data.dataId, immediate, data.groupListDataWrapper)

        updateTopMenu()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter<NodeHolder>) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                check(item.itemId == R.id.action_select_all)

                groupListFragment.treeViewAdapter.selectAll()

                true
            }
        }
    }

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
                .also { it.listener = deleteInstancesListener }
                .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun getBottomBar() = bottomAppBar!!

    private fun updateBottomMenu() {
        bottomAppBar.menu.findItem(R.id.action_select_all)?.isVisible = selectAllVisible
    }

    override fun setToolbarExpanded(expanded: Boolean) = collapseAppBarLayout.setExpanded(expanded)
}