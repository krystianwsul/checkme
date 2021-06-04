package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowNotificationGroupBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.kotlin.plusAssign
import java.io.Serializable
import java.util.*

class ShowNotificationGroupActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val KEY_PROJECT_KEY = "projectKey"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun getIntent(context: Context, projectKey: ProjectKey.Shared? = null) =
            Intent(context, ShowNotificationGroupActivity::class.java).putExtra(KEY_PROJECT_KEY, projectKey as? Parcelable)
    }

    private var selectAllVisible = false

    private lateinit var showNotificationGroupViewModel: ShowNotificationGroupViewModel

    override val snackbarParent get() = binding.showNotificationGroupCoordinator

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setTaskEndTimeStamps(
            showNotificationGroupViewModel.dataId.toFirst(),
            taskKeys as Set<TaskKey>,
            removeInstances,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
            .flatMapCompletable {
                AndroidDomainUpdater.clearTaskEndTimeStamps(showNotificationGroupViewModel.dataId.toFirst(), it)
            }
            .subscribe()
            .addTo(createDisposable)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showNotificationGroupViewModel.refresh()
    }

    override val instanceSearch by lazy {
        binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .filterCriteria
            .cast<FilterCriteria>()
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

        binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .apply {
                setSearchMenuOptions(false, true, false)
                configureMenu(R.menu.show_task_menu_top, R.id.actionShowTaskSearch)
            }

        updateTopMenu()
        initBottomBar()

        val projectKey = intent.getParcelableExtra<ProjectKey.Shared>(KEY_PROJECT_KEY)

        showNotificationGroupViewModel = getViewModel<ShowNotificationGroupViewModel>().apply {
            start(projectKey)

            createDisposable += data.subscribe {
                this@ShowNotificationGroupActivity.data = it

                val immediate = it.immediate

                binding.showNotificationGroupToolbarCollapseInclude
                    .collapseAppBarLayout
                    .setText(it.title, null, binding.groupListFragment.emptyTextLayout, immediate)

                binding.groupListFragment.setInstanceKeys(
                    showNotificationGroupViewModel.dataId,
                    immediate,
                    it.groupListDataWrapper,
                )

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

    override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) {
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