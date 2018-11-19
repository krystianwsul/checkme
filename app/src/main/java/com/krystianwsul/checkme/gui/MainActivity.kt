package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.v4.util.ArrayMap
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding2.widget.textChanges
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.instances.DayFragment
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.pageSelections
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.checkme.viewmodels.MainViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import java.lang.ref.WeakReference

class MainActivity : AbstractActivity(), GroupListFragment.GroupListListener, ShowCustomTimesFragment.CustomTimesListListener, TaskListFragment.TaskListListener {

    companion object {

        private const val VISIBLE_TAB_KEY = "visibleTab"
        private const val IGNORE_FIRST_KEY = "ignoreFirst"
        private const val TIME_RANGE_KEY = "timeRange"
        private const val DEBUG_KEY = "debug"
        private const val SEARCH_KEY = "search"
        private const val CALENDAR_KEY = "calendar"
        private const val DAY_STATES_KEY = "dayStates"

        private const val RC_SIGN_IN = 1000

        private const val NORMAL_ELEVATION = 6f
        private const val INSTANCES_ELEVATION = 0f

        var userInfo: UserInfo? = null
            private set

        fun newIntent() = Intent(MyApplication.instance, MainActivity::class.java)
    }

    private lateinit var taskListFragment: TaskListFragment
    private lateinit var projectListFragment: ProjectListFragment
    private lateinit var showCustomTimesFragment: ShowCustomTimesFragment
    private lateinit var friendListFragment: FriendListFragment

    private var drawerTaskListener: DrawerLayout.DrawerListener? = null
    private var drawerGroupListener: DrawerLayout.DrawerListener? = null
    private var onPageChangeDisposable: Disposable? = null
    private var drawerCustomTimesListener: DrawerLayout.DrawerListener? = null
    private var drawerUsersListener: DrawerLayout.DrawerListener? = null

    private var visibleTab = Tab.INSTANCES
    private var ignoreFirst = false

    private var timeRange = TimeRange.DAY

    private val groupSelectAllVisible = ArrayMap<Int, Boolean>()
    private var taskSelectAllVisible = false
    private var customTimesSelectAllVisible = false
    private var userSelectAllVisible = false

    private lateinit var googleApiClient: GoogleApiClient

    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView

    private lateinit var firebaseAuth: FirebaseAuth

    private var adapterDisposable: Disposable? = null

    private val mAuthStateListener = { firebaseAuth: FirebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            userInfo = UserInfo(firebaseUser)

            DomainFactory.getKotlinDomainFactory().setUserInfo(SaveService.Source.GUI, userInfo!!)

            Log.e("asdf", "firebase logged in")
        } else {
            userInfo = null

            DomainFactory.getKotlinDomainFactory().clearUserInfo()

            Log.e("asdf", "firebase logged out")
        }

        updateSignInState(firebaseUser)
    }

    private var debug = false

    private var calendarOpen = false
    private var calendarHeight: Int? = null
    private var calendarInitial: Boolean = true

    private lateinit var mainViewModel: MainViewModel

    lateinit var dayViewModel: DayViewModel
        private set

    lateinit var states: MutableMap<Pair<TimeRange, Int>, Bundle>
        private set

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_all, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) = menu.run {
        when (visibleTab) {
            Tab.INSTANCES -> {
                findItem(R.id.action_calendar).isVisible = (timeRange == TimeRange.DAY)
                findItem(R.id.action_close).isVisible = false
                findItem(R.id.action_search).isVisible = false
                findItem(R.id.action_select_all).isVisible = groupSelectAllVisible[mainDaysPager.currentPosition] ?: false
            }
            Tab.TASKS -> {
                findItem(R.id.action_calendar).isVisible = false

                val searching = mainActivitySearch.visibility == View.VISIBLE

                findItem(R.id.action_close).isVisible = searching
                findItem(R.id.action_search).isVisible = !searching
                findItem(R.id.action_select_all).isVisible = taskSelectAllVisible && !searching
            }
            Tab.CUSTOM_TIMES -> {
                findItem(R.id.action_calendar).isVisible = false
                findItem(R.id.action_close).isVisible = false
                findItem(R.id.action_search).isVisible = false
                findItem(R.id.action_select_all).isVisible = customTimesSelectAllVisible
            }
            Tab.FRIENDS -> {
                findItem(R.id.action_calendar).isVisible = false
                findItem(R.id.action_close).isVisible = false
                findItem(R.id.action_search).isVisible = false
                findItem(R.id.action_select_all).isVisible = userSelectAllVisible
            }
            else -> {
                findItem(R.id.action_calendar).isVisible = false
                findItem(R.id.action_close).isVisible = false
                findItem(R.id.action_search).isVisible = false
                findItem(R.id.action_select_all).isVisible = false
            }
        }

        true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_calendar -> {
                Log.e("asdf", "calendarOpen: $calendarOpen")

                calendarOpen = !calendarOpen

                updateCalendarHeight()
            }
            R.id.action_close -> closeSearch()
            R.id.action_search -> {
                mainActivitySearch.apply {
                    check(visibility == View.GONE)
                    visibility = View.VISIBLE // todo replace with actionMode
                    requestFocus()

                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }

                invalidateOptionsMenu()
            }
            R.id.action_select_all -> when (visibleTab) {
                MainActivity.Tab.INSTANCES -> {
                    val myFragmentStatePagerAdapter = mainDaysPager.adapter as MyFragmentStatePagerAdapter
                    myFragmentStatePagerAdapter.currentItem.selectAll()
                }
                MainActivity.Tab.TASKS -> {
                    val taskListFragment = supportFragmentManager.findFragmentById(R.id.mainTaskListFrame) as TaskListFragment
                    taskListFragment.selectAll()
                }
                MainActivity.Tab.CUSTOM_TIMES -> showCustomTimesFragment.selectAll()
                MainActivity.Tab.FRIENDS -> friendListFragment.selectAll()
                else -> throw UnsupportedOperationException()
            }
            else -> throw IllegalArgumentException()
        }

        return true
    }

    private fun closeSearch() {
        mainActivitySearch.apply {
            visibility = View.GONE
            text = null
        }
        invalidateOptionsMenu()
        hideKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainActivityToolbar)
        mainDaysPager.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        if (savedInstanceState != null) {
            savedInstanceState.run {
                check(containsKey(VISIBLE_TAB_KEY))
                visibleTab = getSerializable(VISIBLE_TAB_KEY) as Tab

                if (containsKey(IGNORE_FIRST_KEY)) {
                    check(visibleTab == Tab.INSTANCES)
                    ignoreFirst = true
                }

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

        mainActivitySpinner.run {
            adapter = ArrayAdapter.createFromResource(supportActionBar!!.themedContext, R.array.main_activity_spinner, R.layout.custom_toolbar_spinner).apply {
                setDropDownViewResource(R.layout.custom_toolbar_spinner_dropdown)
            }

            setSelection(timeRange.ordinal)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(visibleTab == Tab.INSTANCES)

                    check(position >= 0)
                    check(position < 3)

                    val newTimeRange = TimeRange.values()[position]

                    if (newTimeRange != timeRange) {
                        timeRange = newTimeRange

                        adapterDisposable?.dispose()
                        adapterDisposable = null

                        mainDaysPager.adapter = MyFragmentStatePagerAdapter()

                        groupSelectAllVisible.clear()
                        invalidateOptionsMenu()

                        if (timeRange != TimeRange.DAY)
                            calendarOpen = false

                        updateCalendarDate()
                        updateCalendarHeight()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        val toggle = ActionBarDrawerToggle(this, mainActivityDrawer, mainActivityToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mainActivityDrawer.addDrawerListener(toggle)
        toggle.syncState()

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
            addOnPageChangedListener { _, _ ->
                invalidateOptionsMenu()

                updateCalendarDate()
            }

            adapter = MyFragmentStatePagerAdapter()
        }

        mainActivityNavigation.run {
            setCheckedItem(R.id.main_drawer_instances)

            setNavigationItemSelectedListener {
                mainActivityDrawer.run {
                    when (it.itemId) {
                        R.id.main_drawer_instances -> {
                            drawerTaskListener?.let { removeDrawerListener(it) }
                            drawerTaskListener = null

                            showTab(Tab.INSTANCES)
                        }
                        R.id.main_drawer_tasks -> {
                            drawerGroupListener?.let { removeDrawerListener(it) }
                            drawerGroupListener = null

                            showTab(Tab.TASKS)
                        }
                        R.id.main_drawer_projects -> {
                            drawerTaskListener?.let { removeDrawerListener(it) }
                            drawerTaskListener = null

                            drawerGroupListener?.let { removeDrawerListener(it) }
                            drawerGroupListener = null

                            showTab(Tab.PROJECTS)
                        }
                        R.id.main_drawer_custom_times -> {
                            drawerTaskListener?.let { removeDrawerListener(it) }
                            drawerTaskListener = null

                            drawerGroupListener?.let { mainActivityDrawer.removeDrawerListener(it) }
                            drawerGroupListener = null

                            showTab(Tab.CUSTOM_TIMES)
                        }
                        R.id.main_drawer_friends -> showTab(Tab.FRIENDS)
                        R.id.main_drawer_sign_in -> if (userInfo != null) {
                            Auth.GoogleSignInApi.signOut(googleApiClient)

                            firebaseAuth.signOut()

                            if (visibleTab == Tab.FRIENDS || visibleTab == Tab.PROJECTS) {
                                mainActivityNavigation.setCheckedItem(R.id.main_drawer_instances)
                                showTab(Tab.INSTANCES)
                            }
                        } else {
                            // todo add spinner to grouplistfragment padding
                            startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), RC_SIGN_IN)
                        }
                        R.id.main_drawer_tutorial -> startActivity(TutorialActivity.newIntent())
                        R.id.main_drawer_debug -> {
                            drawerTaskListener?.let { removeDrawerListener(it) }
                            drawerTaskListener = null

                            drawerGroupListener?.let { removeDrawerListener(it) }
                            drawerGroupListener = null

                            showTab(Tab.DEBUG)
                        }
                        else -> throw IndexOutOfBoundsException()
                    }

                    closeDrawer(GravityCompat.START)
                }

                invalidateOptionsMenu()

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
            Log.e("asdf", "height: " + mainCalendar.height)

            calendarHeight = mainCalendar.height

            updateCalendarHeight()
        }

        showTab(visibleTab)

        TickJobIntentService.startServiceRegister(this, "MainActivity: TickJobIntentService.startServiceRegister")

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this) { mainActivityNavigation.menu.findItem(R.id.main_drawer_sign_in).isVisible = false }
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()

        firebaseAuth = FirebaseAuth.getInstance()

        mainActivitySearch.textChanges()
                .subscribe {
                    val query = it.toString().toLowerCase()
                    taskListFragment.search(query)

                    if (query.isEmpty())
                        mainFab.show()
                    else
                        mainFab.hide()
                }
                .addTo(createDisposable)

        mainCalendar.minDate = DateTime.now().millis
        mainCalendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = LocalDate(year, month + 1, dayOfMonth)

            Log.e("asdf", "days: " + Days.daysBetween(date, LocalDate.now()).days)

            mainDaysPager.scrollToPosition(Days.daysBetween(LocalDate.now(), date).days)

            calendarOpen = false
            updateCalendarHeight()
        }

        mainViewModel = getViewModel<MainViewModel>().apply {
            start()

            createDisposable += data.subscribe { taskListFragment.setAllTasks(it.dataId, it.taskData) }
        }

        dayViewModel = getViewModel()
    }

    override fun onStart() {
        super.onStart()

        firebaseAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onStop() {
        super.onStop()

        firebaseAuth.removeAuthStateListener(mAuthStateListener)
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

            putSerializable(VISIBLE_TAB_KEY, visibleTab)

            if (visibleTab == Tab.INSTANCES) {
                check(mainDaysPager.visibility == View.VISIBLE)
                if (mainDaysPager.currentPosition != 0 && onPageChangeDisposable != null)
                    putInt(IGNORE_FIRST_KEY, 1)
            }

            putSerializable(TIME_RANGE_KEY, timeRange)
            putBoolean(DEBUG_KEY, debug)

            mainActivitySearch.run {
                if (visibility == View.VISIBLE)
                    putString(SEARCH_KEY, text.toString())
            }

            putBoolean(CALENDAR_KEY, calendarOpen)

            (mainDaysPager.layoutManager!!.findViewByPosition(mainDaysPager.currentPosition) as DayFragment).saveState()

            putParcelableArrayList(DAY_STATES_KEY, ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) }))
        }
    }

    private fun showTab(tab: Tab) {
        val density = resources.displayMetrics.density

        when (tab) {
            MainActivity.Tab.INSTANCES -> {
                supportActionBar!!.title = null
                mainDaysPager.visibility = View.VISIBLE
                mainTaskListFrame.visibility = View.GONE
                mainProjectListFrame.visibility = View.GONE
                mainCustomTimesFrame.visibility = View.GONE
                mainDebugFrame.visibility = View.GONE
                ViewCompat.setElevation(mainActivityAppBarLayout, INSTANCES_ELEVATION * density)
                mainActivitySpinner.visibility = View.VISIBLE
                mainFriendListFrame.visibility = View.GONE

                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).setFab(mainFab)

                closeSearch()
            }
            MainActivity.Tab.TASKS -> {
                supportActionBar!!.title = getString(R.string.tasks)
                mainDaysPager.visibility = View.GONE
                mainTaskListFrame.visibility = View.VISIBLE
                mainProjectListFrame.visibility = View.GONE
                mainCustomTimesFrame.visibility = View.GONE
                mainDebugFrame.visibility = View.GONE
                ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
                mainActivitySpinner.visibility = View.GONE
                mainFriendListFrame.visibility = View.GONE

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                taskListFragment.setFab(mainFab)

                calendarOpen = false
            }
            MainActivity.Tab.PROJECTS -> {
                supportActionBar!!.title = getString(R.string.projects)
                mainDaysPager.visibility = View.GONE
                mainTaskListFrame.visibility = View.GONE
                mainProjectListFrame.visibility = View.VISIBLE
                mainCustomTimesFrame.visibility = View.GONE
                mainDebugFrame.visibility = View.GONE
                ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
                mainActivitySpinner.visibility = View.GONE
                mainFriendListFrame.visibility = View.GONE

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).clearFab()
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(mainFab)

                closeSearch()

                calendarOpen = false
            }
            MainActivity.Tab.CUSTOM_TIMES -> {
                supportActionBar!!.title = getString(R.string.times)
                mainDaysPager.visibility = View.GONE
                mainTaskListFrame.visibility = View.GONE
                mainProjectListFrame.visibility = View.GONE
                mainCustomTimesFrame.visibility = View.VISIBLE
                mainDebugFrame.visibility = View.GONE
                ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
                mainActivitySpinner.visibility = View.GONE
                mainFriendListFrame.visibility = View.GONE

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(mainFab)

                closeSearch()

                calendarOpen = false
            }
            MainActivity.Tab.FRIENDS -> {
                checkNotNull(userInfo)

                supportActionBar!!.setTitle(R.string.friends)
                mainDaysPager.visibility = View.GONE
                mainTaskListFrame.visibility = View.GONE
                mainProjectListFrame.visibility = View.GONE
                mainCustomTimesFrame.visibility = View.GONE
                mainDebugFrame.visibility = View.GONE
                ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
                mainActivitySpinner.visibility = View.GONE
                mainFriendListFrame.visibility = View.VISIBLE

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(mainFab)

                closeSearch()

                calendarOpen = false
            }
            MainActivity.Tab.DEBUG -> {
                supportActionBar!!.title = "Debug"
                mainDaysPager.visibility = View.GONE
                mainTaskListFrame.visibility = View.GONE
                mainProjectListFrame.visibility = View.GONE
                mainCustomTimesFrame.visibility = View.GONE
                mainDebugFrame.visibility = View.VISIBLE
                ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
                mainActivitySpinner.visibility = View.GONE
                mainFriendListFrame.visibility = View.GONE

                (mainDaysPager.adapter as MyFragmentStatePagerAdapter).clearFab()
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()
                mainFab.hide()

                closeSearch()

                calendarOpen = false
            }
        }

        visibleTab = tab

        updateCalendarHeight()
    }

    override fun onCreateTaskActionMode(actionMode: ActionMode) {
        check(drawerTaskListener == null)

        drawerTaskListener = object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    actionMode.finish()
            }
        }
        mainActivityDrawer.addDrawerListener(drawerTaskListener!!)
    }

    override fun onDestroy() {
        onPageChangeDisposable?.dispose()
        onPageChangeDisposable = null

        adapterDisposable?.dispose()
        adapterDisposable = null

        super.onDestroy()
    }

    override fun onDestroyTaskActionMode() {
        checkNotNull(drawerTaskListener)

        mainActivityDrawer.removeDrawerListener(drawerTaskListener!!)
        drawerTaskListener = null
    }

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        taskSelectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onCreateGroupActionMode(actionMode: ActionMode) {
        check(drawerGroupListener == null)

        drawerGroupListener = object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    actionMode.finish()
            }
        }
        mainActivityDrawer.addDrawerListener(drawerGroupListener!!)

        check(onPageChangeDisposable == null)

        onPageChangeDisposable = mainDaysPager.pageSelections()
                .skip(1)
                .subscribe {
                    if (ignoreFirst)
                        ignoreFirst = false
                    else
                        actionMode.finish()
                }
    }

    override fun onDestroyGroupActionMode() {
        checkNotNull(drawerGroupListener)
        checkNotNull(onPageChangeDisposable)

        mainActivityDrawer.removeDrawerListener(drawerGroupListener!!)
        drawerGroupListener = null

        onPageChangeDisposable!!.dispose()
        onPageChangeDisposable = null
    }

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        checkNotNull(position)

        groupSelectAllVisible[position] = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onCreateCustomTimesActionMode(actionMode: ActionMode) {
        check(drawerCustomTimesListener == null)

        drawerCustomTimesListener = object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    actionMode.finish()
            }
        }
        mainActivityDrawer.addDrawerListener(drawerCustomTimesListener!!)
    }

    override fun onDestroyCustomTimesActionMode() {
        checkNotNull(drawerCustomTimesListener)

        mainActivityDrawer.removeDrawerListener(drawerCustomTimesListener!!)
        drawerCustomTimesListener = null
    }

    override fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean) {
        customTimesSelectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    fun onCreateUserActionMode(actionMode: ActionMode) {
        check(drawerUsersListener == null)

        drawerUsersListener = object : DrawerLayout.DrawerListener {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) = Unit

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    actionMode.finish()
            }
        }
        mainActivityDrawer.addDrawerListener(drawerUsersListener!!)
    }

    fun onDestroyUserActionMode() {
        checkNotNull(drawerUsersListener)

        mainActivityDrawer.removeDrawerListener(drawerUsersListener!!)
        drawerUsersListener = null
    }

    fun setUserSelectAllVisibility(selectAllVisible: Boolean) {
        userSelectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

            if (googleSignInResult.isSuccess) {
                val googleSignInAccount = googleSignInResult.signInAccount!!

                val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)

                firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this) { task ->
                            Log.e("asdf", "signInWithCredential:onComplete:" + task.isSuccessful)

                            if (!task.isSuccessful) {
                                val exception = task.exception!!

                                Log.e("asdf", "firebase signin error: $exception")

                                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()

                                MyCrashlytics.logException(exception)

                                Auth.GoogleSignInApi.signOut(googleApiClient)
                            } else {
                                Toast.makeText(this, getString(R.string.signInAs) + " " + task.result.user.displayName, Toast.LENGTH_SHORT).show()
                            }
                        }
            } else {
                val message = "google signin error: $googleSignInResult"

                Log.e("asdf", message)

                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()

                MyCrashlytics.logException(GoogleSignInException("isSuccess: " + googleSignInResult.isSuccess + ", status: " + googleSignInResult.status))
            }
        }
    }

    private fun updateSignInState(firebaseUser: FirebaseUser?) {
        mainActivityNavigation.menu.run {
            if (firebaseUser != null) {
                val displayName = firebaseUser.displayName
                check(!TextUtils.isEmpty(displayName))

                val email = firebaseUser.email
                check(!TextUtils.isEmpty(email))

                headerName.text = displayName
                headerEmail.text = email

                findItem(R.id.main_drawer_sign_in).setTitle(R.string.signOut)
                findItem(R.id.main_drawer_projects).isEnabled = true
                findItem(R.id.main_drawer_friends).isEnabled = true
            } else {
                headerName.text = null
                headerEmail.text = null

                findItem(R.id.main_drawer_sign_in).setTitle(R.string.signIn)
                findItem(R.id.main_drawer_projects).isEnabled = false
                findItem(R.id.main_drawer_friends).isEnabled = false
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let { (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0) }
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

    private inner class MyFragmentStatePagerAdapter : RecyclerView.Adapter<Holder>(), FabUser {

        private var currentItemRef: WeakReference<DayFragment>? = null

        private var floatingActionButton: FloatingActionButton? = null

        val currentItem: DayFragment
            get() {
                checkNotNull(currentItemRef)

                return currentItemRef!!.get()!!
            }

        init {
            mainDaysPager.addOneShotGlobalLayoutListener {
                check(adapterDisposable == null)

                adapterDisposable = mainDaysPager.pageSelections().subscribe {
                    val newDayFragment = mainDaysPager.layoutManager!!.findViewByPosition(it) as DayFragment

                    currentItemRef?.let {
                        val oldDayFragment = it.get()!!

                        if (newDayFragment != oldDayFragment) {
                            oldDayFragment.run {
                                saveState()
                                clearFab()
                            }
                        } else {
                            return@subscribe
                        }
                    }

                    floatingActionButton?.let { newDayFragment.setFab(it) }

                    currentItemRef = WeakReference(newDayFragment)
                }
            }
        }

        override fun getItemCount() = Integer.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) = Holder(DayFragment(this@MainActivity))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.dayFragment.setPosition(timeRange, position)

        override fun setFab(floatingActionButton: FloatingActionButton) {
            this.floatingActionButton = floatingActionButton

            currentItemRef?.let { it.get()!!.setFab(this.floatingActionButton!!) }
        }

        override fun clearFab() {
            floatingActionButton = null

            currentItemRef?.let { currentItemRef!!.get()!!.clearFab() }
        }
    }

    private class Holder(val dayFragment: DayFragment) : RecyclerView.ViewHolder(dayFragment)

    private class GoogleSignInException(message: String) : Exception(message)

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
