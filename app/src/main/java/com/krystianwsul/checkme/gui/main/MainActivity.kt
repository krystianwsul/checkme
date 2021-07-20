package com.krystianwsul.checkme.gui.main

import android.animation.ValueAnimator
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.*
import com.krystianwsul.checkme.TooltipManager.subscribeShowBalloon
import com.krystianwsul.checkme.databinding.ActivityMainBinding
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.subscribeOnDomain
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.dialogs.RemoveInstancesDialogFragment
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.instances.DayFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListParameters
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.connectInstanceSearch
import com.krystianwsul.checkme.gui.utils.measureVisibleHeight
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.children
import com.krystianwsul.checkme.viewmodels.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTimeSoy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import com.mindorks.scheduler.Priority
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowOrientationRules
import com.soywiz.klock.days
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import java.io.Serializable

class MainActivity :
    AbstractActivity(),
    ShowCustomTimesFragment.CustomTimesListListener,
    DayFragment.Host,
    FriendListFragment.FriendListListener,
    ProjectListFragment.ProjectListListener {

    companion object {

        private const val KEY_TAB_SEARCH_STATE = "tabSearchState"
        private const val DEBUG_KEY = "debug"
        private const val CALENDAR_KEY = "calendar"
        private const val DAY_STATES_KEY = "dayStates"
        private const val KEY_DATE = "date"
        private const val KEY_SEARCH_PAGE = "searchPage"
        private const val KEY_SEARCHING = "searching"
        private const val KEY_MENU_DELEGATE_STATE = "menuDelegateState"

        private const val DRAWER_TAG = "drawer"

        const val ACTION_INSTANCES = "com.krystianwsul.checkme.INSTANCES"
        const val ACTION_NOTES = "com.krystianwsul.checkme.NOTES"
        const val ACTION_SEARCH = "com.krystianwsul.checkme.SEARCH"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun newIntent() = Intent(MyApplication.instance, MainActivity::class.java)

        private fun Preferences.getTab() = Tab.values()[tab]
    }

    private lateinit var noteListFragment: TaskListFragment
    private lateinit var taskListFragment: TaskListFragment
    private lateinit var projectListFragment: ProjectListFragment
    private lateinit var showCustomTimesFragment: ShowCustomTimesFragment
    private lateinit var friendListFragment: FriendListFragment
    private lateinit var aboutFragment: AboutFragment

    private var onPageChangeDisposable: Disposable? = null

    val tabSearchStateRelay = BehaviorRelay.create<TabSearchState>()!!
    private val daysPosition = BehaviorRelay.create<Int>()

    override lateinit var hostEvents: Observable<DayFragment.Event>
        private set

    private val groupSelectAllVisible = mutableMapOf<Int, Boolean>()
    private var searchSelectAllVisible = false
    private var noteSelectAllVisible = false
    private var taskSelectAllVisible = false
    private var customTimesSelectAllVisible = false
    private var userSelectAllVisible = false
    private var projectSelectAllVisible = false

    var debug = false

    private var calendarOpen = false
    private var calendarInitial: Boolean = true

    private val mainNoteViewModel by lazy { getViewModel<MainNoteViewModel>() }
    private val mainTaskViewModel by lazy { getViewModel<MainTaskViewModel>() }

    val dayViewModel by lazy { getViewModel<DayViewModel>() }
    private val searchInstancesViewModel by lazy { getViewModel<SearchInstancesViewModel>() }

    private lateinit var states: MutableMap<Pair<Preferences.TimeRange, Int>, Bundle>

    val selectAllRelay = PublishRelay.create<Unit>()!!

    private var actionMode: ActionMode? = null

    private val filterCriteriaObservable by lazy {
        binding.mainSearchInclude
            .toolbar
            .filterCriteriaObservable
    }

    private val deleteInstancesListener: (Serializable, Boolean) -> Unit = { deleteTasksData, removeInstances ->
        val (dataId, taskKeys) = deleteTasksData as DeleteTasksData

        @Suppress("UNCHECKED_CAST")
        AndroidDomainUpdater.setTaskEndTimeStamps(dataId.toFirst(), taskKeys, removeInstances)
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { showSnackbarRemovedMaybe(it.taskKeys.size).map { _ -> it } }
            .flatMapCompletable {
                // todo get dataId from current screen
                AndroidDomainUpdater.clearTaskEndTimeStamps(DomainListenerManager.NotificationType.First(dataId), it)
            }
            .subscribe()
            .addTo(createDisposable)
    }

    private val dateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = onDateSwitch()
    }

    private lateinit var date: Date

    private val mainToolbarElevation by lazy { resources.getDimension(R.dimen.mainToolbarElevation) }
    private val shortAnimTime by lazy { resources.getInteger(android.R.integer.config_shortAnimTime) }

    private sealed class TabLayoutVisibility {

        object Visible : TabLayoutVisibility()

        data class Gone(val initial: Boolean) : TabLayoutVisibility()
    }

    private val tabLayoutVisibleRelay = PublishRelay.create<TabLayoutVisibility>()

    val daysGroupListListener = object : GroupListListener {

        override val snackbarParent get() = this@MainActivity.snackbarParent

        override val instanceSearch = Preferences.showAssignedObservable.map<FilterCriteria> {
            FilterCriteria.Full(showAssignedToOthers = it)
        }

        override fun setToolbarExpanded(expanded: Boolean) = this@MainActivity.setToolbarExpanded(expanded)

        override fun onCreateGroupActionMode(
            actionMode: ActionMode,
            treeViewAdapter: TreeViewAdapter<AbstractHolder>,
            initial: Boolean,
        ) {
            onCreateActionMode(actionMode)

            check(onPageChangeDisposable == null)

            onPageChangeDisposable = binding.mainDaysPager
                .pageSelections()
                .skip(1)
                .subscribe { actionMode.finish() }

            tabLayoutVisibleRelay.accept(TabLayoutVisibility.Gone(initial))
        }

        override fun onDestroyGroupActionMode() {
            onDestroyActionMode()

            checkNotNull(onPageChangeDisposable)

            onPageChangeDisposable!!.dispose()
            onPageChangeDisposable = null

            tabLayoutVisibleRelay.accept(TabLayoutVisibility.Visible)
        }

        override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
            position?.let {
                groupSelectAllVisible[it] = selectAllVisible
            }

            updateBottomMenu()
        }

        override fun getBottomBar() = this@MainActivity.getBottomBar()

        override fun initBottomBar() = this@MainActivity.initBottomBar()

        override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) =
            this@MainActivity.deleteTasks(dataId, taskKeys)

        override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) =
            bottomFabMenuDelegate.showMenu(menuDelegate)
    }

    private var searchPage = 0

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomBinding: BottomBinding

    private lateinit var bottomFabMenuDelegate: BottomFabMenuDelegate

    override fun getBottomBar() = bottomBinding.bottomAppBar

    fun getState(pair: Pair<Preferences.TimeRange, Int>) = states[pair]

    fun setState(pair: Pair<Preferences.TimeRange, Int>, bundle: Bundle) {
        states[pair] = bundle
    }

    fun updateBottomMenu() {
        val visible = when (val tabSearchState = tabSearchStateRelay.value!!) {
            is TabSearchState.Instances -> {
                if (tabSearchState.isSearching)
                    searchSelectAllVisible
                else
                    groupSelectAllVisible[binding.mainDaysPager.currentPosition] ?: false
            }
            is TabSearchState.Notes -> noteSelectAllVisible
            is TabSearchState.Tasks -> taskSelectAllVisible
            TabSearchState.Projects -> projectSelectAllVisible
            TabSearchState.CustomTimes -> customTimesSelectAllVisible
            TabSearchState.Friends -> userSelectAllVisible
            TabSearchState.Debug, TabSearchState.About -> false
        }

        bottomBinding.bottomAppBar
            .menu
            .findItem(R.id.action_select_all)
            ?.isVisible = visible
    }

    private fun closeSearch() {
        binding.mainSearchInclude
            .toolbar
            .apply {
                check(visibility == View.VISIBLE)

                animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)

                binding.mainSearchGroupListFragment.clearExpansionStates()

                clearSearch()
            }
    }

    private fun fixedSmoothScroll(position: Int) {
        val width = binding.mainDaysPager.width

        val positionDiff = position - binding.mainDaysPager.currentPosition.coerceAtLeast(0)

        binding.mainDaysPager.smoothScrollBy(positionDiff * width, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Preferences.savedStateLog.logLineHour("savedState non-null? " + (savedInstanceState != null))

        binding = ActivityMainBinding.inflate(layoutInflater)
        bottomBinding = BottomBinding.bind(binding.root)
        setContentView(binding.root)

        bottomFabMenuDelegate = BottomFabMenuDelegate(
            bottomBinding,
            binding.mainCoordinator,
            this,
            savedInstanceState?.getParcelable(KEY_MENU_DELEGATE_STATE),
        )

        binding.mainSearchGroupListFragment.listener = object : GroupListListener {

            override val snackbarParent get() = this@MainActivity.snackbarParent

            override val instanceSearch = Observable.never<FilterCriteria>()

            override fun setToolbarExpanded(expanded: Boolean) = this@MainActivity.setToolbarExpanded(expanded)

            override fun onCreateGroupActionMode(
                actionMode: ActionMode,
                treeViewAdapter: TreeViewAdapter<AbstractHolder>,
                initial: Boolean,
            ) {
                onCreateActionMode(actionMode)

                hideKeyboard()
            }

            override fun onDestroyGroupActionMode() = onDestroyActionMode()

            override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
                check(position == null)

                searchSelectAllVisible = selectAllVisible

                updateBottomMenu()
            }

            override fun getBottomBar() = this@MainActivity.getBottomBar()

            override fun initBottomBar() = this@MainActivity.initBottomBar()

            override fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) =
                this@MainActivity.deleteTasks(dataId, taskKeys)

            override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) =
                bottomFabMenuDelegate.showMenu(menuDelegate)
        }

        binding.mainDaysPager.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        PagerSnapHelper().attachToRecyclerView(binding.mainDaysPager)

        hostEvents = Observable.combineLatest(
            tabSearchStateRelay,
            daysPosition
        ) { tabSearchState: TabSearchState, position: Int ->
            if ((tabSearchState as? TabSearchState.Instances)?.isSearching == false)
                DayFragment.Event.PageVisible(position, bottomFabMenuDelegate.fabDelegate)
            else
                DayFragment.Event.Invisible
        }
            .distinctUntilChanged()
            .replay(1)
            .apply { createDisposable += connect() }

        val overrideTabSearchState: TabSearchState?

        if (savedInstanceState != null) {
            savedInstanceState.run {
                check(containsKey(KEY_TAB_SEARCH_STATE))
                overrideTabSearchState = getParcelable(KEY_TAB_SEARCH_STATE)!!

                check(containsKey(DEBUG_KEY))
                debug = getBoolean(DEBUG_KEY)

                if (getBoolean(KEY_SEARCHING)) {
                    binding.mainSearchInclude
                        .toolbar
                        .visibility = View.VISIBLE
                }

                calendarOpen = getBoolean(CALENDAR_KEY)

                states = getParcelableArrayList<ParcelableState>(DAY_STATES_KEY)!!.associate {
                    Pair(it.timeRange, it.position) to it.state
                }.toMutableMap()

                date = getParcelable(KEY_DATE)!!

                searchPage = savedInstanceState.getInt(KEY_SEARCH_PAGE)
            }
        } else {
            states = mutableMapOf()

            overrideTabSearchState = getTabSearchStateFromIntent(intent)

            date = Date.today()
        }

        menuInflater.inflate(R.menu.main_activity_filter, binding.mainActivityToolbar.menu)
        MenuCompat.setGroupDividerEnabled(binding.mainActivityToolbar.menu, true)

        val timeRangeTriples = listOf(
            R.id.actionMainFilterDay to Preferences.TimeRange.DAY,
            R.id.actionMainFilterWeek to Preferences.TimeRange.WEEK,
            R.id.actionMainFilterMonth to Preferences.TimeRange.MONTH
        ).map { Triple(it.first, it.second, binding.mainActivityToolbar.menu.findItem(it.first)) }

        binding.mainActivityToolbar.apply {
            Preferences.showDeletedObservable
                .subscribe { menu.findItem(R.id.actionMainShowDeleted).isChecked = it }
                .addTo(createDisposable)

            Preferences.showAssignedObservable
                .subscribe { menu.findItem(R.id.actionMainAssigned).isChecked = it }
                .addTo(createDisposable)

            Preferences.showProjectsObservable
                .subscribe { menu.findItem(R.id.actionMainShowProjects).isChecked = it }
                .addTo(createDisposable)

            setOnMenuItemClickListener { item ->
                val triple = timeRangeTriples.singleOrNull { it.first == item.itemId }
                if (triple != null) {
                    check(tabSearchStateRelay.value!!.tab == Tab.INSTANCES)

                    Preferences.timeRange = triple.second
                } else {
                    when (item.itemId) {
                        R.id.actionMainCalendar -> {
                            calendarOpen = !calendarOpen

                            updateCalendarHeight()
                        }
                        R.id.actionMainSearch -> {
                            setTabSearchState(tabSearchStateRelay.value!!.startSearch())

                            animateVisibility(
                                listOf(binding.mainSearchInclude.toolbar),
                                listOf(),
                                duration = MyBottomBar.duration
                            )

                            binding.mainSearchInclude
                                .toolbar
                                .requestSearchFocus()
                        }
                        R.id.actionMainShowDeleted -> Preferences.showDeleted = !Preferences.showDeleted
                        R.id.actionMainAssigned -> Preferences.showAssigned = !Preferences.showAssigned
                        R.id.actionMainShowProjects -> Preferences.showProjects = !Preferences.showProjects
                        else -> throw IllegalArgumentException()
                    }
                }

                true
            }
        }

        binding.mainSearchInclude
            .toolbar
            .setNavigationOnClickListener { setTabSearchState(tabSearchStateRelay.value!!.closeSearch()) }

        var debugFragment = supportFragmentManager.findFragmentById(R.id.mainDebugFrame)
        if (debugFragment != null) {
            noteListFragment = supportFragmentManager.findFragmentById(R.id.mainNoteListFrame) as TaskListFragment
            taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
            projectListFragment = supportFragmentManager.findFragmentById(R.id.mainProjectListFrame) as ProjectListFragment
            showCustomTimesFragment =
                supportFragmentManager.findFragmentById(R.id.mainCustomTimesFrame) as ShowCustomTimesFragment
            friendListFragment = supportFragmentManager.findFragmentById(R.id.mainFriendListFrame) as FriendListFragment
            aboutFragment = supportFragmentManager.findFragmentById(R.id.mainAboutFrame) as AboutFragment
        } else {
            debugFragment = DebugFragment.newInstance()
            noteListFragment = TaskListFragment.newInstance()
            taskListFragment = TaskListFragment.newInstance()
            projectListFragment = ProjectListFragment.newInstance()
            showCustomTimesFragment = ShowCustomTimesFragment.newInstance()
            friendListFragment = FriendListFragment.newInstance()
            aboutFragment = AboutFragment.newInstance()

            supportFragmentManager.beginTransaction()
                .add(R.id.mainDebugFrame, debugFragment)
                .add(R.id.mainNoteListFrame, noteListFragment)
                .add(R.id.mainTaskListFrame, taskListFragment)
                .add(R.id.mainProjectListFrame, projectListFragment)
                .add(R.id.mainFriendListFrame, friendListFragment)
                .add(R.id.mainCustomTimesFrame, showCustomTimesFragment)
                .add(R.id.mainAboutFrame, aboutFragment)
                .commit()
        }

        noteListFragment.listener = newTaskListListener(
            { it is TabSearchState.Notes },
            { noteSelectAllVisible = it },
        )

        taskListFragment.listener = newTaskListListener(
            { it is TabSearchState.Tasks },
            { taskSelectAllVisible = it },
        )

        binding.mainDaysPager
            .pageSelections()
            .subscribe {
                Preferences.mainTabsLog.logLineHour("pageSelections $it")

                daysPosition.accept(it)

                updateBottomMenu()

                updateCalendarDate()
            }
            .addTo(createDisposable)

        binding.mainFrame.addOneShotGlobalLayoutListener { updateCalendarHeight() }

        setTabSearchState(
            overrideTabSearchState ?: TabSearchState.fromTabSetting(Preferences.getTab(), false),
            true,
        )

        Preferences.timeRangeObservable
            .subscribe {
                binding.mainTabLayout.removeAllTabs()
                binding.mainDaysPager.adapter = MyFragmentStatePagerAdapter()

                binding.mainTabLayout.selectTab(
                    binding.mainTabLayout.getTabAt(binding.mainDaysPager.currentPosition)
                )

                groupSelectAllVisible.clear()

                updateBottomMenu()

                if (it != Preferences.TimeRange.DAY) calendarOpen = false
                updateCalendarDate()
                updateCalendarHeight()

                timeRangeTriples.single { it.second == Preferences.timeRange }.third.isChecked = true
            }
            .addTo(createDisposable)

        initBottomBar()

        tabSearchStateRelay.filter { it is TabSearchState.Notes || it is TabSearchState.Tasks }
            .subscribe {
                bottomFabMenuDelegate.fabDelegate.apply { if (it.isSearching) show() else hide() }
            }
            .addTo(createDisposable)

        binding.mainCalendar.apply {
            minDate = DateTime.now().millis
            setOnDateChangeListener { _, year, month, dayOfMonth ->
                val date = LocalDate(year, month + 1, dayOfMonth)

                val position = Days.daysBetween(LocalDate.now(), date).days
                fixedSmoothScroll(position)

                actionMode?.finish()

                calendarOpen = false
                updateCalendarHeight()
            }
        }

        mainNoteViewModel.data
            .subscribe {
                noteListFragment.parameters = TaskListFragment.Parameters.Notes(
                    TaskListFragment.Data(mainNoteViewModel.dataId, it.immediate, it.taskData, true),
                    true,
                )
            }
            .addTo(createDisposable)

        mainTaskViewModel.apply {
            createDisposable += data.subscribe {
                taskListFragment.parameters = TaskListFragment.Parameters.All(
                    TaskListFragment.Data(dataId, it.immediate, it.taskData, true),
                    true,
                )

                fun showDrawerTooltip(type: TooltipManager.Type, @StringRes message: Int) {
                    TooltipManager.fiveSecondDelay()
                        .subscribeShowBalloon(
                            this@MainActivity,
                            type,
                            {
                                setTextResource(message)
                                setArrowOrientation(ArrowOrientation.BOTTOM)
                                setArrowPosition(0.08f)
                            },
                            { showAlignTop((bottomBinding.bottomAppBar as Toolbar).getPrivateField("mNavButtonView")) }
                        )
                        .addTo(createDisposable)
                }

                if (it.taskData.entryDatas.isNotEmpty())
                    showDrawerTooltip(TooltipManager.Type.TASKS_TAB, R.string.tooltip_tasks_tab)

                if (it.taskData.entryDatas.size > 1)
                    showDrawerTooltip(TooltipManager.Type.ADD_PROJECT, R.string.tooltip_add_project)
            }
        }

        searchInstancesViewModel.apply {
            val instanceSearch = Observable.combineLatest(
                tabSearchStateRelay,
                filterCriteriaObservable
            ) { tabSearchState, searchData ->
                if ((tabSearchState as? TabSearchState.Instances)?.isSearching == true) {
                    NullableWrapper(searchData)
                } else {
                    searchPage = 0
                    NullableWrapper()
                }
            }

            connectInstanceSearch(
                instanceSearch.filterNotNull(),
                true,
                { searchPage },
                { searchPage = it },
                binding.mainSearchGroupListFragment.progressShown,
                createDisposable,
                searchInstancesViewModel,
                {
                    binding.mainSearchGroupListFragment.setParameters(
                        GroupListParameters.Search(
                            searchInstancesViewModel.dataId,
                            it.immediate,
                            it.groupListDataWrapper,
                            it.showLoader,
                            FilterCriteria.ExpandOnly(it.searchCriteria),
                        )
                    )
                },
                this::start,
                setOf(),
            )
        }

        binding.mainDaysPager.addOneShotGlobalLayoutListener {
            var state: PagerScrollState = PagerScrollState.Settled

            binding.mainDaysPager
                .pageSelections()
                .subscribe {
                    val currState = state
                    if (currState is PagerScrollState.TabTarget) {
                        if (it == currState.position)
                            state = PagerScrollState.Settled
                    } else {
                        state = PagerScrollState.PagerTarget
                        binding.mainTabLayout.apply { selectTab(getTabAt(it)) }
                    }
                }
                .addTo(createDisposable)

            binding.mainTabLayout.select(binding.mainDaysPager.currentPosition)

            binding.mainTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (state == PagerScrollState.PagerTarget) {
                        Preferences.mainTabsLog.logLineHour("onTabSelected ignoring")

                        state = PagerScrollState.Settled
                    } else {
                        Preferences.mainTabsLog.logLineHour("onTabSelected ${tab.position}")

                        state = PagerScrollState.TabTarget(tab.position)
                        fixedSmoothScroll(tab.position)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) = onTabSelected(tab)

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            })

            if (Date.today() != date) onDateSwitch()
        }

        tryGetFragment<RemoveInstancesDialogFragment>(TAG_DELETE_INSTANCES)?.listener = deleteInstancesListener

        startDate(dateReceiver)

        Observables.combineLatest(
            tabLayoutVisibleRelay,
            binding.mainFrame
                .onGlobalLayout()
                .map { binding.mainTabLayout.measureVisibleHeight(binding.mainFrame.height) }
                .toObservable()
        )
            .subscribe { (tabLayoutVisibility, tabHeight) ->
                check(tabHeight > 0)

                binding.mainTabLayout.apply {
                    when (tabLayoutVisibility) {
                        is TabLayoutVisibility.Visible -> animateHeight(0, false)
                        is TabLayoutVisibility.Gone -> animateHeight(-tabHeight, tabLayoutVisibility.initial)
                    }
                }
            }
            .addTo(createDisposable)
    }

    private fun getTabSearchStateFromIntent(intent: Intent) = when (intent.action) {
        ACTION_INSTANCES -> TabSearchState.Instances(false)
        ACTION_NOTES -> TabSearchState.Notes(false)
        ACTION_SEARCH -> {
            binding.mainSearchInclude
                .toolbar
                .apply {
                    visibility = View.VISIBLE
                    requestSearchFocus()
                }

            TabSearchState.fromTabSetting(Preferences.getTab(), true)
        }
        else -> null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        getTabSearchStateFromIntent(intent)?.let(::setTabSearchState)
    }

    private fun newTaskListListener(
        checkTabSearchState: (TabSearchState) -> Boolean,
        storeSelectAllVisibility: (Boolean) -> Unit,
    ) = object : TaskListFragment.Listener {

        override val snackbarParent get() = this@MainActivity.snackbarParent

        override val taskSearch by lazy {
            tabSearchStateRelay.switchMap {
                if (checkTabSearchState(it)) {
                    if (it.isSearching) {
                        filterCriteriaObservable
                    } else {
                        Preferences.filterParamsObservable.map { FilterCriteria.Full(filterParams = it) }
                    }
                } else {
                    Observable.never()
                }
            }!!
        }

        override fun onCreateActionMode(actionMode: ActionMode) = this@MainActivity.onCreateActionMode(actionMode)

        override fun onDestroyActionMode() = this@MainActivity.onDestroyActionMode()

        override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
            storeSelectAllVisibility(selectAllVisible)

            updateBottomMenu()
        }

        override fun getBottomBar() = this@MainActivity.getBottomBar()

        override fun initBottomBar() = this@MainActivity.initBottomBar()

        override fun setToolbarExpanded(expanded: Boolean) = this@MainActivity.setToolbarExpanded(expanded)

        override fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate) =
            bottomFabMenuDelegate.showMenu(menuDelegate)
    }

    private data class DeleteTasksData(val dataId: DataId, val taskKeys: Set<TaskKey>) : Serializable

    private fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>) {
        RemoveInstancesDialogFragment.newInstance(DeleteTasksData(dataId, taskKeys))
            .also { it.listener = deleteInstancesListener }
            .show(supportFragmentManager, TAG_DELETE_INSTANCES)
    }

    private sealed class PagerScrollState {

        object Settled : PagerScrollState()
        object PagerTarget : PagerScrollState()
        class TabTarget(val position: Int) : PagerScrollState()
    }

    override fun onStart() {
        super.onStart()

        if (tabSearchStateRelay.value!!.tab == Tab.TASKS) taskListFragment.checkCreatedTaskKey()
        if (tabSearchStateRelay.value!!.tab == Tab.NOTES) noteListFragment.checkCreatedTaskKey()

        TooltipManager.fiveSecondDelay()
            .mapNotNull { findViewById(R.id.actionMainCalendar) }
            .subscribeShowBalloon(
                this,
                TooltipManager.Type.PRESS_MENU_TOOLTIP,
                {
                    setTextResource(R.string.tooltip_press_menu_tooltip)
                    setArrowOrientation(ArrowOrientation.TOP)
                    setArrowOrientationRules(ArrowOrientationRules.ALIGN_ANCHOR)
                },
                { showAlignBottom(it) }
            )
            .addTo(startDisposable)
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.DEBUG && !Preferences.instanceWarningSnoozeSet) {
            DomainFactory.instanceRelay
                .subscribeOnDomain(Priority.LOW)
                .filterNotNull()
                .firstOrError()
                .mapNotNull {
                    val now = ExactTimeStamp.Local.now

                    val twoDaysAgo = DateTimeSoy.now() - 2.days

                    fun ExactTimeStamp.old() = toDateTimeSoy() <= twoDaysAgo

                    it.getRootInstances(null, now.plusOne().toOffset(), now, filterVisible = false)
                        .filter { it.done != null }
                        .map {
                            listOf(it.instanceDateTime, it.scheduleDateTime).map {
                                it.toLocalExactTimeStamp()
                            }.let { list -> list + it.done!! }
                        }
                        .filter {
                            it.all { it.old() }
                        }
                        .map { it.minOrNull()!! }
                        .minOrNull()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { Toast.makeText(this, "Oldest instance: $it", Toast.LENGTH_LONG).show() }
                .addTo(resumeDisposable)
        }
    }

    override fun initBottomBar() {
        bottomBinding.bottomAppBar.apply {
            setNavigationIcon(R.drawable.ic_menu_white_24dp)
            setNavigationOnClickListener {
                actionMode?.finish()
                DrawerFragment.newInstance().show(supportFragmentManager, DRAWER_TAG)
            }

            animateReplaceMenu(R.menu.menu_select_all) { updateBottomMenu() }

            setOnMenuItemClickListener { item ->
                MyCrashlytics.logMethod(this, "item: " + item.title)

                when (item.itemId) {
                    R.id.action_select_all -> when (val tabSearchState = tabSearchStateRelay.value!!) {
                        is TabSearchState.Instances -> {
                            if (tabSearchState.isSearching)
                                binding.mainSearchGroupListFragment.treeViewAdapter.selectAll()
                            else
                                selectAllRelay.accept(Unit)
                        }
                        is TabSearchState.Notes -> noteListFragment.treeViewAdapter.selectAll()
                        is TabSearchState.Tasks ->
                            forceGetFragment<TaskListFragment>(R.id.mainTaskListFrame).treeViewAdapter.selectAll()
                        TabSearchState.CustomTimes ->
                            forceGetFragment<ShowCustomTimesFragment>(R.id.mainCustomTimesFrame).treeViewAdapter.selectAll()
                        TabSearchState.Friends ->
                            forceGetFragment<FriendListFragment>(R.id.mainFriendListFrame).treeViewAdapter.selectAll()
                        TabSearchState.Projects ->
                            forceGetFragment<ProjectListFragment>(R.id.mainProjectListFrame).treeViewAdapter.selectAll()
                        TabSearchState.Debug, TabSearchState.About -> throw UnsupportedOperationException()
                    }
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
    }

    private fun updateTopMenu() {
        val tabSearchState = tabSearchStateRelay.value!!

        val itemVisibilities = when (tabSearchState) {
            is TabSearchState.Instances -> {
                listOf(
                    R.id.actionMainCalendar to (Preferences.timeRange == Preferences.TimeRange.DAY),
                    R.id.actionMainSearch to true
                )
            }
            is TabSearchState.Notes, is TabSearchState.Tasks -> listOf(
                R.id.actionMainCalendar to false,
                R.id.actionMainSearch to true,
            )
            else -> listOf(
                R.id.actionMainCalendar to false,
                R.id.actionMainSearch to false
            )
        }

        binding.mainActivityToolbar.apply {
            animateItems(itemVisibilities) {
                menu.setGroupVisible(R.id.actionMainFilter, tabSearchStateRelay.value!!.tab == Tab.INSTANCES)
                menu.findItem(R.id.actionMainShowDeleted).isVisible = tabSearchState.tab.showDeleted
                menu.findItem(R.id.actionMainAssigned).isVisible = tabSearchState.tab.showAssignedTo
                menu.findItem(R.id.actionMainShowProjects).isVisible = tabSearchState.tab.showProjects
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            super.onSaveInstanceState(this)

            putParcelable(KEY_TAB_SEARCH_STATE, tabSearchStateRelay.value!!)
            putBoolean(DEBUG_KEY, debug)

            putBoolean(KEY_SEARCHING, tabSearchStateRelay.value!!.isSearching)

            putBoolean(CALENDAR_KEY, calendarOpen)

            binding.mainDaysPager
                .children
                .forEach { (it as DayFragment).saveState() }

            putParcelableArrayList(
                DAY_STATES_KEY,
                ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) })
            )

            putParcelable(KEY_DATE, date)

            putInt(KEY_SEARCH_PAGE, searchPage)

            putParcelable(KEY_MENU_DELEGATE_STATE, bottomFabMenuDelegate.state)
        }
    }

    private var elevationValueAnimator: ValueAnimator? = null

    fun setTabSearchState(tabSearchState: TabSearchState, immediate: Boolean = false) {
        val tab = tabSearchState.tab

        elevationValueAnimator?.cancel()

        val showViews = mutableListOf<View>()
        val hideViews = mutableListOf<View>()

        val currentElevation = binding.mainActivityAppBarLayout.elevation
        val targetElevation = if (tab.elevated) mainToolbarElevation else 0f
        if (targetElevation != currentElevation) {
            elevationValueAnimator = ValueAnimator.ofFloat(currentElevation, targetElevation).apply {
                addUpdateListener {
                    ViewCompat.setElevation(binding.mainActivityAppBarLayout, it.animatedValue as Float)
                }

                start()
            }
        }

        val currentTabLayout = when (tabSearchState) {
            is TabSearchState.Instances ->
                if (tabSearchState.isSearching) binding.mainSearchGroupListFragment else binding.mainDaysLayout
            is TabSearchState.Notes -> binding.mainNoteListFrame
            is TabSearchState.Tasks -> binding.mainTaskListFrame
            TabSearchState.Projects -> binding.mainProjectListFrame
            TabSearchState.CustomTimes -> binding.mainCustomTimesFrame
            TabSearchState.Friends -> binding.mainFriendListFrame
            TabSearchState.Debug -> binding.mainDebugFrame
            TabSearchState.About -> binding.mainAboutFrame
        }

        showViews += currentTabLayout

        binding.mainSearchInclude
            .toolbar
            .setMenuOptions(tabSearchState.tab.showDeleted, true, tabSearchState.tab.showProjects)

        hideViews += listOf(
            binding.mainSearchGroupListFragment,
            binding.mainDaysLayout,
            binding.mainNoteListFrame,
            binding.mainTaskListFrame,
            binding.mainProjectListFrame,
            binding.mainCustomTimesFrame,
            binding.mainFriendListFrame,
            binding.mainDebugFrame,
            binding.mainAboutFrame
        ) - currentTabLayout

        if (tab == Tab.INSTANCES) {
            hideViews += binding.mainProgress
        } else {
            showViews += binding.mainProgress

            calendarOpen = false
        }

        if (tab == Tab.ABOUT) aboutFragment.onShown()

        mainNoteViewModel.apply { if (tab == Tab.NOTES) start() else stop() }
        mainTaskViewModel.apply { if (tab == Tab.TASKS) start() else stop() }

        binding.mainActivityToolbar.title = getString(tabSearchState.title)

        fun hideFab() {
            binding.mainSearchGroupListFragment.clearFab()
            noteListFragment.clearFab()
            taskListFragment.clearFab()
            projectListFragment.clearFab()
            showCustomTimesFragment.clearFab()
            friendListFragment.clearFab()

            bottomFabMenuDelegate.fabDelegate.hide()
        }

        when (tabSearchState) {
            is TabSearchState.Instances -> {
                noteListFragment.clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                if (tabSearchState.isSearching) {
                    binding.mainSearchGroupListFragment.setFab(bottomFabMenuDelegate.fabDelegate)
                } else {
                    binding.mainSearchGroupListFragment.clearFab()
                }
            }
            is TabSearchState.Notes -> {
                binding.mainSearchGroupListFragment.clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                noteListFragment.setFab(bottomFabMenuDelegate.fabDelegate)
            }
            is TabSearchState.Tasks -> {
                binding.mainSearchGroupListFragment.clearFab()
                noteListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                taskListFragment.setFab(bottomFabMenuDelegate.fabDelegate)
            }
            TabSearchState.Projects -> {
                binding.mainSearchGroupListFragment.clearFab()
                noteListFragment.clearFab()
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(bottomFabMenuDelegate.fabDelegate)
            }
            TabSearchState.CustomTimes -> {
                binding.mainSearchGroupListFragment.clearFab()
                noteListFragment.clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(bottomFabMenuDelegate.fabDelegate)
            }
            TabSearchState.Friends -> {
                binding.mainSearchGroupListFragment.clearFab()
                noteListFragment.clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(bottomFabMenuDelegate.fabDelegate)
            }
            TabSearchState.Debug, TabSearchState.About -> hideFab()
        }

        val wasSearching = tabSearchStateRelay.value?.isSearching == true
        val isSearching = tabSearchState.isSearching

        tabSearchStateRelay.accept(tabSearchState)

        animateVisibility(showViews, hideViews, immediate, shortAnimTime)

        if (wasSearching && !isSearching) closeSearch()

        if (!isSearching) updateTopMenu()
    }

    override fun onDestroy() {
        onPageChangeDisposable?.dispose()
        onPageChangeDisposable = null

        unregisterReceiver(dateReceiver)

        super.onDestroy()
    }

    private fun setToolbarExpanded(expanded: Boolean) = binding.mainActivityAppBarLayout.setExpanded(expanded)

    override fun onCreateActionMode(actionMode: ActionMode) {
        check(this.actionMode == null)

        this.actionMode = actionMode
    }

    override fun onDestroyActionMode() {
        checkNotNull(actionMode)

        actionMode = null
    }

    override fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean) {
        customTimesSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun setUserSelectAllVisibility(selectAllVisible: Boolean) {
        userSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun setProjectSelectAllVisibility(selectAllVisible: Boolean) {
        projectSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override val snackbarParent get() = binding.mainCoordinator

    private fun hideKeyboard() {
        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            binding.mainCoordinator.windowToken,
            0
        )
    }

    private fun View.setTopMargin(height: Int) = updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = height }

    private fun View.animateHeight(targetHeight: Int, immediate: Boolean) {
        if (immediate) {
            setTopMargin(targetHeight)
        } else {
            ValueAnimator.ofInt(top, targetHeight).apply {
                addUpdateListener { setTopMargin(it.animatedValue as Int) }
                duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                start()
            }
        }
    }

    private fun updateCalendarHeight() {
        if (binding.mainCalendar.height == 0) return // prevent executing before global layout

        val targetHeight = if (calendarOpen) binding.mainCalendar.height else 0

        binding.mainFrame.animateHeight(targetHeight, calendarInitial)
        calendarInitial = false
    }

    private fun updateCalendarDate() {
        if (Preferences.timeRange != Preferences.TimeRange.DAY) return

        binding.mainCalendar.date = LocalDate.now()
            .plusDays(binding.mainDaysPager.currentPosition)
            .toDateTimeAtStartOfDay()
            .millis
    }

    override fun onBackPressed() {
        val tabSearchState = tabSearchStateRelay.value!!

        if (binding.mainSearchInclude.toolbar.visibility == View.VISIBLE) {
            check(tabSearchState.isSearching)

            setTabSearchState(tabSearchState.closeSearch())
        } else {
            check(!tabSearchState.isSearching)

            val defaultTab = Preferences.getTab()

            if (tabSearchState.tab != defaultTab) {
                setTabSearchState(TabSearchState.fromTabSetting(defaultTab, false))
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun onDateSwitch() {
        val today = Date.today()
        check(date != today)

        date = today

        binding.mainDaysPager
            .currentPosition
            .let { if (it > 0) fixedSmoothScroll(it - 1) }

        dayViewModel.refresh()
        mainNoteViewModel.refresh()
        mainTaskViewModel.refresh()
        searchInstancesViewModel.refresh()
    }

    private inner class MyFragmentStatePagerAdapter : RecyclerView.Adapter<Holder>() {

        override fun getItemCount() = Integer.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) =
            Holder(DayFragment(this@MainActivity))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val maxPosition = position + 10
            binding.mainTabLayout.apply {
                (tabCount..maxPosition).forEach {
                    addTab(newTab().setText(DayFragment.getTitle(Preferences.timeRange, it)))
                }
            }

            holder.dayFragment.setPosition(Preferences.timeRange, position)
        }
    }

    private class Holder(val dayFragment: DayFragment) : RecyclerView.ViewHolder(dayFragment)

    sealed class TabSearchState : Parcelable {

        companion object {

            fun fromTabSetting(tab: Tab, isSearching: Boolean) = when (tab) {
                Tab.INSTANCES -> Instances(isSearching)
                Tab.NOTES -> Notes(isSearching)
                else -> throw IllegalArgumentException()
            }
        }

        abstract val tab: Tab

        open val isSearching = false

        abstract val title: Int

        open fun startSearch(): TabSearchState = throw UnsupportedOperationException()

        open fun closeSearch(): TabSearchState = throw UnsupportedOperationException()

        @Parcelize
        data class Instances(override val isSearching: Boolean) : TabSearchState() {

            override val tab get() = Tab.INSTANCES

            override val title get() = R.string.instances

            override fun startSearch(): TabSearchState {
                check(!isSearching)

                return copy(isSearching = true)
            }

            override fun closeSearch(): TabSearchState {
                check(isSearching)

                return copy(isSearching = false)
            }
        }

        @Parcelize
        data class Notes(override val isSearching: Boolean) : TabSearchState() {

            override val tab get() = Tab.NOTES

            override val title get() = R.string.notes

            override fun startSearch(): TabSearchState {
                check(!isSearching)

                return copy(isSearching = true)
            }

            override fun closeSearch(): TabSearchState {
                check(isSearching)

                return copy(isSearching = false)
            }
        }

        @Parcelize
        data class Tasks(override val isSearching: Boolean) : TabSearchState() {

            override val tab get() = Tab.TASKS

            override val title get() = R.string.tasks

            override fun startSearch(): TabSearchState {
                check(!isSearching)

                return copy(isSearching = true)
            }

            override fun closeSearch(): TabSearchState {
                check(isSearching)

                return copy(isSearching = false)
            }
        }

        @Parcelize
        object Projects : TabSearchState() {

            override val tab get() = Tab.PROJECTS

            override val title get() = R.string.projects
        }

        @Parcelize
        object CustomTimes : TabSearchState() {

            override val tab get() = Tab.CUSTOM_TIMES

            override val title get() = R.string.times
        }

        @Parcelize
        object Friends : TabSearchState() {

            override val tab get() = Tab.FRIENDS

            override val title get() = R.string.friends
        }

        @Parcelize
        object Debug : TabSearchState() {

            override val tab get() = Tab.DEBUG

            override val title get() = R.string.debug
        }

        @Parcelize
        object About : TabSearchState() {

            override val tab get() = Tab.ABOUT

            override val title get() = R.string.about
        }
    }

    enum class Tab {
        INSTANCES {

            override val elevated = false
            override val showDeleted = false
            override val showAssignedTo = true
        },
        NOTES {

            override val showDeleted = true
            override val showAssignedTo = true
            override val showProjects = true
        },
        TASKS {

            override val showDeleted = true
            override val showAssignedTo = true
            override val showProjects = true
        },
        PROJECTS,
        CUSTOM_TIMES,
        FRIENDS,
        DEBUG,
        ABOUT;

        open val elevated = true
        open val showDeleted = false
        open val showAssignedTo = false
        open val showProjects = false
    }

    @Parcelize
    private class ParcelableState(val timeRange: Preferences.TimeRange, val position: Int, val state: Bundle) :
        Parcelable
}
