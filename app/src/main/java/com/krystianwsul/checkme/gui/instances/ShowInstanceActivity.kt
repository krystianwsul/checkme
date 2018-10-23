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
import kotlinx.android.synthetic.main.activity_show_instance.*
import kotlinx.android.synthetic.main.toolbar.*

class ShowInstanceActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowInstanceLoader.DomainData>, GroupListFragment.GroupListListener {

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
        menu.run {
            findItem(R.id.instance_menu_check).isVisible = instanceData?.done == false
            findItem(R.id.instance_menu_uncheck).isVisible = instanceData?.done == true
            findItem(R.id.instance_menu_edit_instance).isVisible = instanceData?.run { !done && isRootInstance } == true
            findItem(R.id.instance_menu_share).isVisible = instanceData != null
            findItem(R.id.instance_menu_show_task).isVisible = instanceData?.run { !done && taskCurrent } == true
            findItem(R.id.instance_menu_edit_task).isVisible = instanceData?.run { !done && taskCurrent } == true
            findItem(R.id.instance_menu_delete_task).isVisible = instanceData?.run { !done && taskCurrent } == true
            findItem(R.id.instance_menu_select_all).isVisible = selectAllVisible
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        instanceData!!.let {
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

                    @Suppress("DEPRECATION")
                    supportLoaderManager.destroyLoader(0)

                    startActivityForResult(ShowTaskActivity.newIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_edit_task -> {
                    check(!it.done)
                    check(it.taskCurrent)

                    @Suppress("DEPRECATION")
                    supportLoaderManager.destroyLoader(0)

                    startActivityForResult(CreateTaskActivity.getEditIntent(instanceKey.taskKey), ShowTaskActivity.REQUEST_EDIT_TASK)
                }
                R.id.instance_menu_delete_task -> {
                    check(!it.done)
                    check(it.taskCurrent)

                    @Suppress("DEPRECATION")
                    if (!it.exists)
                        supportLoaderManager.destroyLoader(0)

                    DomainFactory.getDomainFactory().setTaskEndTimeStamp(this, dataId, SaveService.Source.GUI, instanceKey.taskKey)

                    if (!it.exists)
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

        check(intent.hasExtra(INSTANCE_KEY))
        instanceKey = intent.getParcelableExtra(INSTANCE_KEY)!!

        groupListFragment = supportFragmentManager.findFragmentById(R.id.show_instance_list) as? GroupListFragment ?: GroupListFragment.newInstance().also {
            supportFragmentManager.beginTransaction()
                    .add(R.id.show_instance_list, it)
                    .commit()
        }

        groupListFragment.setFab(showInstanceFab)

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this)

        NotificationWrapper.instance.cleanGroup(null)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowInstanceLoader(this, instanceKey)

    override fun onLoadFinished(loader: Loader<ShowInstanceLoader.DomainData>, data: ShowInstanceLoader.DomainData) {
        if (data.instanceData == null) {
            finish()
            return
        }

        dataId = data.dataId
        instanceData = data.instanceData.also {
            if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && first) {
                first = false

                DomainFactory.getDomainFactory().let {
                    val remoteCustomTimeFixInstanceKey = NotificationWrapperImpl.getRemoteCustomTimeFixInstanceKey(it, instanceKey)

                    it.setInstanceNotified(this, data.dataId, SaveService.Source.GUI, remoteCustomTimeFixInstanceKey)
                }
            }

            actionBar.run {
                title = it.name
                subtitle = it.displayText
            }

            invalidateOptionsMenu()

            groupListFragment.setInstanceKey(instanceKey, data.dataId, it.dataWrapper)
        }
    }

    private fun setDone(done: Boolean) {
        DomainFactory.getDomainFactory().setInstanceDone(this, dataId, SaveService.Source.GUI, instanceKey, done)
        instanceData!!.done = done

        invalidateOptionsMenu()
    }

    override fun onLoaderReset(loader: Loader<ShowInstanceLoader.DomainData>) = Unit

    override fun onCreateGroupActionMode(actionMode: ActionMode) = Unit

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
        }

        @Suppress("DEPRECATION")
        supportLoaderManager.initLoader(0, null, this)
    }
}
