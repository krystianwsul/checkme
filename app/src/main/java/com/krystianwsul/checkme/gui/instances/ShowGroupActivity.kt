package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowGroupBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.edit.SnackbarEditInstancesHostDelegate
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListParameters
import com.krystianwsul.checkme.gui.instances.list.GroupListViewModel
import com.krystianwsul.checkme.gui.instances.list.GroupMenuUtils
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.CopyAllRemindersDelegate
import com.krystianwsul.checkme.gui.widgets.toQuery
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import java.io.Serializable

class ShowGroupActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_BOTTOM_FAB_MENU_DELEGATE_STATE = "bottomFabMenuDelegateState"

        fun getIntent(context: Context, parameters: Parameters) = Intent(
            context,
            ShowGroupActivity::class.java
        ).apply { putExtra(KEY_PARAMETERS, parameters) }
    }

    private lateinit var parameters: Parameters

    private var selectAllVisible = false

    private val showGroupViewModel by lazy { getViewModel<ShowGroupViewModel>() }

    override val snackbarParent get() = binding.showGroupCoordinator

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setTaskEndTimeStamps(
            showGroupViewModel.dataId.toFirst(),
            taskKeys as Set<TaskKey>,
            removeInstances,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
            .flatMapCompletable { AndroidDomainUpdater.clearTaskEndTimeStamps(showGroupViewModel.dataId.toFirst(), it) }
            .subscribe()
            .addTo(createDisposable)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showGroupViewModel.refresh()
    }

    private var data: ShowGroupViewModel.Data? = null

    private lateinit var binding: ActivityShowGroupBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

    override val groupListViewModel by lazy { getViewModel<GroupListViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.showGroupCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE),
        )

        binding.groupListFragment.listener = this

        parameters = intent.getParcelableExtra(KEY_PARAMETERS) ?: return

        binding.groupListFragment.setVisible(bottomFabMenuDelegate.fabDelegate)

        binding.showGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .apply {
                setSearchMenuOptions(false, true, false)

                configureMenu(
                    R.menu.show_group_menu_top,
                    R.id.actionShowGroupSearch,
                    showAssignedToOthersId = R.id.actionShowGroupAssigned
                ) { itemId ->
                    val instanceDatas = data!!.groupListDataWrapper!!.allInstanceDatas
                    val dataId = showGroupViewModel.dataId
                    val listener = this@ShowGroupActivity

                    when (itemId) {
                        R.id.actionShowGroupNotify -> GroupMenuUtils.onNotify(instanceDatas, dataId).addTo(createDisposable)
                        R.id.actionShowGroupHour -> {
                            val oldParameters = parameters as Parameters.Project

                            fun changeParameters(newParameters: Parameters.Project) {
                                parameters = newParameters

                                showGroupViewModel.start(parameters)
                            }

                            GroupMenuUtils.onHour(
                                instanceDatas,
                                dataId,
                                listener,
                                { showGroupViewModel.stop() },
                                { changeParameters(oldParameters.copy(timeStamp = it)) },
                                { changeParameters(oldParameters) },
                            ).addTo(createDisposable)
                        }
                        R.id.actionShowGroupEditInstance -> GroupMenuUtils.onEdit(instanceDatas, editInstancesHostDelegate)
                        R.id.actionShowGroupCheck ->
                            GroupMenuUtils.onCheck(instanceDatas, dataId, listener).addTo(createDisposable)
                        R.id.actionShowGroupUncheck ->
                            GroupMenuUtils.onUncheck(instanceDatas, dataId, listener).addTo(createDisposable)
                    }
                }
            }

        updateTopMenu()
        initBottomBar()

        showGroupViewModel.apply {
            start(parameters)

            binding.showGroupToolbarCollapseInclude
                .collapseAppBarLayout
                .searchParamsObservable
                .toQuery()
                .subscribe(searchRelay)
                .addTo(createDisposable)

            createDisposable += data.subscribe(::onLoadFinished)
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        editInstancesHostDelegate.onCreate()

        startDate(receiver)

        CopyAllRemindersDelegate(this, groupListViewModel, createDisposable)
    }

    private val editInstancesHostDelegate = object : SnackbarEditInstancesHostDelegate(createDisposable) {

        override val dataId get() = showGroupViewModel.dataId

        override val activity = this@ShowGroupActivity

        override val snackbarListener = this@ShowGroupActivity

        private var possiblyClosing = false

        override fun beforeEditInstances(instanceKeys: Set<InstanceKey>) {
            if (parameters is Parameters.Project &&
                instanceKeys == data!!.groupListDataWrapper!!
                    .allInstanceDatas
                    .map { it.instanceKey }
                    .toSet()
            ) {
                showGroupViewModel.stop()
                possiblyClosing = true
            }
        }

        /**
         * This is awful, but I didn't feel like organizing it better.  So, if all instances are being edited on the screen,
         * then it would normally be empty afterwards.  So if that flag gets set, then if a timestamp is returned (meaning
         * we edited times, not parent) then we change the time for the screen.  Otherwise (parent set), we give up and close
         * it.
         */

        override fun afterEditInstances(undoData: UndoData, count: Int, newTimeStamp: TimeStamp?) {
            if (possiblyClosing) {
                check(parameters is Parameters.Project)

                if (newTimeStamp == null) {
                    finish()
                } else {
                    possiblyClosing = false

                    parameters = (parameters as Parameters.Project).copy(timeStamp = newTimeStamp)

                    showGroupViewModel.start(parameters)
                }
            } else {
                super.afterEditInstances(undoData, count, newTimeStamp)
            }
        }
    }

    private fun updateTopMenu() {
        val instanceDatas = data?.groupListDataWrapper?.allInstanceDatas

        var hasItems = false
        var notify = false
        var hour = false
        var edit = false
        var check = false
        var uncheck = false
        if (!instanceDatas.isNullOrEmpty()) {
            hasItems = true

            if (parameters is Parameters.Project && hasItems) {
                // yes, there's a reason for any/all here.  It may not be good, but it exists.

                notify = GroupMenuUtils.showNotification(instanceDatas)
                hour = instanceDatas.any(GroupMenuUtils::showHour)
                edit = instanceDatas.any(GroupMenuUtils::showEdit)
                check = instanceDatas.any(GroupMenuUtils::showCheck)
                uncheck = GroupMenuUtils.showUncheck(instanceDatas)
            }
        }

        binding.showGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .menu
            .apply {
                findItem(R.id.actionShowGroupSearch).isVisible = hasItems

                // project section
                findItem(R.id.actionShowGroupNotify).isVisible = notify
                findItem(R.id.actionShowGroupHour).isVisible = hour
                findItem(R.id.actionShowGroupEditInstance).isVisible = edit
                findItem(R.id.actionShowGroupCheck).isVisible = check
                findItem(R.id.actionShowGroupUncheck).isVisible = uncheck

                findItem(R.id.actionShowGroupAssigned).isVisible = hasItems && parameters.showAssignedToOthers
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
            .setText(data.title, data.subtitle, binding.groupListFragment.emptyTextLayout, immediate)

        if (data.groupListDataWrapper == null) {
            finish()

            return
        }

        binding.groupListFragment.setParameters(
            GroupListParameters.TimeStamp(
                showGroupViewModel.dataId,
                immediate,
                data.groupListDataWrapper,
                parameters.getFabData(),
            )
        )

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
    ) = binding.showGroupToolbarCollapseInclude
        .collapseAppBarLayout
        .collapse()

    override fun onDestroyGroupActionMode() = binding.showGroupToolbarCollapseInclude
        .collapseAppBarLayout
        .expand()

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

    override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
            .also { it.listener = deleteInstancesListener }
            .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) = bottomFabMenuDelegate.showMenu(menuDelegate)

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
    }

    sealed interface Parameters : Parcelable {

        val projectKey: ProjectKey.Shared?
        val groupingMode: GroupType.GroupingMode
        val showAssignedToOthers: Boolean

        fun getFabData(): GroupListParameters.TimeStamp.FabData

        sealed interface TimeBased : Parameters {

            val timeStamp: TimeStamp
            val showUngrouped: Boolean

            override val showAssignedToOthers get() = true

            override fun getFabData() = GroupListParameters.TimeStamp.FabData.TimeBased(timeStamp, projectKey)
        }

        @Parcelize
        data class Time(override val timeStamp: TimeStamp) : TimeBased {

            override val projectKey: ProjectKey.Shared? get() = null

            override val groupingMode get() = GroupType.GroupingMode.Project

            override val showUngrouped get() = true
        }

        @Parcelize
        data class Project(
            override val timeStamp: TimeStamp,
            override val projectKey: ProjectKey.Shared,
            override val showUngrouped: Boolean = true,
        ) : TimeBased {

            override val groupingMode get() = GroupType.GroupingMode.None
        }

        // for project node inside instance
        @Parcelize
        data class InstanceProject(
            override val projectKey: ProjectKey.Shared, // group hack
            val parentInstanceKey: InstanceKey,
        ) : Parameters {

            override val groupingMode get() = GroupType.GroupingMode.None

            override val showAssignedToOthers get() = false

            override fun getFabData() = GroupListParameters.TimeStamp.FabData.InstanceProject(parentInstanceKey, projectKey)
        }
    }
}