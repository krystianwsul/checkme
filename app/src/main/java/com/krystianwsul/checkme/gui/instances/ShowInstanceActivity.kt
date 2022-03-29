package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowInstanceBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.edit.EditInstancesHostDelegate
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListViewModel
import com.krystianwsul.checkme.gui.main.DebugFragment
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.CopyAllRemindersDelegate
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.kotlin.plusAssign
import java.io.Serializable

class ShowInstanceActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_BOTTOM_FAB_MENU_DELEGATE_STATE = "bottomFabMenuDelegateState"

        fun getIntent(context: Context, instanceKey: InstanceKey) =
            Intent(context, ShowInstanceActivity::class.java).apply {
                putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            }

        fun getNotificationIntent(context: Context, instanceKey: InstanceKey) =
            Intent(context, ShowInstanceActivity::class.java).apply {
                putExtra(INSTANCE_KEY, instanceKey as Parcelable)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        fun getForwardIntent(context: Context, instanceKey: InstanceKey) =
            Intent(context, ShowInstanceActivity::class.java).putExtra(INSTANCE_KEY, instanceKey as Parcelable)
    }

    private lateinit var instanceKey: InstanceKey

    private var data: ShowInstanceViewModel.Data? = null

    private var selectAllVisible = false

    private val showInstanceViewModel by lazy { getViewModel<ShowInstanceViewModel>() }

    override val snackbarParent get() = binding.showInstanceCoordinator

    private val ticksReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = updateBottomMenu()
    }

    private val dateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showInstanceViewModel.refresh()
    }

    @Suppress("UNCHECKED_CAST")
    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        showInstanceViewModel.stop()

        val undoTaskDataSingle = AndroidDomainUpdater.setTaskEndTimeStamps(
            showInstanceViewModel.dataId.toFirst(),
            taskKeys as Set<TaskKey>,
            removeInstances,
            instanceKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .cache()

        undoTaskDataSingle.filter { (_, visible) -> visible }
            .map { (undoTaskData, _) -> undoTaskData }
            .doOnSuccess { showInstanceViewModel.start(instanceKey) }
            .flatMap { showSnackbarRemovedMaybe(taskKeys.size).map { _ -> it } }
            .flatMapCompletable {
                AndroidDomainUpdater.clearTaskEndTimeStamps(showInstanceViewModel.dataId.toFirst(), it)
            }
            .subscribe()
            .addTo(createDisposable)

        undoTaskDataSingle.filter { (_, visible) -> !visible }
            .map { (undoTaskData, _) -> undoTaskData }
            .subscribe {
                setSnackbar(it)
                finish()
            }
            .addTo(createDisposable)
    }

    override val instanceSearch by lazy { // todo expand
        binding.showInstanceToolbarCollapseInclude
            .collapseAppBarLayout
            .filterCriteria
            .cast<FilterCriteria>()
    }

    private lateinit var binding: ActivityShowInstanceBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

    private val editInstancesHostDelegate = object : EditInstancesHostDelegate() {

        override val dataId get() = showInstanceViewModel.dataId

        override val activity = this@ShowInstanceActivity

        override fun afterEditInstances(undoData: UndoData, count: Int, newTimeStamp: TimeStamp?) {}
    }

    override val groupListViewModel by lazy { getViewModel<GroupListViewModel>() }
    private lateinit var copyAllRemindersDelegate: CopyAllRemindersDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowInstanceBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.showInstanceCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE),
        )

        binding.groupListFragment.listener = this

        binding.showInstanceToolbarCollapseInclude
            .collapseAppBarLayout
            .apply {
                setSearchMenuOptions(false, false, false)

                configureMenu(R.menu.show_instance_menu_top, R.id.instanceMenuSearch) { itemId ->
                    data!!.also {
                        when (itemId) {
                            R.id.instanceMenuNotify -> {
                                check(!it.done)
                                check(it.instanceDateTime.timeStamp <= TimeStamp.now)
                                check(it.isRootInstance)

                                AndroidDomainUpdater.setInstancesNotNotified(
                                    showInstanceViewModel.dataId.toFirst(),
                                    listOf(instanceKey),
                                    false,
                                ).subscribe()
                            }
                            R.id.instanceMenuHour -> {
                                check(showHour())

                                AndroidDomainUpdater.setInstancesAddHourActivity(
                                    showInstanceViewModel.dataId.toFirst(),
                                    listOf(instanceKey),
                                )
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .flatMapMaybe {
                                        showSnackbarHourMaybe(it.instanceDateTimes.size).map { _ -> it }
                                    }
                                    .flatMapCompletable {
                                        AndroidDomainUpdater.undoInstancesAddHour(
                                            showInstanceViewModel.dataId.toFirst(),
                                            it,
                                        )
                                    }
                                    .subscribe()
                                    .addTo(createDisposable)
                            }
                            R.id.instanceMenuEditInstance -> {
                                check(!it.done)

                                editInstancesHostDelegate.show(listOf(instanceKey))
                            }
                            R.id.instanceMenuSplit -> {
                                AndroidDomainUpdater.splitInstance(
                                    DomainListenerManager.NotificationType.All,
                                    instanceKey,
                                ).subscribe()

                                finish()
                            }
                            R.id.instanceMenuCheck -> if (!it.done) setDone(true) // todo flowable
                            R.id.instanceMenuUncheck -> if (it.done) setDone(false)
                        }
                    }
                }
            }

        editInstancesHostDelegate.onCreate()

        updateTopMenu()

        initBottomBar()

        binding.groupListFragment.setVisible(bottomFabMenuDelegate.fabDelegate)

        instanceKey = (savedInstanceState ?: intent.extras)?.getParcelable(INSTANCE_KEY) ?: return

        showInstanceViewModel.apply {
            start(instanceKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(dateReceiver)

        copyAllRemindersDelegate = CopyAllRemindersDelegate(this, groupListViewModel, createDisposable)
    }

    private fun showHour() = data?.run {
        !done && isRootInstance && instanceDateTime.timeStamp <= TimeStamp.now
    } == true

    private fun updateTopMenu() {
        binding.showInstanceToolbarCollapseInclude
            .collapseAppBarLayout
            .menu
            .apply {
                findItem(R.id.instanceMenuSearch).isVisible = !data?.groupListDataWrapper
                    ?.allInstanceDatas
                    .isNullOrEmpty()
                findItem(R.id.instanceMenuEditInstance).isVisible = data?.done == false
                findItem(R.id.instanceMenuSplit).isVisible = data?.run {
                    !done && groupListDataWrapper.allInstanceDatas.size > 1
                } == true
                findItem(R.id.instanceMenuNotify).isVisible = data?.run {
                    !done && isRootInstance && instanceDateTime.timeStamp <= TimeStamp.now
                } == true
                findItem(R.id.instanceMenuHour).isVisible = showHour()
                findItem(R.id.instanceMenuCheck).isVisible = data?.done == false
                findItem(R.id.instanceMenuUncheck).isVisible = data?.done == true
            }
    }

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
            .menu
            .run {
                if (findItem(R.id.instance_menu_share) == null) return

                findItem(R.id.instance_menu_share).isVisible = data != null
                findItem(R.id.instance_menu_show_task).isVisible = data != null
                findItem(R.id.instance_menu_edit_task).isVisible = data?.taskCurrent == true
                findItem(R.id.instance_menu_delete_task).isVisible = data?.taskCurrent == true
                findItem(R.id.instance_menu_select_all).isVisible = selectAllVisible
                findItem(R.id.instanceMenuCopyTask).isVisible = data?.taskCurrent == true
                findItem(R.id.instanceMenuWebSearch).isVisible = data != null
                findItem(R.id.instanceMenuMigrateDescription).isVisible = data?.canMigrateDescription == true
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!
        if (instanceKey == this.instanceKey) {
            setIntent(intent)
        } else {
            startActivity(getForwardIntent(this, instanceKey))
        }
    }

    override fun onStart() {
        super.onStart()

        startTicks(ticksReceiver)

        binding.groupListFragment.checkCreatedTaskKey()
    }

    override fun onStop() {
        unregisterReceiver(ticksReceiver)

        super.onStop()
    }

    private fun onLoadFinished(data: ShowInstanceViewModel.Data) {
        DebugFragment.logDone("ShowInstanceActivity.onLoadFinished")

        this.data = data
        this.instanceKey = data.newInstanceKey

        if (!data.isVisible) {
            finish()
            return
        }

        val immediate = data.immediate

        binding.showInstanceToolbarCollapseInclude
            .collapseAppBarLayout
            .setText(data.name, data.displayText, binding.groupListFragment.emptyTextLayout, immediate)

        updateTopMenu()
        updateBottomMenu()

        binding.groupListFragment.setInstanceKey(
            instanceKey,
            showInstanceViewModel.dataId,
            immediate,
            data.groupListDataWrapper,
        )
    }

    private fun setDone(done: Boolean) {
        DebugFragment.logDone("ShowInstanceActivity.setDone start")
        AndroidDomainUpdater.setInstanceDone(showInstanceViewModel.dataId.toFirst(), instanceKey, done)
            .subscribe { DebugFragment.logDone("ShowInstanceActivity.setDone onSuccess") }
            .addTo(createDisposable)
    }

    override fun onCreateGroupActionMode(
        actionMode: ActionMode,
        treeViewAdapter: TreeViewAdapter<AbstractHolder>,
        initial: Boolean,
    ) = binding.showInstanceToolbarCollapseInclude
        .collapseAppBarLayout
        .collapse()

    override fun onDestroyGroupActionMode() = binding.showInstanceToolbarCollapseInclude
        .collapseAppBarLayout
        .expand()

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun getBottomBar() = bottomBinding.bottomAppBar

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            replaceMenu(R.menu.show_instance_menu_bottom)

            setOnMenuItemClickListener { item ->
                data!!.let {
                    when (item.itemId) {
                        R.id.instance_menu_share -> {
                            val shareData = binding.groupListFragment.shareData
                            Utils.share(
                                this@ShowInstanceActivity,
                                it.name + if (shareData.isEmpty()) "" else "\n" + shareData
                            )
                        }
                        R.id.instance_menu_show_task -> startActivity(ShowTaskActivity.newIntent(instanceKey.taskKey))
                        R.id.instance_menu_edit_task -> {
                            check(it.taskCurrent)

                            startActivity(EditActivity.getParametersIntent(EditParameters.Edit(instanceKey)))
                        }
                        R.id.instance_menu_delete_task -> {
                            check(it.taskCurrent)

                            deleteTasks(showInstanceViewModel.dataId, setOf(instanceKey.taskKey))
                        }
                        R.id.instance_menu_select_all -> binding.groupListFragment
                            .treeViewAdapter
                            .selectAll()
                        R.id.instanceMenuCopyTask -> copyAllRemindersDelegate.showDialog(instanceKey)
                        R.id.instanceMenuWebSearch -> startActivity(webSearchIntent(data!!.name))
                        R.id.instanceMenuMigrateDescription -> startActivity(
                            EditActivity.getParametersIntent(EditParameters.MigrateDescription(data!!.taskKey))
                        )
                        else -> throw UnsupportedOperationException()
                    }
                }

                true
            }
        }

        updateBottomMenu()
    }

    override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
            .also { it.listener = deleteInstancesListener }
            .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) = bottomFabMenuDelegate.showMenu(menuDelegate)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(INSTANCE_KEY, instanceKey)

        outState.putParcelable(KEY_BOTTOM_FAB_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
    }

    override fun onDestroy() {
        unregisterReceiver(dateReceiver)

        super.onDestroy()
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showInstanceToolbarCollapseInclude
        .collapseAppBarLayout
        .setExpanded(expanded)

    override fun onBackPressed() {
        binding.showInstanceToolbarCollapseInclude
            .collapseAppBarLayout
            .apply { if (isSearching) closeSearch() else super.onBackPressed() }
    }
}
