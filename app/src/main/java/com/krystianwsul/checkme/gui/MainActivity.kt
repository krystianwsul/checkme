package com.krystianwsul.checkme.gui

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
import android.widget.FrameLayout
import androidx.appcompat.view.ActionMode
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.extensions.setTaskEndTimeStamps
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.instances.DayFragment
import com.krystianwsul.checkme.gui.instances.list.GroupListListener
import com.krystianwsul.checkme.gui.instances.list.GroupListParameters
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import java.io.Serializable
import java.util.concurrent.TimeUnit

class MainActivity :
        AbstractActivity(),
        ShowCustomTimesFragment.CustomTimesListListener,
        TaskListFragment.Listener,
        DayFragment.Host,
        FriendListFragment.FriendListListener,
        ProjectListFragment.ProjectListListener {

    companion object {

        private const val KEY_TAB_SEARCH_STATE = "tabSearchState"
        private const val TIME_RANGE_KEY = "timeRange"
        private const val DEBUG_KEY = "debug"
        private const val SEARCH_KEY = "search"
        private const val CALENDAR_KEY = "calendar"
        private const val DAY_STATES_KEY = "dayStates"
        private const val KEY_SHOW_DELETED = "showDeleted"
        private const val KEY_DATE = "date"
        private const val KEY_SEARCH_PAGE = "searchPage"

        private const val DRAWER_TAG = "drawer"

        private const val ACTION_INSTANCES = "com.krystianwsul.checkme.INSTANCES"
        private const val ACTION_TASKS = "com.krystianwsul.checkme.TASKS"
        private const val ACTION_SEARCH = "com.krystianwsul.checkme.SEARCH"

        private const val TAG_DELETE_INSTANCES = "deleteInstances"

        fun newIntent() = Intent(MyApplication.instance, MainActivity::class.java)

        private fun Preferences.getTab() = Tab.values()[tab]
    }

    private lateinit var taskListFragment: TaskListFragment
    private lateinit var projectListFragment: ProjectListFragment
    private lateinit var showCustomTimesFragment: ShowCustomTimesFragment
    private lateinit var friendListFragment: FriendListFragment
    private lateinit var aboutFragment: AboutFragment

    private var onPageChangeDisposable: Disposable? = null

    val tabSearchStateRelay = BehaviorRelay.create<TabSearchState>()
    private val daysPosition = BehaviorRelay.create<Int>()

    override lateinit var hostEvents: Observable<DayFragment.Event>
        private set

    private var timeRange = TimeRange.DAY

    private val groupSelectAllVisible = mutableMapOf<Int, Boolean>()
    private var searchSelectAllVisible = false
    private var taskSelectAllVisible = false
    private var customTimesSelectAllVisible = false
    private var userSelectAllVisible = false
    private var projectSelectAllVisible = false

    var debug = false

    private var calendarOpen = false
    private var calendarInitial: Boolean = true

    private lateinit var mainViewModel: MainViewModel

    val dayViewModel by lazy { getViewModel<DayViewModel>() }
    private val searchInstancesViewModel by lazy { getViewModel<SearchInstancesViewModel>() }

    private lateinit var states: MutableMap<Pair<TimeRange, Int>, Bundle>

    val selectAllRelay = PublishRelay.create<Unit>()

    private var actionMode: ActionMode? = null

    private val showDeleted = BehaviorRelay.create<Boolean>()

    private val searchDataObservable by lazy {
        Observables.combineLatest(
                mainSearchText.textChanges(),
                showDeleted
        )
                .map { NullableWrapper(SearchData(it.first.toString().normalized(), it.second)) }
                .replay(1)
                .apply { createDisposable += connect() }!!
    }

    override val taskSearch by lazy {
        Observables.combineLatest(tabSearchStateRelay, searchDataObservable) { tabSearchState, searchData ->
            if ((tabSearchState as? TabSearchState.Tasks)?.isSearching == true)
                searchData
            else
                NullableWrapper()
        }
    }

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

    private val dateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = onDateSwitch()
    }

    private lateinit var date: Date

    private val mainToolbarElevation by lazy { resources.getDimension(R.dimen.mainToolbarElevation) }
    private val shortAnimTime by lazy { resources.getInteger(android.R.integer.config_shortAnimTime) }

    val daysGroupListListener = object : GroupListListener {

        override val snackbarParent get() = this@MainActivity.snackbarParent

        override val instanceSearch = Observable.just(NullableWrapper<SearchData>())

        override fun setToolbarExpanded(expanded: Boolean) = this@MainActivity.setToolbarExpanded(expanded)

        override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter<NodeHolder>) {
            onCreateActionMode(actionMode)

            check(onPageChangeDisposable == null)

            onPageChangeDisposable = mainDaysPager.pageSelections()
                    .skip(1)
                    .subscribe { actionMode.finish() }
        }

        override fun onDestroyGroupActionMode() {
            onDestroyActionMode()

            checkNotNull(onPageChangeDisposable)

            onPageChangeDisposable!!.dispose()
            onPageChangeDisposable = null
        }

        override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
            position?.let {
                groupSelectAllVisible[it] = selectAllVisible
            }

            updateBottomMenu()
        }

        override fun getBottomBar() = this@MainActivity.getBottomBar()

        override fun initBottomBar() = this@MainActivity.initBottomBar()

        override fun deleteTasks(taskKeys: Set<TaskKey>) {
            RemoveInstancesDialogFragment.newInstance(taskKeys)
                    .also { it.listener = deleteInstancesListener }
                    .show(supportFragmentManager, TAG_DELETE_INSTANCES)
        }
    }

    private var searchPage = 0

    override fun getBottomBar() = bottomAppBar!!

    fun getState(pair: Pair<TimeRange, Int>) = states[pair]

    fun setState(pair: Pair<TimeRange, Int>, bundle: Bundle) {
        states[pair] = bundle
    }

    fun updateBottomMenu() {
        val visible = when (val tabSearchState = tabSearchStateRelay.value!!) {
            is TabSearchState.Instances -> {
                if (tabSearchState.isSearching)
                    searchSelectAllVisible
                else
                    groupSelectAllVisible[mainDaysPager.currentPosition] ?: false
            }
            is TabSearchState.Tasks -> taskSelectAllVisible
            TabSearchState.Projects -> projectSelectAllVisible
            TabSearchState.CustomTimes -> customTimesSelectAllVisible
            TabSearchState.Friends -> userSelectAllVisible
            TabSearchState.Debug, TabSearchState.About -> false
        }

        bottomAppBar.menu
                .findItem(R.id.action_select_all)
                ?.isVisible = visible
    }

    private fun closeSearch() {
        mainSearchToolbar.apply {
            check(visibility == View.VISIBLE)

            animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)
        }

        mainSearchText.text = null

        hideKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mainSearchGroupListFragment.listener = object : GroupListListener {

            override val snackbarParent get() = this@MainActivity.snackbarParent

            override val instanceSearch = Observable.just(NullableWrapper<SearchData>())

            override fun setToolbarExpanded(expanded: Boolean) = this@MainActivity.setToolbarExpanded(expanded)

            override fun onCreateGroupActionMode(
                    actionMode: ActionMode,
                    treeViewAdapter: TreeViewAdapter<NodeHolder>
            ) = onCreateActionMode(actionMode)

            override fun onDestroyGroupActionMode() = onDestroyActionMode()

            override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean) {
                check(position == null)

                searchSelectAllVisible = selectAllVisible

                updateBottomMenu()
            }

            override fun getBottomBar() = this@MainActivity.getBottomBar()

            override fun initBottomBar() = this@MainActivity.initBottomBar()

            override fun deleteTasks(taskKeys: Set<TaskKey>) = throw UnsupportedOperationException()
        }

        mainDaysPager.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(mainDaysPager)

        hostEvents = Observables.combineLatest(
                tabSearchStateRelay,
                daysPosition
        ) { tabSearchState: TabSearchState, position: Int ->
            if (tabSearchState.tab == Tab.INSTANCES) {
                DayFragment.Event.PageVisible(position, bottomFab)
            } else {
                DayFragment.Event.Invisible
            }
        }
                .distinctUntilChanged()
                .replay(1)
                .apply { createDisposable += connect() }

        val overrideTabSearchState: TabSearchState?

        if (savedInstanceState != null) {
            savedInstanceState.run {
                check(containsKey(KEY_TAB_SEARCH_STATE))
                overrideTabSearchState = getParcelable(KEY_TAB_SEARCH_STATE)!!

                check(containsKey(TIME_RANGE_KEY))
                timeRange = getSerializable(TIME_RANGE_KEY) as TimeRange

                check(containsKey(DEBUG_KEY))
                debug = getBoolean(DEBUG_KEY)

                if (containsKey(SEARCH_KEY)) {
                    mainSearchToolbar.visibility = View.VISIBLE
                    mainSearchText.setText(getString(SEARCH_KEY))
                }

                calendarOpen = getBoolean(CALENDAR_KEY)

                updateCalendarDate()

                states = getParcelableArrayList<ParcelableState>(DAY_STATES_KEY)!!.associate {
                    Pair(it.timeRange, it.position) to it.state
                }.toMutableMap()

                check(containsKey(KEY_SHOW_DELETED))
                showDeleted.accept(getBoolean(KEY_SHOW_DELETED))

                date = getParcelable(KEY_DATE)!!

                searchPage = savedInstanceState.getInt(KEY_SEARCH_PAGE)
            }
        } else {
            states = mutableMapOf()

            when (intent.action) {
                ACTION_INSTANCES -> overrideTabSearchState = TabSearchState.Instances(false)
                ACTION_TASKS -> overrideTabSearchState = TabSearchState.Tasks(false)
                ACTION_SEARCH -> {
                    overrideTabSearchState = if (Preferences.getTab() == Tab.INSTANCES)
                        TabSearchState.Instances(true)
                    else
                        TabSearchState.Tasks(true)

                    mainSearchToolbar.visibility = View.VISIBLE
                    mainSearchText.apply {
                        requestFocus()

                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                this,
                                InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                }
                else -> overrideTabSearchState = null
            }

            showDeleted.accept(false)

            date = Date.today()
        }

        mainActivityToolbar.apply {
            menuInflater.inflate(R.menu.main_activity_filter, menu)

            val triples = listOf(
                    R.id.actionMainFilterDay to TimeRange.DAY,
                    R.id.actionMainFilterWeek to TimeRange.WEEK,
                    R.id.actionMainFilterMonth to TimeRange.MONTH
            ).map { Triple(it.first, it.second, menu.findItem(it.first)) }

            fun updateTimeRangeFilter() {
                triples.single { it.second == timeRange }.third.isChecked = true
            }

            updateTimeRangeFilter()

            setOnMenuItemClickListener { item ->
                val triple = triples.singleOrNull { it.first == item.itemId }
                if (triple != null) {
                    check(tabSearchStateRelay.value!!.tab == Tab.INSTANCES)

                    val newTimeRange = triple.second

                    if (newTimeRange != timeRange) {
                        timeRange = newTimeRange

                        mainTabLayout.removeAllTabs()
                        mainDaysPager.adapter = MyFragmentStatePagerAdapter()
                        mainTabLayout.selectTab(mainTabLayout.getTabAt(mainDaysPager.currentPosition))

                        groupSelectAllVisible.clear()
                        updateBottomMenu()

                        if (timeRange != TimeRange.DAY)
                            calendarOpen = false

                        updateCalendarDate()
                        updateCalendarHeight()
                        updateTimeRangeFilter()
                    }
                } else {
                    when (item.itemId) {
                        R.id.actionMainCalendar -> {
                            calendarOpen = !calendarOpen

                            updateCalendarHeight()
                        }
                        R.id.actionMainSearch -> {
                            setTabSearchState(tabSearchStateRelay.value!!.startSearch())

                            animateVisibility(listOf(mainSearchToolbar), listOf(), duration = MyBottomBar.duration)

                            mainSearchText.apply {
                                requestFocus()

                                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                        else -> throw IllegalArgumentException()
                    }
                }

                true
            }
        }

        mainSearchToolbar.apply {
            menuInflater.inflate(R.menu.main_activity_search, menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.actionSearchClose -> mainSearchText.text = null
                    R.id.actionSearchShowDeleted -> showDeleted.accept(!showDeleted.value!!)
                    else -> throw IllegalArgumentException()
                }

                true
            }

            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

            setNavigationOnClickListener { setTabSearchState(tabSearchStateRelay.value!!.closeSearch()) }

            createDisposable += showDeleted.subscribe {
                menu.findItem(R.id.actionSearchShowDeleted).isChecked = it
            }
        }

        var debugFragment = supportFragmentManager.findFragmentById(R.id.mainDebugFrame)
        if (debugFragment != null) {
            taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
            projectListFragment = supportFragmentManager.findFragmentById(R.id.mainProjectListFrame) as ProjectListFragment
            showCustomTimesFragment = supportFragmentManager.findFragmentById(R.id.mainCustomTimesFrame) as ShowCustomTimesFragment
            friendListFragment = supportFragmentManager.findFragmentById(R.id.mainFriendListFrame) as FriendListFragment
            aboutFragment = supportFragmentManager.findFragmentById(R.id.mainAboutFrame) as AboutFragment
        } else {
            debugFragment = DebugFragment.newInstance()
            taskListFragment = TaskListFragment.newInstance()
            projectListFragment = ProjectListFragment.newInstance()
            showCustomTimesFragment = ShowCustomTimesFragment.newInstance()
            friendListFragment = FriendListFragment.newInstance()
            aboutFragment = AboutFragment.newInstance()

            supportFragmentManager.beginTransaction()
                    .add(R.id.mainDebugFrame, debugFragment)
                    .add(R.id.mainTaskListFrame, taskListFragment)
                    .add(R.id.mainProjectListFrame, projectListFragment)
                    .add(R.id.mainFriendListFrame, friendListFragment)
                    .add(R.id.mainCustomTimesFrame, showCustomTimesFragment)
                    .add(R.id.mainAboutFrame, aboutFragment)
                    .commit()
        }

        mainDaysPager.run {
            pageSelections().subscribe {
                Preferences.mainTabsLog.logLineHour("pageSelections $it")

                daysPosition.accept(it)

                updateBottomMenu()

                updateCalendarDate()
            }.addTo(createDisposable)

            adapter = MyFragmentStatePagerAdapter()
        }

        mainFrame.addOneShotGlobalLayoutListener { updateCalendarHeight() }

        setTabSearchState(overrideTabSearchState ?: TabSearchState.fromTabSetting(Preferences.getTab()), true)

        initBottomBar()

        taskSearch.filter { tabSearchStateRelay.value!!.tab == Tab.TASKS }
                .subscribe {
                    if (it.value != null)
                        bottomFab.show()
                    else
                        bottomFab.hide()
                }
                .addTo(createDisposable)

        mainCalendar.minDate = DateTime.now().millis
        mainCalendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = LocalDate(year, month + 1, dayOfMonth)

            val position = Days.daysBetween(LocalDate.now(), date).days
            mainDaysPager.smoothScrollToPosition(position)

            actionMode?.finish()

            calendarOpen = false
            updateCalendarHeight()
        }

        mainViewModel = getViewModel<MainViewModel>().apply {
            start()

            createDisposable += data.subscribe {
                taskListFragment.setAllTasks(TaskListFragment.Data(
                        it.dataId,
                        it.immediate,
                        it.taskData,
                        true
                ))
            }

            if (overrideTabSearchState == null) {
                data.firstOrError()
                        .subscribeBy { setTabSearchState(TabSearchState.fromTabSetting(Tab.values()[it.defaultTab])) }
                        .addTo(createDisposable)
            }
        }

        searchInstancesViewModel.apply {
            val instanceSearch = Observables.combineLatest(
                    tabSearchStateRelay,
                    searchDataObservable
            ) { tabSearchState, searchData ->
                if ((tabSearchState as? TabSearchState.Instances)?.isSearching == true) {
                    searchData
                } else {
                    searchPage = 0
                    NullableWrapper()
                }
            }

            val searchParameters = Observables.combineLatest(
                    instanceSearch.filterNotNull()
                            .map { it.query }
                            .distinctUntilChanged(),
                    mainSearchGroupListFragment.progressShown
                            .doOnNext { searchPage += 1 }
                            .startWith(Unit)
                            .map { searchPage }
            )
                    .replay(1)
                    .apply { createDisposable += connect() }

            data.doOnNext {
                mainSearchGroupListFragment.setParameters(GroupListParameters.Search(
                        it.dataId,
                        it.immediate,
                        it.groupListDataWrapper,
                        it.showLoader
                ))
            }
                    .map { Unit }
                    .startWith(Unit)
                    .switchMap { searchParameters }
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { start(it.first, it.second) }
                    .addTo(createDisposable)
        }

        mainDaysPager.addOneShotGlobalLayoutListener {
            var state: PagerScrollState = PagerScrollState.Settled

            mainDaysPager.pageSelections()
                    .subscribe {
                        val currState = state
                        if (currState is PagerScrollState.TabTarget) {
                            if (it == currState.position)
                                state = PagerScrollState.Settled
                        } else {
                            state = PagerScrollState.PagerTarget
                            mainTabLayout.apply { selectTab(getTabAt(it)) }
                        }
                    }
                    .addTo(createDisposable)

            mainTabLayout.select(mainDaysPager.currentPosition)

            mainTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (state == PagerScrollState.PagerTarget) {
                        Preferences.mainTabsLog.logLineHour("onTabSelected ignoring")

                        state = PagerScrollState.Settled
                    } else {
                        Preferences.mainTabsLog.logLineHour("onTabSelected ${tab.position}")

                        state = PagerScrollState.TabTarget(tab.position)
                        mainDaysPager.smoothScrollToPosition(tab.position)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            })

            if (Date.today() != date)
                onDateSwitch()
        }

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_INSTANCES) as? RemoveInstancesDialogFragment)?.listener = deleteInstancesListener

        startDate(dateReceiver)
    }

    private sealed class PagerScrollState {

        object Settled : PagerScrollState()
        object PagerTarget : PagerScrollState()
        class TabTarget(val position: Int) : PagerScrollState()
    }

    override fun onStart() {
        super.onStart()

        if (tabSearchStateRelay.value!!.tab == Tab.TASKS)
            taskListFragment.checkCreatedTaskKey()
    }

    override fun initBottomBar() {
        bottomAppBar.apply {
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
                                mainSearchGroupListFragment.treeViewAdapter.selectAll()
                            else
                                selectAllRelay.accept(Unit)
                        }
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
        val itemVisibilities = when (tabSearchStateRelay.value) {
            is TabSearchState.Instances -> {
                listOf(
                        R.id.actionMainCalendar to (timeRange == TimeRange.DAY),
                        R.id.actionMainSearch to true
                )
            }
            is TabSearchState.Tasks -> listOf(
                    R.id.actionMainCalendar to false,
                    R.id.actionMainSearch to true
            )
            else -> listOf(
                    R.id.actionMainCalendar to false,
                    R.id.actionMainSearch to false
            )
        }

        mainActivityToolbar.apply {
            animateItems(itemVisibilities) {
                menu.setGroupVisible(R.id.actionMainFilter, tabSearchStateRelay.value!!.tab == Tab.INSTANCES)
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            super.onSaveInstanceState(this)

            putParcelable(KEY_TAB_SEARCH_STATE, tabSearchStateRelay.value!!)
            putSerializable(TIME_RANGE_KEY, timeRange)
            putBoolean(DEBUG_KEY, debug)

            if (tabSearchStateRelay.value!!.isSearching) putString(SEARCH_KEY, mainSearchText.text.toString())

            putBoolean(CALENDAR_KEY, calendarOpen)
            mainDaysPager.children.forEach { (it as DayFragment).saveState() }

            putParcelableArrayList(DAY_STATES_KEY, ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) }))

            putBoolean(KEY_SHOW_DELETED, showDeleted.value!!)

            putParcelable(KEY_DATE, date)

            putInt(KEY_SEARCH_PAGE, searchPage)
        }
    }

    private var elevationValueAnimator: ValueAnimator? = null

    fun setTabSearchState(tabSearchState: TabSearchState, immediate: Boolean = false) {
        val tab = tabSearchState.tab

        elevationValueAnimator?.cancel()

        val showViews = mutableListOf<View>()
        val hideViews = mutableListOf<View>()

        val currentElevation = mainActivityAppBarLayout.elevation
        val targetElevation = if (tab.elevated) mainToolbarElevation else 0f
        if (targetElevation != currentElevation) {
            elevationValueAnimator = ValueAnimator.ofFloat(currentElevation, targetElevation).apply {
                addUpdateListener {
                    ViewCompat.setElevation(mainActivityAppBarLayout, it.animatedValue as Float)
                }

                start()
            }
        }

        val currentTabLayout = when (tabSearchState) {
            is TabSearchState.Instances ->
                if (tabSearchState.isSearching) mainSearchGroupListFragment else mainDaysLayout
            is TabSearchState.Tasks -> mainTaskListFrame
            TabSearchState.Projects -> mainProjectListFrame
            TabSearchState.CustomTimes -> mainCustomTimesFrame
            TabSearchState.Friends -> mainFriendListFrame
            TabSearchState.Debug -> mainDebugFrame
            TabSearchState.About -> mainAboutFrame
        }

        showViews += currentTabLayout

        hideViews += listOf(
                mainSearchGroupListFragment,
                mainDaysLayout,
                mainTaskListFrame,
                mainProjectListFrame,
                mainCustomTimesFrame,
                mainFriendListFrame,
                mainDebugFrame,
                mainAboutFrame
        ) - currentTabLayout

        if (tab == Tab.INSTANCES) {
            hideViews += mainProgress
        } else {
            showViews += mainProgress

            calendarOpen = false
        }

        if (tab == Tab.ABOUT) aboutFragment.onShown()

        mainActivityToolbar.title = getString(tabSearchState.title)

        fun hideFab() {
            taskListFragment.clearFab()
            projectListFragment.clearFab()
            showCustomTimesFragment.clearFab()
            friendListFragment.clearFab()

            bottomFab.hide()
        }

        when (tabSearchState) {
            is TabSearchState.Instances -> {
                if (tabSearchState.isSearching) {
                    hideFab()
                } else {
                    taskListFragment.clearFab()
                    projectListFragment.clearFab()
                    showCustomTimesFragment.clearFab()
                    friendListFragment.clearFab()
                }
            }
            is TabSearchState.Tasks -> {
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                taskListFragment.setFab(bottomFab)
            }
            TabSearchState.Projects -> {
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(bottomFab)
            }
            TabSearchState.CustomTimes -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(bottomFab)
            }
            TabSearchState.Friends -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(bottomFab)
            }
            TabSearchState.Debug, TabSearchState.About -> hideFab()
        }

        val wasSearching = tabSearchStateRelay.value?.isSearching == true
        val isSearching = tabSearchState.isSearching

        tabSearchStateRelay.accept(tabSearchState)

        animateVisibility(showViews, hideViews, immediate, shortAnimTime)

        updateCalendarHeight()

        if (wasSearching && !isSearching) closeSearch()

        if (!isSearching) updateTopMenu()
    }

    override fun onDestroy() {
        onPageChangeDisposable?.dispose()
        onPageChangeDisposable = null

        unregisterReceiver(dateReceiver)

        super.onDestroy()
    }

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        taskSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun setToolbarExpanded(expanded: Boolean) = mainActivityAppBarLayout.setExpanded(expanded)

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

    override val snackbarParent get() = mainCoordinator!!

    private fun hideKeyboard() {
        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(mainCoordinator.windowToken, 0)
    }

    private fun updateCalendarHeight() {
        if (mainCalendar.height == 0) // prevent executing before global layout
            return

        val targetHeight = if (calendarOpen) mainCalendar.height else 0

        fun setHeight(height: Int) {
            mainFrame.layoutParams = (mainFrame.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = height
            }
        }

        if (calendarInitial) {
            setHeight(targetHeight)

            calendarInitial = false
        } else {
            val animation = ValueAnimator.ofInt(mainFrame.top, targetHeight)
            animation.addUpdateListener {
                val height = it.animatedValue as Int
                setHeight(height)
            }
            animation.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            animation.start()
        }
    }

    private fun updateCalendarDate() {
        if (timeRange != TimeRange.DAY)
            return

        mainCalendar.date = LocalDate.now()
                .plusDays(mainDaysPager.currentPosition)
                .toDateTimeAtStartOfDay()
                .millis
    }

    override fun onBackPressed() {
        if (mainSearchToolbar.visibility == View.VISIBLE)
            setTabSearchState(tabSearchStateRelay.value!!.closeSearch())
        else
            super.onBackPressed()
    }

    private fun onDateSwitch() {
        val today = Date.today()
        check(date != today)

        date = today

        mainDaysPager.currentPosition.let {
            if (it > 0)
                mainDaysPager.smoothScrollToPosition(it - 1)
        }

        dayViewModel.refresh()
        mainViewModel.refresh()
        searchInstancesViewModel.refresh()
    }

    private inner class MyFragmentStatePagerAdapter : RecyclerView.Adapter<Holder>() {

        override fun getItemCount() = Integer.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) = Holder(DayFragment(this@MainActivity))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val maxPosition = position + 10
            mainTabLayout.apply {
                (tabCount..maxPosition).forEach {
                    addTab(newTab().setText(DayFragment.getTitle(timeRange, it)))
                }
            }

            holder.dayFragment.setPosition(timeRange, position)
        }
    }

    private class Holder(val dayFragment: DayFragment) : RecyclerView.ViewHolder(dayFragment)

    sealed class TabSearchState : Parcelable {

        companion object {

            fun fromTabSetting(tab: Tab) = when (tab) {
                Tab.INSTANCES -> Instances(false)
                Tab.TASKS -> Tasks(false)
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
        },
        TASKS,
        PROJECTS,
        CUSTOM_TIMES,
        FRIENDS,
        DEBUG,
        ABOUT;

        open val elevated = true
    }

    enum class TimeRange {
        DAY,
        WEEK,
        MONTH
    }

    @Parcelize
    private class ParcelableState(val timeRange: TimeRange, val position: Int, val state: Bundle) : Parcelable
}
