package com.krystianwsul.checkme.gui.instances

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_notification_group.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class ShowNotificationGroupActivity : ToolbarActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val INSTANCES_KEY = "instanceKeys"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(context: Context, instanceKeys: ArrayList<InstanceKey>) = Intent(context, ShowNotificationGroupActivity::class.java).apply {
            check(instanceKeys.isNotEmpty())

            putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys)
        }
    }

    private lateinit var instanceKeys: Set<InstanceKey>

    private var selectAllVisible = false

    private lateinit var showNotificationGroupViewModel: ShowNotificationGroupViewModel

    override val snackbarParent get() = showNotificationGroupCoordinator!!

    private val deleteInstancesListener = { taskKeys: Set<TaskKey>, removeInstances: Boolean ->
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys, removeInstances)

        showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_notification_group)

        check(intent.hasExtra(INSTANCES_KEY))

        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = HashSet(instanceKeys)

        showNotificationGroupViewModel = getViewModel<ShowNotificationGroupViewModel>().apply {
            start(this@ShowNotificationGroupActivity.instanceKeys)

            createDisposable += data.subscribe { groupListFragment.setInstanceKeys(it.dataId, it.immediate, it.dataWrapper) }
        }

        initBottomBar()

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    override fun onStart() {
        super.onStart()

        groupListFragment.checkCreatedTaskKey()
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomBar()
    }

    private fun updateBottomBar() {
        bottomAppBar.menu
                .findItem(R.id.action_select_all)
                ?.isVisible = selectAllVisible
    }

    override fun getBottomBar() = bottomAppBar!!

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_select_all -> {
                        groupListFragment.treeViewAdapter.updateDisplayedNodes {
                            groupListFragment.selectAll(TreeViewAdapter.Placeholder)
                        }
                    }
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }

        updateBottomBar()
    }

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
                .also { it.listener = deleteInstancesListener }
                .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)
}