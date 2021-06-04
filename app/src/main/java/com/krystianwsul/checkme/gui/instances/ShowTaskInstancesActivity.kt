package com.krystianwsul.checkme.gui.instances

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ActivityShowNotificationGroupBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListParameters
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class ShowTaskInstancesActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_PAGE = "page"

        fun newIntent(parameters: Parameters) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, parameters)
        }
    }

    private lateinit var parameters: Parameters

    private var selectAllVisible = false

    private lateinit var showTaskInstancesViewModel: ShowTaskInstancesViewModel

    override val snackbarParent get() = binding.showNotificationGroupCoordinator

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { taskKeys, removeInstances ->
        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setTaskEndTimeStamps(
                showTaskInstancesViewModel.dataId.toFirst(),
                taskKeys as Set<TaskKey>,
                removeInstances,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapMaybe { showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
                .flatMapCompletable {
                    AndroidDomainUpdater.clearTaskEndTimeStamps(showTaskInstancesViewModel.dataId.toFirst(), it)
                }
                .subscribe()
                .addTo(createDisposable)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTaskInstancesViewModel.refresh()
    }

    private var page = 0

    override val instanceSearch = Observable.just<FilterCriteria>(FilterCriteria.None)!!

    private lateinit var binding: ActivityShowNotificationGroupBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowNotificationGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.groupListFragment.listener = this

        parameters = intent.getParcelableExtra(KEY_PARAMETERS)!!

        savedInstanceState?.apply { page = getInt(KEY_PAGE) }

        showTaskInstancesViewModel = getViewModel<ShowTaskInstancesViewModel>().apply {
            data.doOnNext {
                val immediate = it.immediate

                binding.showNotificationGroupToolbarCollapseInclude
                    .collapseAppBarLayout
                    .setText(it.title, null, binding.groupListFragment.emptyTextLayout, immediate)

                binding.groupListFragment.setParameters(
                    GroupListParameters.Parent(
                        showTaskInstancesViewModel.dataId,
                        immediate,
                        it.groupListDataWrapper,
                        it.showLoader,
                    )
                )
            }
                    .switchMap { binding.groupListFragment.progressShown }
                    .doOnNext { page += 1 }
                    .startWithItem(Unit)
                    .subscribe { start(parameters, page) }
                    .addTo(createDisposable)
        }

        initBottomBar()

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(receiver)
    }

    override fun onStart() {
        super.onStart()

        binding.groupListFragment.checkCreatedTaskKey()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_PAGE, page)
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

    override fun getBottomBar() = bottomBinding.bottomAppBar

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

    private fun updateBottomMenu() {
        bottomBinding.bottomAppBar
                .menu
                .findItem(R.id.action_select_all)
                ?.isVisible = selectAllVisible
    }

    override fun setToolbarExpanded(expanded: Boolean) = binding.showNotificationGroupToolbarCollapseInclude
            .collapseAppBarLayout
            .setExpanded(expanded)

    sealed class Parameters : Parcelable {

        abstract val projectKey: ProjectKey.Shared?

        @Parcelize
        data class Task(val taskKey: TaskKey) : Parameters() {

            override val projectKey: ProjectKey.Shared? get() = null
        }

        @Parcelize
        data class Project(override val projectKey: ProjectKey.Shared) : Parameters()
    }
}