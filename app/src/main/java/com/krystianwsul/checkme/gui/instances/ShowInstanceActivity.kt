package com.krystianwsul.checkme.gui.instances

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.gui.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.ToolbarActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.startTicks
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_instance.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.toolbar.appBarLayout
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.android.synthetic.main.toolbar_collapse.*
import java.io.Serializable

class ShowInstanceActivity : ToolbarActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

        private const val EDIT_INSTANCES_TAG = "editInstances"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(context: Context, instanceKey: InstanceKey) = Intent(context, ShowInstanceActivity::class.java).apply { putExtra(INSTANCE_KEY, instanceKey as Parcelable) }

        fun getNotificationIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) = Intent(context, ShowInstanceActivity::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        fun getForwardIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) = Intent(context, ShowInstanceActivity::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
        }
    }

    private lateinit var instanceKey: InstanceKey

    private var data: ShowInstanceViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var showInstanceViewModel: ShowInstanceViewModel

    override val snackbarParent get() = showInstanceCoordinator!!

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = updateBottomMenu()
    }

    @Suppress("UNCHECKED_CAST")
    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        showInstanceViewModel.stop()

        val (undoTaskData, visible) = DomainFactory.instance.setTaskEndTimeStamps(SaveService.Source.GUI, taskKeys as Set<TaskKey>, removeInstances, instanceKey)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_instance)

        toolbar.apply {
            menuInflater.inflate(R.menu.show_instance_menu_top, menu)

            setOnMenuItemClickListener { item ->
                data!!.also {
                    when (item.itemId) {
                        R.id.instanceMenuNotify -> {
                            check(!it.done)
                            check(it.instanceDateTime.timeStamp <= TimeStamp.now)
                            check(it.isRootInstance)

                            if (!it.notificationShown) { // to ignore double taps
                                DomainFactory.instance.setInstancesNotNotified(0, SaveService.Source.GUI, listOf(instanceKey))
                            }
                        }
                        R.id.instanceMenuHour -> {
                            check(showHour())

                            val hourUndoData = DomainFactory.instance.setInstancesAddHourActivity(0, SaveService.Source.GUI, listOf(instanceKey))

                            showSnackbarHour(hourUndoData.instanceDateTimes.size) {
                                DomainFactory.instance.undoInstancesAddHour(0, SaveService.Source.GUI, hourUndoData)
                            }
                        }
                        R.id.instanceMenuEditInstance -> {
                            check(!it.done)
                            check(it.isRootInstance)

                            EditInstancesFragment.newInstance(listOf(instanceKey)).show(supportFragmentManager, EDIT_INSTANCES_TAG)
                        }
                        R.id.instanceMenuCheck -> {
                            if (!it.done)
                                setDone(true)
                        }
                        R.id.instanceMenuUncheck -> {
                            if (it.done)
                                setDone(false)
                        }
                    }
                }

                true
            }
        }
        updateTopMenu()

        initBottomBar()

        groupListFragment.setFab(bottomFab)

        check(intent.hasExtra(INSTANCE_KEY))
        instanceKey = intent.getParcelableExtra(INSTANCE_KEY)!!

        cancelNotification()

        if (savedInstanceState == null)
            setInstanceNotified()

        showInstanceViewModel = getViewModel<ShowInstanceViewModel>().apply {
            start(instanceKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener
    }

    private fun showHour() = data?.run { !done && isRootInstance && instanceDateTime.timeStamp <= TimeStamp.now } == true

    private fun updateTopMenu() {
        toolbar.menu.apply {
            findItem(R.id.instanceMenuEditInstance).isVisible = data?.run { !done && isRootInstance } == true
            findItem(R.id.instanceMenuNotify).isVisible = data?.run { !done && isRootInstance && instanceDateTime.timeStamp <= TimeStamp.now && !notificationShown } == true
            findItem(R.id.instanceMenuHour).isVisible = showHour()
            findItem(R.id.instanceMenuCheck).isVisible = data?.done == false
            findItem(R.id.instanceMenuUncheck).isVisible = data?.done == true
        }
    }

    private fun updateBottomMenu() {
        bottomAppBar.menu.run {
            if (findItem(R.id.instance_menu_share) == null)
                return

            findItem(R.id.instance_menu_share).isVisible = data != null
            findItem(R.id.instance_menu_show_task).isVisible = data != null
            findItem(R.id.instance_menu_edit_task).isVisible = data?.taskCurrent == true
            findItem(R.id.instance_menu_delete_task).isVisible = data?.taskCurrent == true
            findItem(R.id.instance_menu_select_all).isVisible = selectAllVisible
            findItem(R.id.instance_menu_add_task).isVisible = data?.run { isRootInstance && instanceDateTime.timeStamp > TimeStamp.now } == true
            findItem(R.id.instanceMenuCopyTask).isVisible = data?.taskCurrent == true
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
            startActivity(getForwardIntent(this, instanceKey, intent.getIntExtra(NOTIFICATION_ID_KEY, -1)))
        }
    }

    override fun onStart() {
        super.onStart()

        startTicks(broadcastReceiver)

        groupListFragment.checkCreatedTaskKey()
    }

    override fun onStop() {
        unregisterReceiver(broadcastReceiver)

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

        toolbar.run {
            title = data.name
            subtitle = data.subtitle
        }

        toolbarCollapseText.apply {
            visibility = if (data.displayText.isNullOrEmpty()) View.GONE else View.VISIBLE
            text = data.displayText
        }

        updateTopMenu()
        updateBottomMenu()

        groupListFragment.setInstanceKey(instanceKey, data.dataId, data.immediate, data.dataWrapper)
    }

    private fun setDone(done: Boolean) = DomainFactory.instance.setInstanceDone(0, SaveService.Source.GUI, instanceKey, done)

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter<NodeHolder>) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun getBottomBar() = bottomAppBar!!

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.show_instance_menu_bottom)

            setOnMenuItemClickListener { item ->
                data!!.let {
                    when (item.itemId) {
                        R.id.instance_menu_share -> {
                            val shareData = groupListFragment.shareData
                            if (TextUtils.isEmpty(shareData))
                                Utils.share(this@ShowInstanceActivity, it.name)
                            else
                                Utils.share(this@ShowInstanceActivity, it.name + "\n" + shareData)
                        }
                        R.id.instance_menu_show_task -> {
                            showInstanceViewModel.stop()

                            startActivityForResult(ShowTaskActivity.newIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                        }
                        R.id.instance_menu_edit_task -> {
                            check(it.taskCurrent)

                            showInstanceViewModel.stop()

                            startActivityForResult(CreateTaskActivity.getEditIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                        }
                        R.id.instance_menu_delete_task -> {
                            check(it.taskCurrent)

                            deleteTasks(setOf(instanceKey.taskKey))
                        }
                        R.id.instance_menu_select_all -> {
                            groupListFragment.treeViewAdapter.updateDisplayedNodes {
                                groupListFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        R.id.instance_menu_add_task -> {
                            data!!.instanceDateTime.let {
                                startActivity(CreateTaskActivity.getCreateIntent(CreateTaskActivity.Hint.Schedule(it.date, it.time.timePair)))
                            }
                        }
                        R.id.instanceMenuCopyTask -> {
                            startActivity(CreateTaskActivity.getCopyIntent(data!!.taskKey))
                        }
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

                instanceKey = InstanceKey(taskKey, instanceKey.scheduleKey.scheduleDate, TimePair(instanceKey.scheduleKey.scheduleTimePair.customTimeKey, instanceKey.scheduleKey.scheduleTimePair.hourMinute))
            } else if (resultCode == ShowTaskActivity.RESULT_DELETE) {
                if (!this.data!!.exists) {
                    finish()
                    return
                }
            }

            showInstanceViewModel.start(instanceKey)
        }
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)
}
