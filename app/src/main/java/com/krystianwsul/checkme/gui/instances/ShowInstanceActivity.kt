package com.krystianwsul.checkme.gui.instances

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.NotificationWrapperImpl
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_instance.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowInstanceActivity : AbstractActivity(), GroupListFragment.GroupListListener {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

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

    private var first = false

    private var selectAllVisible = false

    private lateinit var showInstanceViewModel: ShowInstanceViewModel

    override val snackbarParent get() = showInstanceCoordinator!!

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_instance_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.run {
            findItem(R.id.instance_menu_check).isVisible = data?.done == false
            findItem(R.id.instance_menu_uncheck).isVisible = data?.done == true
            findItem(R.id.instance_menu_edit_instance).isVisible = data?.run { !done && isRootInstance } == true
            findItem(R.id.instance_menu_share).isVisible = data != null
            findItem(R.id.instance_menu_show_task).isVisible = data?.taskCurrent == true
            findItem(R.id.instance_menu_edit_task).isVisible = data?.taskCurrent == true
            findItem(R.id.instance_menu_delete_task).isVisible = data?.taskCurrent == true
            findItem(R.id.instance_menu_select_all).isVisible = selectAllVisible
            findItem(R.id.instance_menu_add_task).isVisible = data?.run { isRootInstance && instanceDateTime.timeStamp > TimeStamp.now } == true
            // todo ticks
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        data!!.let {
            when (item.itemId) {
                R.id.instance_menu_check -> {
                    if (!it.done)
                        setDone(true)
                }
                R.id.instance_menu_uncheck -> {
                    if (it.done)
                        setDone(false)
                }
                R.id.instance_menu_edit_instance -> {
                    check(!it.done)
                    check(it.isRootInstance)

                    startActivity(EditInstanceActivity.getIntent(instanceKey))
                }
                R.id.instance_menu_share -> {
                    val shareData = groupListFragment.shareData
                    if (TextUtils.isEmpty(shareData))
                        Utils.share(this, it.name)
                    else
                        Utils.share(this, it.name + "\n" + shareData)
                }
                R.id.instance_menu_show_task -> {
                    check(!it.done)
                    check(it.taskCurrent)

                    showInstanceViewModel.stop()

                    startActivityForResult(ShowTaskActivity.newIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_edit_task -> {
                    check(!it.done)
                    check(it.taskCurrent)

                    showInstanceViewModel.stop()

                    startActivityForResult(CreateTaskActivity.getEditIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_delete_task -> {
                    check(!it.done)
                    check(it.taskCurrent)

                    if (!it.exists)
                        showInstanceViewModel.stop()

                    val todoTaskData = DomainFactory.getInstance().setTaskEndTimeStamp(it.dataId, SaveService.Source.GUI, instanceKey.taskKey)

                    if (it.exists) {
                        it.taskCurrent = false

                        invalidateOptionsMenu()

                        showSnackbar(1) {
                            it.taskCurrent = true

                            invalidateOptionsMenu()

                            DomainFactory.getInstance().clearTaskEndTimeStamps(it.dataId, SaveService.Source.GUI, todoTaskData)
                        }
                    } else {
                        setSnackbar(todoTaskData)

                        finish()
                    }
                }
                R.id.instance_menu_select_all -> {
                    groupListFragment.treeViewAdapter.updateDisplayedNodes {
                        groupListFragment.selectAll(TreeViewAdapter.Placeholder)
                    }
                }
                R.id.instance_menu_add_task -> {
                    data!!.instanceDateTime.let {
                        startActivity(CreateTaskActivity.getCreateIntent(this, CreateTaskActivity.ScheduleHint(it.date, it.time.timePair)))
                    }
                }
                else -> throw UnsupportedOperationException()
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_instance)

        setSupportActionBar(toolbar)
        supportActionBar!!.title = null

        if (savedInstanceState == null)
            first = true

        groupListFragment.setFab(showInstanceFab)

        check(intent.hasExtra(INSTANCE_KEY))
        instanceKey = intent.getParcelableExtra(INSTANCE_KEY)!!

        cancelNotification()

        showInstanceViewModel = getViewModel<ShowInstanceViewModel>().apply {
            start(instanceKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val instanceKey = intent!!.getParcelableExtra<InstanceKey>(INSTANCE_KEY)
        if (instanceKey == this.instanceKey) {
            setIntent(intent)
            cancelNotification()
            setInstanceNotified()
        } else {
            startActivity(getForwardIntent(this, instanceKey, intent.getIntExtra(NOTIFICATION_ID_KEY, -1)))
        }
    }

    private fun cancelNotification() {
        val notificationId = intent.takeIf { it.hasExtra(NOTIFICATION_ID_KEY) }?.getIntExtra(NOTIFICATION_ID_KEY, -1)

        NotificationWrapper.instance.run {
            notificationId?.let { cancelNotification(it) }

            cleanGroup(null)
        }
    }

    private fun setInstanceNotified() {
        DomainFactory.getInstance().let {
            val remoteCustomTimeFixInstanceKey = NotificationWrapperImpl.getRemoteCustomTimeFixInstanceKey(it, instanceKey)

            it.setInstanceNotified(data!!.dataId, SaveService.Source.GUI, remoteCustomTimeFixInstanceKey)
        }
    }

    private fun onLoadFinished(data: ShowInstanceViewModel.Data) {
        this.data = data

        if (first) {
            first = false

            setInstanceNotified()
        }

        supportActionBar!!.run {
            title = data.name
            subtitle = data.displayText
        }

        invalidateOptionsMenu()

        groupListFragment.setInstanceKey(instanceKey, data.dataId, data.dataWrapper)
    }

    private fun setDone(done: Boolean) {
        DomainFactory.getInstance().setInstanceDone(data!!.dataId, SaveService.Source.GUI, instanceKey, done)

        data!!.let {
            it.done = done

            if (done)
                it.exists = true
        }

        invalidateOptionsMenu()
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowTaskActivity.REQUEST_EDIT_TASK)

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
