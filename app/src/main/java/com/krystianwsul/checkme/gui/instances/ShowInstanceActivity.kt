package com.krystianwsul.checkme.gui.instances

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowInstanceBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import java.io.Serializable

class ShowInstanceActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

        private const val EDIT_INSTANCES_TAG = "editInstances"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(context: Context, instanceKey: InstanceKey) =
                Intent(context, ShowInstanceActivity::class.java).apply {
                    putExtra(INSTANCE_KEY, instanceKey as Parcelable)
                }

        fun getNotificationIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) =
                Intent(context, ShowInstanceActivity::class.java).apply {
                    putExtra(INSTANCE_KEY, instanceKey as Parcelable)
                    putExtra(NOTIFICATION_ID_KEY, notificationId)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

        fun getForwardIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) =
                Intent(context, ShowInstanceActivity::class.java).apply {
                    putExtra(INSTANCE_KEY, instanceKey as Parcelable)
                    putExtra(NOTIFICATION_ID_KEY, notificationId)
                }
    }

    private lateinit var instanceKey: InstanceKey

    private var data: ShowInstanceViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var showInstanceViewModel: ShowInstanceViewModel

    override val snackbarParent get() = binding.showInstanceCoordinator

    private val ticksReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = updateBottomMenu()
    }

    private val dateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showInstanceViewModel.refresh()
    }

    @Suppress("UNCHECKED_CAST")
    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        showInstanceViewModel.stop()

        val (undoTaskData, visible) = DomainFactory.instance.setTaskEndTimeStamps(
                SaveService.Source.GUI,
                taskKeys as Set<TaskKey>,
                removeInstances,
                instanceKey
        )

        if (visible) {
            showInstanceViewModel.start(instanceKey)

            showSnackbarRemoved(taskKeys.size) {
                DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, undoTaskData)
            }
        } else {
            setSnackbar(undoTaskData)

            finish()
        }
    }

    override val instanceSearch by lazy {
        binding.showInstanceToolbarCollapseInclude
                .collapseAppBarLayout
                .filterCriteria
    }

    private lateinit var binding: ActivityShowInstanceBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowInstanceBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.groupListFragment.listener = this

        binding.showInstanceToolbarCollapseInclude
                .collapseAppBarLayout
                .apply {
                    setSearchMenuOptions(false, false)

                    configureMenu(R.menu.show_instance_menu_top) { itemId ->
                        data!!.also {
                            when (itemId) {
                                R.id.instanceMenuSearch -> startSearch()
                                R.id.instanceMenuNotify -> {
                                    check(!it.done)
                                    check(it.instanceDateTime.timeStamp <= TimeStamp.now)
                                    check(it.isRootInstance)

                                    // to ignore double taps
                                    if (!it.notificationShown) DomainFactory.instance.setInstancesNotNotified(
                                            0,
                                            SaveService.Source.GUI,
                                            listOf(instanceKey)
                                    )
                                }
                                R.id.instanceMenuHour -> {
                                    check(showHour())

                                    val hourUndoData = DomainFactory.instance.setInstancesAddHourActivity(
                                            0,
                                            SaveService.Source.GUI,
                                            listOf(instanceKey)
                                    )

                                    showSnackbarHour(hourUndoData.instanceDateTimes.size) {
                                        DomainFactory.instance.undoInstancesAddHour(
                                                0,
                                                SaveService.Source.GUI,
                                                hourUndoData
                                        )
                                    }
                                }
                                R.id.instanceMenuEditInstance -> {
                                    check(!it.done)
                                    check(it.isRootInstance)

                                    EditInstancesFragment.newInstance(listOf(instanceKey)).show(
                                            supportFragmentManager,
                                            EDIT_INSTANCES_TAG
                                    )
                                }
                                R.id.instanceMenuCheck -> if (!it.done) setDone(true)
                                R.id.instanceMenuUncheck -> if (it.done) setDone(false)
                                R.id.instanceMenuRemoveFromParent -> {
                                    check(it.isRecurringGroupChild)

                                    DomainFactory.instance.removeFromParent(SaveService.Source.GUI, listOf(instanceKey))
                                }
                            }
                        }
                    }
                }

        updateTopMenu()

        initBottomBar()

        binding.groupListFragment.setFab(bottomBinding.bottomFab)

        check(intent.hasExtra(INSTANCE_KEY))
        instanceKey = intent.getParcelableExtra(INSTANCE_KEY)!!

        cancelNotification()

        if (savedInstanceState == null) setInstanceNotified()

        showInstanceViewModel = getViewModel<ShowInstanceViewModel>().apply {
            start(instanceKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(dateReceiver)
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
                            ?.instanceDatas
                            .isNullOrEmpty()
                    findItem(R.id.instanceMenuEditInstance).isVisible = data?.run { !done && isRootInstance } == true
                    findItem(R.id.instanceMenuNotify).isVisible = data?.run {
                        !done && isRootInstance && instanceDateTime.timeStamp <= TimeStamp.now && !notificationShown
                    } == true
                    findItem(R.id.instanceMenuHour).isVisible = showHour()
                    findItem(R.id.instanceMenuCheck).isVisible = data?.done == false
                    findItem(R.id.instanceMenuUncheck).isVisible = data?.done == true
                    findItem(R.id.instanceMenuRemoveFromParent).isVisible = data?.isRecurringGroupChild == true
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
                }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!
        if (instanceKey == this.instanceKey) {
            setIntent(intent)
            cancelNotification()
            setInstanceNotified()

            updateTopMenu()
        } else {
            startActivity(getForwardIntent(
                    this,
                    instanceKey,
                    intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
            ))
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

    private fun cancelNotification() = NotificationWrapper.instance.run {
        intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
                .takeIf { it != -1 }
                ?.let {
                    logNotificationIds("ShowInstanceActivity.cancelNotification")

                    cancelNotification(it)
                }

        cleanGroup(null)
    }

    private fun setInstanceNotified() {
        Preferences.tickLog.logLineHour("ShowInstanceActivity: setting notified")

        if (intent.hasExtra(NOTIFICATION_ID_KEY)) {
            DomainFactory.addFirebaseListener {
                it.setInstanceNotified(data?.dataId ?: 0, SaveService.Source.GUI, instanceKey)
            }
            data?.notificationShown = false
        }
    }

    private fun onLoadFinished(data: ShowInstanceViewModel.Data) {
        this.data = data

        val immediate = data.immediate

        binding.showInstanceToolbarCollapseInclude
                .collapseAppBarLayout
                .setText(data.name, data.displayText, binding.groupListFragment.emptyTextLayout, immediate)

        updateTopMenu()
        updateBottomMenu()

        binding.groupListFragment.setInstanceKey(instanceKey, data.dataId, immediate, data.groupListDataWrapper)
    }

    private fun setDone(done: Boolean) {
        DomainFactory.instance.setInstanceDone(
                DomainListenerManager.NotificationType.First(data!!.dataId),
                SaveService.Source.GUI,
                instanceKey,
                done
        )
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
                        R.id.instance_menu_show_task -> {
                            showInstanceViewModel.stop()

                            startActivityForResult(
                                    ShowTaskActivity.newIntent(instanceKey.taskKey),
                                    ShowTaskActivity.REQUEST_EDIT_TASK
                            )
                        }
                        R.id.instance_menu_edit_task -> {
                            check(it.taskCurrent)

                            showInstanceViewModel.stop()

                            startActivityForResult(
                                    EditActivity.getParametersIntent(EditParameters.Edit(instanceKey)),
                                    ShowTaskActivity.REQUEST_EDIT_TASK
                            )
                        }
                        R.id.instance_menu_delete_task -> {
                            check(it.taskCurrent)

                            deleteTasks(setOf(instanceKey.taskKey))
                        }
                        R.id.instance_menu_select_all -> binding.groupListFragment
                                .treeViewAdapter
                                .selectAll()
                        R.id.instanceMenuCopyTask -> startActivity(
                                EditActivity.getParametersIntent(EditParameters.Copy(data!!.taskKey))
                        )
                        R.id.instanceMenuWebSearch -> startActivity(webSearchIntent(data!!.name))
                        else -> throw UnsupportedOperationException()
                    }
                }

                true
            }
        }

        updateBottomMenu()
    }

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(taskKeys)
                .also { it.listener = deleteInstancesListener }
                .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ShowTaskActivity.REQUEST_EDIT_TASK) {
            if (resultCode == Activity.RESULT_OK) {
                check(data!!.hasExtra(ShowTaskActivity.TASK_KEY_KEY))

                val taskKey = data.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!

                instanceKey = instanceKey.scheduleKey.run {
                    InstanceKey(
                            taskKey,
                            scheduleDate,
                            scheduleTimePair.run { TimePair(customTimeKey, hourMinute) }
                    )
                }
            } else if (resultCode == ShowTaskActivity.RESULT_DELETE) {
                if (!this.data!!.exists) {
                    finish()
                    return
                }
            }

            showInstanceViewModel.start(instanceKey)
        }
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
