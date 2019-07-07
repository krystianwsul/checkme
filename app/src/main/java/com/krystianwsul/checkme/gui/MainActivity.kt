package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageButton
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
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.instances.DayFragment
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.checkme.viewmodels.MainViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate

class MainActivity : ToolbarActivity(), GroupListFragment.GroupListListener, ShowCustomTimesFragment.CustomTimesListListener, TaskListFragment.TaskListListener, DayFragment.Host, FriendListFragment.FriendListListener, ProjectListFragment.ProjectListListener {

    companion object {

        private const val VISIBLE_TAB_KEY = "visibleTab"
        private const val TIME_RANGE_KEY = "timeRange"
        private const val DEBUG_KEY = "debug"
        private const val SEARCH_KEY = "search"
        private const val CALENDAR_KEY = "calendar"
        private const val DAY_STATES_KEY = "dayStates"
        private const val RESTORE_INSTANCES_KEY = "restoreInstances"

        private const val NORMAL_ELEVATION = 6f
        private const val INSTANCES_ELEVATION = 0f

        private const val DRAWER_TAG = "drawer"

        private const val ACTION_INSTANCES = "com.krystianwsul.checkme.INSTANCES"
        private const val ACTION_TASKS = "com.krystianwsul.checkme.TASKS"
        private const val ACTION_SEARCH = "com.krystianwsul.checkme.SEARCH"

        fun newIntent() = Intent(MyApplication.instance, MainActivity::class.java)
    }

    private lateinit var taskListFragment: TaskListFragment
    private lateinit var projectListFragment: ProjectListFragment
    private lateinit var showCustomTimesFragment: ShowCustomTimesFragment
    private lateinit var friendListFragment: FriendListFragment
    private lateinit var aboutFragment: AboutFragment

    private var onPageChangeDisposable: Disposable? = null

    val visibleTab = BehaviorRelay.createDefault(Tab.values()[Preferences.tab])
    private val daysPosition = BehaviorRelay.create<Int>()

    override lateinit var hostEvents: Observable<DayFragment.Event>
        private set

    private var timeRange = TimeRange.DAY

    private val groupSelectAllVisible = mutableMapOf<Int, Boolean>()
    private var taskSelectAllVisible = false
    private var customTimesSelectAllVisible = false
    private var userSelectAllVisible = false
    private var projectSelectAllVisible = false

    var debug = false

    private var calendarOpen = false
    private var calendarHeight: Int? = null
    private var calendarInitial: Boolean = true

    private lateinit var mainViewModel: MainViewModel

    lateinit var dayViewModel: DayViewModel
        private set

    private lateinit var states: MutableMap<Pair<TimeRange, Int>, Bundle>

    val selectAllRelay = PublishRelay.create<Unit>()

    override val search by lazy {
        mainActivitySearch.textChanges()
                .map { it.toString() }
                .share()!!
    }

    private var actionMode: ActionMode? = null

    private var restoreInstances: Boolean? = null

    override fun getBottomBar() = bottomAppBar!!

    fun getState(pair: Pair<TimeRange, Int>) = states[pair]

    fun setState(pair: Pair<TimeRange, Int>, bundle: Bundle) {
        states[pair] = bundle
    }

    fun updateBottomMenu() {
        bottomAppBar.menu
                .findItem(R.id.action_select_all)
                ?.isVisible = when (visibleTab.value!!) {
            Tab.INSTANCES -> groupSelectAllVisible[mainDaysPager.currentPosition] ?: false
            Tab.TASKS -> taskSelectAllVisible
            Tab.CUSTOM_TIMES -> customTimesSelectAllVisible
            Tab.FRIENDS -> userSelectAllVisible
            Tab.PROJECTS -> projectSelectAllVisible
            Tab.DEBUG -> false
            Tab.ABOUT -> false
        }
    }

    private fun closeSearch(switchingTab: Boolean) {
        restoreInstances?.let {
            if (!switchingTab)
                check(visibleTab.value!! == Tab.TASKS)

            restoreInstances = null

            mainActivitySearch.apply {
                check(visibility == View.VISIBLE)

                animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)
                text = null
            }

            if (it && !switchingTab)
                showTab(Tab.INSTANCES, changingSearch = true)
        }

        updateTopMenu(true)
        hideKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mainDaysPager.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(mainDaysPager)

        hostEvents = Observables.combineLatest(visibleTab, daysPosition) { tab: Tab, position: Int ->
            if (tab == Tab.INSTANCES) {
                DayFragment.Event.PageVisible(position, bottomFab)
            } else {
                DayFragment.Event.Invisible
            }
        }
                .distinctUntilChanged()
                .replay(1)
                .apply { connect() }

        if (savedInstanceState != null) {
            savedInstanceState.run {
                check(containsKey(VISIBLE_TAB_KEY))
                visibleTab.accept(getSerializable(VISIBLE_TAB_KEY) as Tab)

                check(containsKey(TIME_RANGE_KEY))
                timeRange = getSerializable(TIME_RANGE_KEY) as TimeRange

                check(containsKey(DEBUG_KEY))
                debug = getBoolean(DEBUG_KEY)

                if (containsKey(SEARCH_KEY)) {
                    mainActivitySearch.run {
                        visibility = View.VISIBLE
                        setText(getString(SEARCH_KEY))
                    }

                    restoreInstances = getBoolean(RESTORE_INSTANCES_KEY)
                } else {
                    check(!containsKey(RESTORE_INSTANCES_KEY))
                }

                calendarOpen = getBoolean(CALENDAR_KEY)

                updateCalendarHeight()
                updateCalendarDate()

                states = getParcelableArrayList<ParcelableState>(DAY_STATES_KEY)!!.associate {
                    Pair(it.timeRange, it.position) to it.state
                }.toMutableMap()
            }
        } else {
            states = mutableMapOf()

            when (intent.action) {
                ACTION_INSTANCES -> visibleTab.accept(Tab.INSTANCES)
                ACTION_TASKS -> visibleTab.accept(Tab.TASKS)
                ACTION_SEARCH -> {
                    check(restoreInstances == null)

                    restoreInstances = false
                    visibleTab.accept(Tab.TASKS)

                    mainActivitySearch.apply {
                        visibility = View.VISIBLE
                        requestFocus()

                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }

        mainActivityToolbar.apply {
            menuInflater.inflate(R.menu.main_activity_filter, menu)

            val triples = listOf(
                    R.id.actionMainFilterDay to TimeRange.DAY,
                    R.id.actionMainFilterWeek to TimeRange.WEEK,
                    R.id.actionMainFilterMonth to TimeRange.MONTH).map { Triple(it.first, it.second, menu.findItem(it.first)) }

            fun update() {
                triples.single { it.second == timeRange }.third.isChecked = true
            }

            update()

            setOnMenuItemClickListener { item ->
                val triple = triples.singleOrNull { it.first == item.itemId }
                if (triple != null) {
                    check(visibleTab.value!! == Tab.INSTANCES)

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
                        update()
                    }
                } else {
                    when (item.itemId) {
                        R.id.actionMainCalendar -> {
                            calendarOpen = !calendarOpen

                            updateCalendarHeight()
                        }
                        R.id.actionMainClose -> mainActivitySearch.text = null
                        R.id.actionMainSearch -> {
                            check(restoreInstances == null)

                            restoreInstances = when (visibleTab.value!!) {
                                Tab.INSTANCES -> {
                                    showTab(Tab.TASKS, changingSearch = true)
                                    true
                                }
                                Tab.TASKS -> {
                                    false
                                }
                                else -> throw IllegalArgumentException()
                            }

                            mainActivitySearch.apply {
                                animateVisibility(listOf(this), listOf(), duration = MyBottomBar.duration)
                                requestFocus()

                                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

                            }

                            updateTopMenu(true)
                        }
                        else -> throw IllegalArgumentException()
                    }
                }

                true
            }

            setNavigationOnClickListener { closeSearch(false) }
        }

        initBottomBar()

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
                daysPosition.accept(it)

                updateBottomMenu()

                updateCalendarDate()
            }.addTo(createDisposable)

            adapter = MyFragmentStatePagerAdapter()
        }

        mainCalendar.addOneShotGlobalLayoutListener {
            calendarHeight = mainCalendar.height

            updateCalendarHeight()
        }

        showTab(visibleTab.value!!, true)

        search.filter { visibleTab.value == Tab.TASKS }
                .subscribe {
                    val query = it.toString().toLowerCase()
                    if (query.isEmpty())
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

            createDisposable += data.subscribe { taskListFragment.setAllTasks(TaskListFragment.Data(it.dataId, it.immediate, it.taskData)) }

            if (savedInstanceState == null) {
                data.firstOrError()
                        .subscribe { mainData ->
                            visibleTab.accept(Tab.values()[mainData.defaultTab])
                        }
                        .addTo(createDisposable)
            }
        }

        dayViewModel = getViewModel()

        mainDaysPager.addOneShotGlobalLayoutListener {
            var selectedByTab: Int? = null
            var selectedByPager = false

            mainDaysPager.pageSelections()
                    .subscribe {
                        if (selectedByTab != null) {
                            if (it == selectedByTab)
                                selectedByTab = null
                        } else {
                            selectedByPager = true
                            mainTabLayout.apply { selectTab(getTabAt(it)) }
                        }
                    }
                    .addTo(createDisposable)

            mainTabLayout.select(mainDaysPager.currentPosition)

            mainTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (selectedByPager) {
                        selectedByPager = false
                    } else {
                        selectedByTab = tab.position

                        mainDaysPager.smoothScrollToPosition(tab.position)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            })
        }

        mainActivitySearch.apply {
            addOneShotGlobalLayoutListener {
                layoutParams = layoutParams.apply { width = mainActivityToolbar.width - dpToPx(64 + 56).toInt() }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (visibleTab.value == Tab.TASKS)
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
                    R.id.action_select_all -> when (visibleTab.value!!) {
                        Tab.INSTANCES -> selectAllRelay.accept(Unit)
                        Tab.TASKS -> {
                            val taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
                            taskListFragment.treeViewAdapter.updateDisplayedNodes {
                                taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        Tab.CUSTOM_TIMES -> {
                            val showCustomTimesFragment = supportFragmentManager.findFragmentById(R.id.mainCustomTimesFrame) as ShowCustomTimesFragment
                            showCustomTimesFragment.treeViewAdapter.updateDisplayedNodes {
                                showCustomTimesFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        Tab.FRIENDS -> {
                            val friendListFragment = supportFragmentManager.findFragmentById(R.id.mainFriendListFrame) as FriendListFragment
                            friendListFragment.treeViewAdapter.updateDisplayedNodes {
                                friendListFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        Tab.PROJECTS -> {
                            val projectListFragment = supportFragmentManager.findFragmentById(R.id.mainProjectListFrame) as ProjectListFragment
                            projectListFragment.treeViewAdapter.updateDisplayedNodes {
                                projectListFragment.treeViewAdapter.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        else -> throw UnsupportedOperationException()
                    }
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
    }

    private fun updateTopMenu(changingSearch: Boolean) {
        val searching = restoreInstances != null
        if (searching)
            check(mainActivitySearch.visibility == View.VISIBLE)

        val itemVisibilities = when (visibleTab.value!!) {
            Tab.INSTANCES -> {
                check(!searching)

                listOf(
                        R.id.actionMainCalendar to (timeRange == TimeRange.DAY),
                        R.id.actionMainClose to false,
                        R.id.actionMainSearch to true
                )
            }
            Tab.TASKS -> listOf(
                    R.id.actionMainCalendar to false,
                    R.id.actionMainClose to searching,
                    R.id.actionMainSearch to !searching
            )
            else -> listOf(
                    R.id.actionMainCalendar to false,
                    R.id.actionMainClose to false,
                    R.id.actionMainSearch to false
            )
        }

        mainActivityToolbar.apply {
            animateItems(itemVisibilities, changingSearch) {
                menu.setGroupVisible(R.id.actionMainFilter, visibleTab.value!! == Tab.INSTANCES)
            }

            if (searching) {
                setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

                if (changingSearch) {
                    val button = children.filterIsInstance<AppCompatImageButton>().single()
                    button.visibility = View.INVISIBLE

                    animateVisibility2(listOf(Pair(button, HideType.INVISIBLE)), listOf(), duration = MyBottomBar.duration)
                }
            } else {
                if (changingSearch) {
                    val button = children.filterIsInstance<AppCompatImageButton>().singleOrNull()
                    if (button != null) {
                        animateVisibility2(listOf(), listOf(Pair(button, HideType.INVISIBLE)), duration = MyBottomBar.duration) {
                            navigationIcon = null
                        }
                    }
                } else {
                    navigationIcon = null
                }
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            super.onSaveInstanceState(this)

            putSerializable(VISIBLE_TAB_KEY, visibleTab.value!!)
            putSerializable(TIME_RANGE_KEY, timeRange)
            putBoolean(DEBUG_KEY, debug)

            if (restoreInstances != null) {
                putBoolean(RESTORE_INSTANCES_KEY, restoreInstances!!)
                putString(SEARCH_KEY, mainActivitySearch.text.toString())
            }

            putBoolean(CALENDAR_KEY, calendarOpen)
            mainDaysPager.children.forEach { (it as DayFragment).saveState() }

            putParcelableArrayList(DAY_STATES_KEY, ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) }))
        }
    }

    fun showTab(tab: Tab, immediate: Boolean = false, changingSearch: Boolean = false) {
        var closeSearch = false

        val density = resources.displayMetrics.density

        val showViews = mutableListOf<View>()
        val hideViews = mutableListOf<View>()

        if (tab == Tab.INSTANCES) {
            showViews.add(mainDaysLayout)
            ViewCompat.setElevation(mainActivityAppBarLayout, INSTANCES_ELEVATION * density)
        } else {
            hideViews.add(mainDaysLayout)
            ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
            calendarOpen = false
        }

        if (tab == Tab.TASKS) {
            showViews.add(mainTaskListFrame)
        } else {
            hideViews.add(mainTaskListFrame)

            if (!changingSearch)
                closeSearch = true
        }

        if (tab == Tab.PROJECTS) {
            showViews.add(mainProjectListFrame)
        } else {
            hideViews.add(mainProjectListFrame)
        }

        if (tab == Tab.CUSTOM_TIMES) {
            showViews.add(mainCustomTimesFrame)
        } else {
            hideViews.add(mainCustomTimesFrame)
        }

        if (tab == Tab.FRIENDS) {
            showViews.add(mainFriendListFrame)
        } else {
            hideViews.add(mainFriendListFrame)
        }

        if (tab == Tab.DEBUG) {
            showViews.add(mainDebugFrame)
        } else {
            hideViews.add(mainDebugFrame)
        }

        if (tab == Tab.ABOUT) {
            showViews.add(mainAboutFrame)
            aboutFragment.onShown()
        } else {
            hideViews.add(mainAboutFrame)
        }

        mainActivityToolbar.title = when (tab) {
            Tab.INSTANCES -> getString(R.string.instances)
            Tab.TASKS -> getString(R.string.tasks)
            Tab.PROJECTS -> getString(R.string.projects)
            Tab.CUSTOM_TIMES -> getString(R.string.times)
            Tab.FRIENDS -> getString(R.string.friends)
            Tab.DEBUG -> "Debug"
            Tab.ABOUT -> getString(R.string.about)
        }

        when (tab) {
            Tab.INSTANCES -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()
            }
            Tab.TASKS -> {
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                taskListFragment.setFab(bottomFab)
            }
            Tab.PROJECTS -> {
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(bottomFab)
            }
            Tab.CUSTOM_TIMES -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(bottomFab)
            }
            Tab.FRIENDS -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(bottomFab)
            }
            Tab.DEBUG, Tab.ABOUT -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                bottomFab.hide()
            }
        }

        visibleTab.accept(tab)

        animateVisibility(showViews, hideViews, immediate, resources.getInteger(android.R.integer.config_shortAnimTime))

        updateCalendarHeight()
        if (!changingSearch) {
            if (closeSearch)
                closeSearch(true)
            else
                updateTopMenu(false)
        }
    }

    override fun onDestroy() {
        onPageChangeDisposable?.dispose()
        onPageChangeDisposable = null

        super.onDestroy()
    }

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        taskSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    override fun setToolbarExpanded(expanded: Boolean) = mainActivityAppBarLayout.setExpanded(expanded)

    override fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter) {
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
        if (calendarHeight == null)
            return

        val targetHeight = if (calendarOpen) calendarHeight!! else 0

        fun setHeight(height: Int) {
            val layoutParams = mainCalendar.layoutParams
            layoutParams.height = height
            mainCalendar.layoutParams = layoutParams
        }

        if (calendarInitial) {
            setHeight(targetHeight)

            calendarInitial = false
        } else {
            val animation = ValueAnimator.ofInt(mainCalendar.height, targetHeight)
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
        if (mainActivitySearch.visibility == View.VISIBLE)
            closeSearch(false)
        else
            super.onBackPressed()
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

    enum class Tab {
        INSTANCES,
        TASKS,
        PROJECTS,
        CUSTOM_TIMES,
        FRIENDS,
        DEBUG,
        ABOUT
    }

    enum class TimeRange {
        DAY,
        WEEK,
        MONTH
    }

    @Parcelize
    private class ParcelableState(val timeRange: TimeRange, val position: Int, val state: Bundle) : Parcelable
}
