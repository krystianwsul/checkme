package com.krystianwsul.checkme.gui.instances

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBar
import android.support.v7.view.ActionMode
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.NotificationWrapperImpl
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.loaders.ShowInstanceLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.TimePair

import junit.framework.Assert
import kotlinx.android.synthetic.main.activity_show_instance.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowInstanceActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data>, GroupListFragment.GroupListListener {

    companion object {

        private val INSTANCE_KEY = "instanceKey"
        private val SET_NOTIFIED_KEY = "setNotified"

        fun getIntent(context: Context, instanceKey: InstanceKey) = Intent(context, ShowInstanceActivity::class.java).apply { putExtra(INSTANCE_KEY, instanceKey as Parcelable) }

        fun getNotificationIntent(context: Context, instanceKey: InstanceKey) = Intent(context, ShowInstanceActivity::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(SET_NOTIFIED_KEY, true)
        }
    }

    private lateinit var actionBar: ActionBar

    private lateinit var instanceKey: InstanceKey

    private var dataId = -1
    private var instanceData: ShowInstanceLoader.InstanceData? = null

    private var first = false

    private lateinit var groupListFragment: GroupListFragment

    private var selectAllVisible = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_instance_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.instance_menu_check).isVisible = instanceData?.Done == false

        menu.findItem(R.id.instance_menu_uncheck).isVisible = instanceData?.Done == true

        menu.findItem(R.id.instance_menu_edit_instance).isVisible = instanceData?.run { !Done && IsRootInstance } == true

        menu.findItem(R.id.instance_menu_share).isVisible = instanceData != null

        menu.findItem(R.id.instance_menu_show_task).isVisible = instanceData?.run { !Done && TaskCurrent } == true

        menu.findItem(R.id.instance_menu_edit_task).isVisible = instanceData?.run { !Done && TaskCurrent } == true

        menu.findItem(R.id.instance_menu_delete_task).isVisible = instanceData?.run { !Done && TaskCurrent } == true

        menu.findItem(R.id.instance_menu_select_all).isVisible = selectAllVisible

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        instanceData!!.let {
            when (item.itemId) {
                R.id.instance_menu_check -> {
                    Assert.assertTrue(!it.Done)

                    setDone(true)
                }
                R.id.instance_menu_uncheck -> {
                    Assert.assertTrue(it.Done)

                    setDone(false)
                }
                R.id.instance_menu_edit_instance -> {
                    Assert.assertTrue(!it.Done)
                    Assert.assertTrue(it.IsRootInstance)

                    startActivity(EditInstanceActivity.getIntent(instanceKey))
                }
                R.id.instance_menu_share -> {
                    val shareData = groupListFragment.shareData
                    if (TextUtils.isEmpty(shareData))
                        Utils.share(it.Name)
                    else
                        Utils.share(it.Name + "\n" + shareData)
                }
                R.id.instance_menu_show_task -> {
                    Assert.assertTrue(!it.Done)
                    Assert.assertTrue(it.TaskCurrent)

                    supportLoaderManager.destroyLoader(0)

                    startActivityForResult(ShowTaskActivity.newIntent(instanceKey.mTaskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_edit_task -> {
                    Assert.assertTrue(!it.Done)
                    Assert.assertTrue(it.TaskCurrent)

                    supportLoaderManager.destroyLoader(0)

                    startActivityForResult(CreateTaskActivity.getEditIntent(instanceKey.mTaskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_delete_task -> {
                    Assert.assertTrue(!it.Done)
                    Assert.assertTrue(it.TaskCurrent)

                    if (!it.mExists)
                        supportLoaderManager.destroyLoader(0)

                    DomainFactory.getDomainFactory(this).setTaskEndTimeStamp(this, dataId, SaveService.Source.GUI, instanceKey.mTaskKey)

                    if (!it.mExists)
                        finish()
                }
                R.id.instance_menu_select_all -> groupListFragment.selectAll()
                else -> throw UnsupportedOperationException()
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_instance)

        setSupportActionBar(toolbar)

        actionBar = supportActionBar!!

        actionBar.title = null

        if (savedInstanceState == null)
            first = true

        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY))
        instanceKey = intent.getParcelableExtra(INSTANCE_KEY)!!

        groupListFragment = supportFragmentManager.findFragmentById(R.id.show_instance_list) as? GroupListFragment ?: GroupListFragment.newInstance().also {
            supportFragmentManager.beginTransaction()
                    .add(R.id.show_instance_list, it)
                    .commit()
        }

        groupListFragment.setFab(showInstanceFab)

        supportLoaderManager.initLoader(0, null, this)

        NotificationWrapper.instance.cleanGroup(null)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowInstanceLoader(this, instanceKey)

    override fun onLoadFinished(loader: Loader<ShowInstanceLoader.Data>, data: ShowInstanceLoader.Data) {
        if (data.mInstanceData == null) {
            finish()
            return
        }

        dataId = data.DataId
        instanceData = data.mInstanceData.also {
            if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && first) {
                first = false

                DomainFactory.getDomainFactory(this).let {
                    val remoteCustomTimeFixInstanceKey = NotificationWrapperImpl.getRemoteCustomTimeFixInstanceKey(it, instanceKey)

                    it.setInstanceNotified(this, data.DataId, SaveService.Source.GUI, remoteCustomTimeFixInstanceKey)
                }
            }

            actionBar.run {
                title = it.Name
                subtitle = it.DisplayText
            }

            invalidateOptionsMenu()

            groupListFragment.setInstanceKey(instanceKey, data.DataId, it.mDataWrapper)
        }
    }

    private fun setDone(done: Boolean) {
        DomainFactory.getDomainFactory(this@ShowInstanceActivity).setInstanceDone(this, dataId, SaveService.Source.GUI, instanceKey, done)
        instanceData!!.Done = done

        invalidateOptionsMenu()
    }

    override fun onLoaderReset(loader: Loader<ShowInstanceLoader.Data>) = Unit

    override fun onCreateGroupActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyGroupActionMode() = Unit

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Assert.assertTrue(requestCode == ShowTaskActivity.REQUEST_EDIT_TASK)

        if (resultCode == Activity.RESULT_OK) {
            Assert.assertTrue(data!!.hasExtra(ShowTaskActivity.TASK_KEY_KEY))

            val taskKey = data.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!

            instanceKey = InstanceKey(taskKey, instanceKey.mScheduleKey.ScheduleDate, TimePair(instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey, instanceKey.mScheduleKey.ScheduleTimePair.mHourMinute))
        }

        supportLoaderManager.initLoader(0, null, this)
    }
}
