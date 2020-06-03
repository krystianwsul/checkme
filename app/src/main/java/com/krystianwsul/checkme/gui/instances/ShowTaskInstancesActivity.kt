package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListListener
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_show_notification_group.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.Serializable

class ShowTaskInstancesActivity : ToolbarActivity(), GroupListListener {

    companion object {

        private const val TASK_KEY = "taskKey"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_PAGE = "page"

        fun getIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(TASK_KEY, taskKey as Parcelable)
        }
    }

    private lateinit var taskKey: TaskKey

    private var selectAllVisible = false

    private lateinit var showTaskInstancesViewModel: ShowTaskInstancesViewModel

    override val snackbarParent get() = showNotificationGroupCoordinator!!

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTaskInstancesViewModel.refresh()
    }

    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_notification_group)

        setSupportActionBar(toolbar)

        supportActionBar!!.title = null

        check(intent.hasExtra(TASK_KEY))

        taskKey = intent.getParcelableExtra(TASK_KEY)!!

        if (savedInstanceState != null)
            page = savedInstanceState.getInt(KEY_PAGE)

        showTaskInstancesViewModel = getViewModel<ShowTaskInstancesViewModel>().apply {
            data.doOnNext { groupListFragment.setTaskKey(taskKey, it.dataId, it.immediate, it.dataWrapper, it.showLoader) }
                    .switchMap { groupListFragment.treeViewAdapter.progressShown }
                    .doOnNext { page += 1 }
                    .startWith(Unit)
                    .subscribe { start(taskKey, page) }
                    .addTo(createDisposable)
        }

        initBottomBar()

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    override fun onStart() {
        super.onStart()

        groupListFragment.checkCreatedTaskKey()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_PAGE, page)
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

    override fun getBottomBar() = bottomAppBar!!

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

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
                .also { it.listener = deleteInstancesListener }
                .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    private fun updateBottomMenu() {
        bottomAppBar.menu
                .findItem(R.id.action_select_all)
                ?.isVisible = selectAllVisible
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)
}