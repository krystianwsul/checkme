package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowGroupBinding
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
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.plusAssign
import java.io.Serializable

class ShowGroupActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val TIME_KEY = "time"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(exactTimeStamp: ExactTimeStamp.Local, context: Context) = Intent(
                context,
                ShowGroupActivity::class.java
        ).apply { putExtra(TIME_KEY, exactTimeStamp.long) }
    }

    private lateinit var timeStamp: TimeStamp

    private var selectAllVisible = false

    private lateinit var showGroupViewModel: ShowGroupViewModel

    override val snackbarParent get() = binding.showGroupCoordinator

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(
                SaveService.Source.GUI,
                taskKeys as Set<TaskKey>,
                removeInstances
        )

        showSnackbarRemoved(taskUndoData.taskKeys.size) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showGroupViewModel.refresh()
    }

    override val instanceSearch by lazy {
        binding.showGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .filterCriteria
                .cast<FilterCriteria>()
    }

    private var data: ShowGroupViewModel.Data? = null

    private lateinit var binding: ActivityShowGroupBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.groupListFragment.listener = this

        check(intent.hasExtra(TIME_KEY))

        val time = intent.getLongExtra(TIME_KEY, -1)
        check(time != -1L)

        timeStamp = TimeStamp.fromMillis(time)

        binding.groupListFragment.setFab(bottomBinding.bottomFab)

        binding.showGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .apply {
                    setSearchMenuOptions(false, true)
                    configureMenu(R.menu.show_group_menu_top, R.id.actionShowGroupSearch, R.id.actionShowGroupAssigned)
                }

        updateTopMenu()
        initBottomBar()

        showGroupViewModel = getViewModel<ShowGroupViewModel>().apply {
            start(timeStamp)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    private fun updateTopMenu() {
        val hasItems = !data?.groupListDataWrapper
                ?.instanceDatas
                .isNullOrEmpty()

        binding.showGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .menu
                .apply {
                    findItem(R.id.actionShowGroupSearch).isVisible = hasItems
                    findItem(R.id.actionShowGroupAssigned).isVisible = hasItems
                }
    }

    override fun onStart() {
        super.onStart()

        binding.groupListFragment.checkCreatedTaskKey()
    }

    private fun onLoadFinished(data: ShowGroupViewModel.Data) {
        this.data = data

        val immediate = data.immediate

        binding.showGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .setText(data.displayText, null, binding.groupListFragment.emptyTextLayout, immediate)

        if (data.groupListDataWrapper == null) {
            finish()

            return
        }

        binding.groupListFragment.setTimeStamp(timeStamp, data.dataId, immediate, data.groupListDataWrapper)

        updateTopMenu()
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

        updateBottomMenu()
    }

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                check(item.itemId == R.id.action_select_all)

                binding.groupListFragment
                        .treeViewAdapter
                        .selectAll()

                true
            }
        }
    }

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
                .also { it.listener = deleteInstancesListener }
                .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
                .menu
                .findItem(R.id.action_select_all)
                ?.isVisible = selectAllVisible
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .setExpanded(expanded)
}