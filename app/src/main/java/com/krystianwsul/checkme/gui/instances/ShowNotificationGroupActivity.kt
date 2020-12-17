package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowNotificationGroupBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import java.io.Serializable
import java.util.*

class ShowNotificationGroupActivity : AbstractActivity(), GroupListListener {

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

    override val snackbarParent get() = binding.showNotificationGroupCoordinator

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances)

        showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showNotificationGroupViewModel.refresh()
    }

    override val instanceSearch by lazy {
        binding.showNotificationGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .filterCriteria
    }

    private var data: ShowNotificationGroupViewModel.Data? = null

    private lateinit var binding: ActivityShowNotificationGroupBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowNotificationGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.groupListFragment.listener = this

        check(intent.hasExtra(INSTANCES_KEY))

        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = HashSet(instanceKeys)

        binding.showNotificationGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .apply {
                    setSearchMenuOptions(false, true)
                    configureMenu(R.menu.show_task_menu_top, R.id.actionShowTaskSearch)
                }

        updateTopMenu()
        initBottomBar()

        showNotificationGroupViewModel = getViewModel<ShowNotificationGroupViewModel>().apply {
            start(this@ShowNotificationGroupActivity.instanceKeys)

            createDisposable += data.subscribe {
                this@ShowNotificationGroupActivity.data = it

                binding.groupListFragment.setInstanceKeys(it.dataId, it.immediate, it.groupListDataWrapper)

                updateTopMenu()
            }
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    private fun updateTopMenu() {
        binding.showNotificationGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .menu
                .findItem(R.id.actionShowTaskSearch)
                .isVisible = !data?.groupListDataWrapper
                ?.instanceDatas
                .isNullOrEmpty()
    }

    override fun onStart() {
        super.onStart()

        binding.groupListFragment.checkCreatedTaskKey()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onCreateGroupActionMode(
            actionMode: ActionMode,
            treeViewAdapter: TreeViewAdapter<AbstractHolder>,
            initial: Boolean,
    ) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomBar()
    }

    private fun updateBottomBar() {
        bottomBinding.bottomAppBar
                .menu
                .findItem(R.id.action_select_all)
                ?.isVisible = selectAllVisible
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_select_all -> binding.groupListFragment
                            .treeViewAdapter
                            .selectAll()
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

    override fun setToolbarExpanded(expanded: Boolean) = binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .setExpanded(expanded)

    override fun onBackPressed() {
        binding.showNotificationGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .apply { if (isSearching) closeSearch() else super.onBackPressed() }
    }
}