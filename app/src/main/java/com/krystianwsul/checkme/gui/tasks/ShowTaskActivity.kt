package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import com.jakewharton.rxbinding3.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.MyBottomBar
import com.krystianwsul.checkme.gui.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.ShowTaskInstancesActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_task.*
import kotlinx.android.synthetic.main.bottom.*
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.toolbar_collapse.*
import java.io.Serializable

class ShowTaskActivity : AbstractActivity(), TaskListFragment.TaskListListener {

    companion object {

        const val TASK_KEY_KEY = "taskKey"

        const val REQUEST_EDIT_TASK = 1
        const val RESULT_DELETE = 65

        private const val TAG_REMOVE_INSTANCES = "removeInstances"

        private const val KEY_SEARCH_DATA = "searchData"

        fun newIntent(taskKey: TaskKey) = Intent(MyApplication.instance, ShowTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private lateinit var taskKey: TaskKey

    private var data: ShowTaskViewModel.Data? = null

    private var selectAllVisible = false

    private lateinit var taskListFragment: TaskListFragment

    private lateinit var showTaskViewModel: ShowTaskViewModel

    private val searching = BehaviorRelay.createDefault(false)
    private val showDeleted = BehaviorRelay.createDefault(false)

    override val search = searching.flatMap {
        if (it) {
            Observables.combineLatest(
                    searchToolbarText.textChanges(),
                    showDeleted
            ) { searchText, showDeleted ->
                NullableWrapper(TaskListFragment.SearchData(searchText.toString(), showDeleted))
            }
        } else {
            Observable.just(NullableWrapper())
        }
    }!!

    private val deleteInstancesListener = { taskKeys: Serializable, removeInstances: Boolean ->
        showTaskViewModel.stop()

        @Suppress("UNCHECKED_CAST")
        val taskUndoData = DomainFactory.instance.setTaskEndTimeStamps(
                SaveService.Source.GUI,
                taskKeys as Set<TaskKey>,
                removeInstances
        )

        setResult(RESULT_DELETE)

        finish()

        setSnackbar(taskUndoData)
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = showTaskViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_task)

        toolbar.apply {
            inflateMenu(R.menu.show_task_menu_top)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.actionShowTaskSearch -> {
                        searching.accept(true)

                        appBarLayout.hideText()

                        animateVisibility(listOf(searchToolbar), listOf(), duration = MyBottomBar.duration)

                        searchToolbarText.apply {
                            requestFocus()

                            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
        updateTopMenu()

        initBottomBar()

        taskKey = (savedInstanceState ?: intent.extras!!).getParcelable(TASK_KEY_KEY)!!

        savedInstanceState?.getParcelable<TaskListFragment.SearchData>(KEY_SEARCH_DATA)?.let {
            searching.accept(true)
            showDeleted.accept(it.showDeleted)

            searchToolbarText.apply {
                setText(it.query)

                requestFocus()

                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }

            searchToolbar.isVisible = true
        }

        taskListFragment = getOrInitializeFragment(R.id.showTaskFragment) {
            TaskListFragment.newInstance()
        }.also { it.setFab(bottomFab) }

        showTaskViewModel = getViewModel<ShowTaskViewModel>().apply {
            start(taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        (supportFragmentManager.findFragmentByTag(TAG_REMOVE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener

        startDate(receiver)

        searchToolbar.apply {
            inflateMenu(R.menu.main_activity_search)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.actionSearchClose -> searchToolbarText.text = null
                    R.id.actionSearchShowDeleted -> showDeleted.accept(!showDeleted.value!!)
                    else -> throw IllegalArgumentException()
                }

                true
            }

            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

            setNavigationOnClickListener { closeSearch() }

            createDisposable += showDeleted.subscribe {
                menu.findItem(R.id.actionSearchShowDeleted).isChecked = it
            }
        }
    }

    private fun closeSearch() {
        appBarLayout.showText()

        searchToolbar.apply {
            check(visibility == View.VISIBLE)

            animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)
        }

        searchToolbarText.text = null
    }

    override fun onStart() {
        super.onStart()

        taskListFragment.checkCreatedTaskKey()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EDIT_TASK) {
            if (resultCode == Activity.RESULT_OK) {
                check(data!!.hasExtra(TASK_KEY_KEY))

                taskKey = data.getParcelableExtra(TASK_KEY_KEY)!!

                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(TASK_KEY_KEY, taskKey as Parcelable)
                })
            }

            showTaskViewModel.start(taskKey)
        }
    }

    private fun onLoadFinished(data: ShowTaskViewModel.Data) {
        this.data = data

        Handler(Looper.getMainLooper()).post { // apparently included layout isn't immediately available in onCreate
            appBarLayout.setText(data.name, data.collapseText, emptyTextLayout, searching.value!!)
        }

        updateTopMenu()
        updateBottomMenu()

        taskListFragment.setTaskKey(
                TaskListFragment.RootTaskData(taskKey, data.imageData),
                TaskListFragment.Data(
                        data.dataId,
                        data.immediate,
                        data.taskData,
                        false
                )
        )
    }

    private fun updateTopMenu() {
        toolbar.menu.apply {
            findItem(R.id.actionShowTaskSearch).isVisible = data != null
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode) = Unit

    override fun onDestroyActionMode() = Unit

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        this.selectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override val snackbarParent get() = showTaskCoordinator!!

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putParcelable(TASK_KEY_KEY, taskKey)

            search.getCurrentValue()
                    ?.value
                    ?.let { putParcelable(KEY_SEARCH_DATA, it) }
        }
    }

    private fun updateBottomMenu() {
        bottomAppBar.menu.run {
            if (findItem(R.id.task_menu_edit) == null)
                return

            findItem(R.id.task_menu_edit).isVisible = data?.current == true
            findItem(R.id.task_menu_share).isVisible = data != null
            findItem(R.id.task_menu_delete).isVisible = data?.current == true
            findItem(R.id.task_menu_select_all).isVisible = selectAllVisible
            findItem(R.id.task_menu_show_instances).isVisible = data != null
            findItem(R.id.taskMenuCopyTask).isVisible = data?.current == true
            findItem(R.id.taskMenuWebSearch).isVisible = data != null
        }
    }

    override fun getBottomBar() = bottomAppBar!!

    override fun initBottomBar() {
        bottomAppBar.apply {
            animateReplaceMenu(R.menu.show_task_menu_bottom) { updateBottomMenu() }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.task_menu_edit -> {
                        showTaskViewModel.stop()

                        startActivityForResult(EditActivity.getParametersIntent(EditParameters.Edit(taskKey)), REQUEST_EDIT_TASK)
                    }
                    R.id.task_menu_share -> {
                        check(data != null)

                        Utils.share(this@ShowTaskActivity, data!!.name + taskListFragment.shareData.let { "\n" + it })
                    }
                    R.id.task_menu_delete -> {
                        RemoveInstancesDialogFragment.newInstance(setOf(taskKey))
                                .also { it.listener = deleteInstancesListener }
                                .show(supportFragmentManager, TAG_REMOVE_INSTANCES)
                    }
                    R.id.task_menu_select_all -> {
                        taskListFragment.treeViewAdapter.updateDisplayedNodes {
                            taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                        }
                    }
                    R.id.task_menu_show_instances -> startActivity(ShowTaskInstancesActivity.getIntent(taskKey))
                    R.id.taskMenuCopyTask -> startActivity(EditActivity.getParametersIntent(EditParameters.Copy(taskKey)))
                    R.id.taskMenuWebSearch -> startActivity(webSearchIntent(data!!.name))
                    else -> throw UnsupportedOperationException()
                }

                true
            }
        }
    }

    override fun setToolbarExpanded(expanded: Boolean) = appBarLayout.setExpanded(expanded)

    override fun onBackPressed() {
        if (searchToolbar.visibility == View.VISIBLE)
            closeSearch()
        else
            super.onBackPressed()
    }
}
