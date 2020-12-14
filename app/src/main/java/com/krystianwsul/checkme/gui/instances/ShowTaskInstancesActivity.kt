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
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.startDate
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import java.io.Serializable

class ShowTaskInstancesActivity : AbstractActivity(), GroupListListener {

    companion object {

        private const val TASK_KEY = "taskKey"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        private const val KEY_PAGE = "page"

        fun getIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskInstancesActivity::class.java).apply {
            putExtra(TASK_KEY, taskKey as Parcelable)
        }
    }

    private lateinit var taskKey: TaskKey

    private var selectAllVisible = false

    private lateinit var showTaskInstancesViewModel: ShowTaskInstancesViewModel

    override val snackbarParent get() = binding.showNotificationGroupCoordinator

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

        override fun onReceive(context: Context?, intent: Intent?) = showTaskInstancesViewModel.refresh()
    }

    private var page = 0

    override val instanceSearch = Observable.just(SearchData())

    private lateinit var binding: ActivityShowNotificationGroupBinding
    private lateinit var bottomBinding: BottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowNotificationGroupBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        binding.groupListFragment.listener = this

        check(intent.hasExtra(TASK_KEY))

        taskKey = intent.getParcelableExtra(TASK_KEY)!!

        savedInstanceState?.apply { page = getInt(KEY_PAGE) }

        showTaskInstancesViewModel = getViewModel<ShowTaskInstancesViewModel>().apply {
            data.doOnNext {
                binding.groupListFragment.setTaskKey(
                        taskKey,
                        it.dataId,
                        it.immediate,
                        it.groupListDataWrapper,
                        it.showLoader
                )
            }
                    .switchMap { binding.groupListFragment.progressShown }
                    .doOnNext { page += 1 }
                    .startWith(Unit)
                    .subscribe { start(taskKey, page) }
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

    override fun deleteTasks(taskKeys: Set<TaskKey>) {
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
}