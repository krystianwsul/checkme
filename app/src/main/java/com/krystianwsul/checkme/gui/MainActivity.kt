package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding3.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.instances.DayFragment
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.children
import com.krystianwsul.checkme.utils.currentPosition
import com.krystianwsul.checkme.utils.pageSelections
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
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate

class MainActivity : AbstractActivity(), GroupListFragment.GroupListListener, ShowCustomTimesFragment.CustomTimesListListener, TaskListFragment.TaskListListener, DayFragment.Host {

    companion object {

        private const val VISIBLE_TAB_KEY = "visibleTab"
        private const val TIME_RANGE_KEY = "timeRange"
        private const val DEBUG_KEY = "debug"
        private const val SEARCH_KEY = "search"
        private const val CALENDAR_KEY = "calendar"
        private const val DAY_STATES_KEY = "dayStates"

        private const val NORMAL_ELEVATION = 6f
        private const val INSTANCES_ELEVATION = 0f

        fun newIntent() = Intent(MyApplication.instance, MainActivity::class.java)
    }

    private lateinit var taskListFragment: TaskListFragment
    private lateinit var projectListFragment: ProjectListFragment
    private lateinit var showCustomTimesFragment: ShowCustomTimesFragment
    private lateinit var friendListFragment: FriendListFragment

    private var onPageChangeDisposable: Disposable? = null

    private val visibleTab = BehaviorRelay.createDefault(Tab.INSTANCES)
    private val daysPosition = BehaviorRelay.create<Int>()

    override lateinit var hostEvents: Observable<DayFragment.Event>
        private set

    private var timeRange = TimeRange.DAY

    private val groupSelectAllVisible = mutableMapOf<Int, Boolean>()
    private var taskSelectAllVisible = false
    private var customTimesSelectAllVisible = false
    private var userSelectAllVisible = false
    private var projectSelectAllVisible = false

    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView

    private var debug = false

    private var calendarOpen = false
    private var calendarHeight: Int? = null
    private var calendarInitial: Boolean = true

    private lateinit var mainViewModel: MainViewModel

    lateinit var dayViewModel: DayViewModel
        private set

    private lateinit var states: MutableMap<Pair<TimeRange, Int>, Bundle>

    override fun getBottomBar() = bottomAppBar!!

    fun getState(pair: Pair<TimeRange, Int>) = states[pair]

    fun setState(pair: Pair<TimeRange, Int>, bundle: Bundle) {
        states[pair] = bundle
    }

    val selectAllRelay = PublishRelay.create<Unit>()

    override val search by lazy {
        mainActivitySearch.textChanges()
                .map { it.toString() }
                .share()!!
    }

    private var actionMode: ActionMode? = null

    private fun updateBottomMenu() {
        bottomAppBar.menu
                .findItem(R.id.action_select_all)
                ?.isVisible = when (visibleTab.value!!) { // todo bottom
            Tab.INSTANCES -> groupSelectAllVisible[mainDaysPager.currentPosition] ?: false
            Tab.TASKS -> taskSelectAllVisible
            Tab.CUSTOM_TIMES -> customTimesSelectAllVisible
            Tab.FRIENDS -> userSelectAllVisible
            Tab.PROJECTS -> projectSelectAllVisible
            Tab.DEBUG -> false
        }
    }

    private fun closeSearch() {
        mainActivitySearch.apply {
            visibility = View.GONE
            text = null
        }
        updateSearchMenu()
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

                        mainDaysPager.adapter = MyFragmentStatePagerAdapter()

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
                        R.id.actionMainClose -> closeSearch()
                        R.id.actionMainSearch -> {
                            mainActivitySearch.apply {
                                check(visibility == View.GONE)
                                visibility = View.VISIBLE
                                requestFocus()

                                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }

                            updateSearchMenu()
                        }
                        else -> throw IllegalArgumentException()
                    }
                }

                true
            }
        }

        initBottomBar()

        val toggle = ActionBarDrawerToggle(this, mainActivityDrawer, mainActivityToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mainActivityDrawer.addDrawerListener(toggle)
        toggle.syncState()

        mainActivityDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    actionMode?.finish()
            }
        })

        var debugFragment = supportFragmentManager.findFragmentById(R.id.mainDebugFrame)
        if (debugFragment != null) {
            taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
            projectListFragment = supportFragmentManager.findFragmentById(R.id.mainProjectListFrame) as ProjectListFragment
            showCustomTimesFragment = supportFragmentManager.findFragmentById(R.id.mainCustomTimesFrame) as ShowCustomTimesFragment
            friendListFragment = supportFragmentManager.findFragmentById(R.id.mainFriendListFrame) as FriendListFragment
        } else {
            debugFragment = DebugFragment.newInstance()
            taskListFragment = TaskListFragment.newInstance()
            projectListFragment = ProjectListFragment.newInstance()
            showCustomTimesFragment = ShowCustomTimesFragment.newInstance()
            friendListFragment = FriendListFragment.newInstance()

            supportFragmentManager.beginTransaction()
                    .add(R.id.mainDebugFrame, debugFragment)
                    .add(R.id.mainTaskListFrame, taskListFragment)
                    .add(R.id.mainProjectListFrame, projectListFragment)
                    .add(R.id.mainFriendListFrame, friendListFragment)
                    .add(R.id.mainCustomTimesFrame, showCustomTimesFragment)
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

        mainActivityNavigation.run {
            setCheckedItem(R.id.main_drawer_instances)

            setNavigationItemSelectedListener {
                mainActivityDrawer.run {
                    when (it.itemId) {
                        R.id.main_drawer_instances -> showTab(Tab.INSTANCES)
                        R.id.main_drawer_tasks -> showTab(Tab.TASKS)
                        R.id.main_drawer_projects -> showTab(Tab.PROJECTS)
                        R.id.main_drawer_custom_times -> showTab(Tab.CUSTOM_TIMES)
                        R.id.main_drawer_friends -> showTab(Tab.FRIENDS)
                        R.id.main_drawer_sign_out -> {
                            val domainFactory = DomainFactory.instance
                            val userInfo = MyApplication.instance.userInfo

                            domainFactory.updateUserInfo(SaveService.Source.GUI, userInfo.copy(token = null))

                            MyApplication.instance.googleSigninClient.signOut()

                            FirebaseAuth.getInstance().signOut()

                            finish()

                            startActivity(TutorialActivity.newLoginIntent())
                        }
                        R.id.main_drawer_tutorial -> startActivity(TutorialActivity.newHelpIntent())
                        R.id.main_drawer_debug -> showTab(Tab.DEBUG)
                        else -> throw IndexOutOfBoundsException()
                    }

                    closeDrawer(GravityCompat.START)
                }

                updateBottomMenu()

                true
            }

            menu.findItem(R.id.main_drawer_debug).isVisible = debug

            getHeaderView(0)!!.run {
                setOnLongClickListener {
                    debug = true

                    mainActivityNavigation.menu.findItem(R.id.main_drawer_debug).isVisible = true
                    true
                }

                headerName = navHeaderName
                headerEmail = navHeaderEmail
            }
        }

        mainCalendar.addOneShotGlobalLayoutListener {
            calendarHeight = mainCalendar.height

            updateCalendarHeight()
        }

        showTab(visibleTab.value!!)

        TickJobIntentService.startServiceRegister(this, "MainActivity: TickJobIntentService.startServiceRegister")

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

            mainDaysPager.scrollToPosition(Days.daysBetween(LocalDate.now(), date).days)

            actionMode?.finish()

            calendarOpen = false
            updateCalendarHeight()
        }

        mainViewModel = getViewModel<MainViewModel>().apply {
            start()

            createDisposable += data.subscribe { taskListFragment.setAllTasks(it.dataId, it.taskData) }
        }

        dayViewModel = getViewModel()

        FirebaseAuth.getInstance()
                .currentUser!!
                .let {
                    val displayName = it.displayName
                    check(!TextUtils.isEmpty(displayName))

                    val email = it.email
                    check(!TextUtils.isEmpty(email))

                    headerName.text = displayName
                    headerEmail.text = email
                }
    }

    override fun initBottomBar() {
        bottomAppBar.apply {
            replaceMenu(R.menu.menu_select_all)

            setOnMenuItemClickListener { item ->
                MyCrashlytics.logMethod(this, "item: " + item.title)

                when (item.itemId) {
                    R.id.action_select_all -> when (visibleTab.value!!) {
                        MainActivity.Tab.INSTANCES -> selectAllRelay.accept(Unit)
                        MainActivity.Tab.TASKS -> {
                            val taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
                            taskListFragment.treeViewAdapter.updateDisplayedNodes {
                                taskListFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        MainActivity.Tab.CUSTOM_TIMES -> {
                            val showCustomTimesFragment = supportFragmentManager.findFragmentById(R.id.mainCustomTimesFrame) as ShowCustomTimesFragment
                            showCustomTimesFragment.treeViewAdapter.updateDisplayedNodes {
                                showCustomTimesFragment.selectAll(TreeViewAdapter.Placeholder)
                            }
                        }
                        MainActivity.Tab.FRIENDS -> {
                            val friendListFragment = supportFragmentManager.findFragmentById(R.id.mainFriendListFrame) as FriendListFragment
                            friendListFragment.treeViewAdapter.updateDisplayedNodes {
                                friendListFragment.selectAll(TreeViewAdapter.Placeholder)
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

    private fun updateSearchMenu() {
        mainActivityToolbar.menu.apply {
            findItem(R.id.actionMainCalendar).isVisible = if (visibleTab.value!! == Tab.INSTANCES) {
                (timeRange == TimeRange.DAY)
            } else {
                false
            }

            if (visibleTab.value!! == Tab.TASKS) {
                val searching = mainActivitySearch.visibility == View.VISIBLE

                findItem(R.id.actionMainClose).isVisible = searching
                findItem(R.id.actionMainSearch).isVisible = !searching
            } else {
                findItem(R.id.actionMainClose).isVisible = false
                findItem(R.id.actionMainSearch).isVisible = false
            }
        }
    }

    override fun onBackPressed() {
        mainActivityDrawer.run {
            if (isDrawerOpen(GravityCompat.START))
                closeDrawer(GravityCompat.START)
            else
                super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            super.onSaveInstanceState(this)

            putSerializable(VISIBLE_TAB_KEY, visibleTab.value!!)
            putSerializable(TIME_RANGE_KEY, timeRange)
            putBoolean(DEBUG_KEY, debug)

            mainActivitySearch.run {
                if (visibility == View.VISIBLE)
                    putString(SEARCH_KEY, text.toString())
            }

            putBoolean(CALENDAR_KEY, calendarOpen)
            mainDaysPager.children.forEach { (it as DayFragment).saveState() }

            putParcelableArrayList(DAY_STATES_KEY, ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) }))
        }
    }

    private fun showTab(tab: Tab) {
        val density = resources.displayMetrics.density

        fun setVisible(visible: Boolean) = mainActivityToolbar.menu.setGroupVisible(R.id.actionMainFilter, visible)

        if (tab == Tab.INSTANCES) {
            mainDaysPager.visibility = View.VISIBLE
            ViewCompat.setElevation(mainActivityAppBarLayout, INSTANCES_ELEVATION * density)
            setVisible(true)
        } else {
            mainDaysPager.visibility = View.GONE
            ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
            setVisible(false)
            calendarOpen = false
        }

        mainTaskListFrame.visibility = if (tab == Tab.TASKS) {
            View.VISIBLE
        } else {
            closeSearch()
            View.GONE
        }

        mainProjectListFrame.visibility = if (tab == Tab.PROJECTS) View.VISIBLE else View.GONE

        if (tab == Tab.CUSTOM_TIMES) {
            mainCustomTimesFrame.visibility = View.VISIBLE
        } else {
            mainCustomTimesFrame.visibility = View.GONE
        }

        mainFriendListFrame.visibility = if (tab == Tab.FRIENDS) View.VISIBLE else View.GONE

        if (tab == Tab.DEBUG) {
            mainDebugFrame.visibility = View.VISIBLE
        } else {
            mainDebugFrame.visibility = View.GONE
        }

        mainActivityToolbar.title = when (tab) {
            MainActivity.Tab.INSTANCES -> null
            MainActivity.Tab.TASKS -> getString(R.string.tasks)
            MainActivity.Tab.PROJECTS -> getString(R.string.projects)
            MainActivity.Tab.CUSTOM_TIMES -> getString(R.string.times)
            MainActivity.Tab.FRIENDS -> getString(R.string.friends)
            MainActivity.Tab.DEBUG -> "Debug"
        }

        when (tab) {
            MainActivity.Tab.INSTANCES -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()
            }
            MainActivity.Tab.TASKS -> {
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                taskListFragment.setFab(bottomFab)
            }
            MainActivity.Tab.PROJECTS -> {
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(bottomFab)
            }
            MainActivity.Tab.CUSTOM_TIMES -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(bottomFab)
            }
            MainActivity.Tab.FRIENDS -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(bottomFab)
            }
            MainActivity.Tab.DEBUG -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                bottomFab.hide()
            }
        }

        visibleTab.accept(tab)

        updateCalendarHeight()
        updateSearchMenu()
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

    override fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean, addHourVisible: Boolean) {
        checkNotNull(position)

        groupSelectAllVisible[position] = selectAllVisible

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

    fun setUserSelectAllVisibility(selectAllVisible: Boolean) {
        userSelectAllVisible = selectAllVisible

        updateBottomMenu()
    }

    fun setProjectSelectAllVisibility(selectAllVisible: Boolean) {
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

    private inner class MyFragmentStatePagerAdapter : RecyclerView.Adapter<Holder>() {

        override fun getItemCount() = Integer.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) = Holder(DayFragment(this@MainActivity))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.dayFragment.setPosition(timeRange, position)
    }

    private class Holder(val dayFragment: DayFragment) : RecyclerView.ViewHolder(dayFragment)

    private enum class Tab {
        INSTANCES,
        TASKS,
        PROJECTS,
        CUSTOM_TIMES,
        FRIENDS,
        DEBUG
    }

    enum class TimeRange {
        DAY,
        WEEK,
        MONTH
    }

    @Parcelize
    private class ParcelableState(val timeRange: TimeRange, val position: Int, val state: Bundle) : Parcelable
}
