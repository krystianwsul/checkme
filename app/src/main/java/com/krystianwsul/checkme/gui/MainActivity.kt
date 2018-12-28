package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding2.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
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

        private const val RC_SIGN_IN = 1000

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

    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView

    private val authStateListener = { firebaseAuth: FirebaseAuth -> updateSignInState(firebaseAuth.currentUser) }

    private var debug = false

    private var calendarOpen = false
    private var calendarHeight: Int? = null
    private var calendarInitial: Boolean = true

    private lateinit var mainViewModel: MainViewModel

    lateinit var dayViewModel: DayViewModel
        private set

    lateinit var states: MutableMap<Pair<TimeRange, Int>, Bundle>
        private set

    val selectAllRelay = PublishRelay.create<Unit>()

    override val search by lazy {
        mainActivitySearch.textChanges()
                .map { it.toString() }
                .share()!!
    }

    private val googleSigninClient by lazy {
        GoogleSignIn.getClient(this@MainActivity, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build())
    }

    private var actionMode: ActionMode? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_all, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) = menu.run {
        when (visibleTab.value!!) {
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
        MyCrashlytics.logMethod(this, "item: " + item.title)

        when (item.itemId) {
            R.id.action_calendar -> {
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
        Preferences.logLineDate("MainModel.onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(mainActivityToolbar)
        mainDaysPager.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(mainDaysPager)

        hostEvents = Observables.combineLatest(visibleTab, daysPosition) { tab: Tab, position: Int ->
            if (tab == Tab.INSTANCES) {
                DayFragment.Event.PageVisible(position, mainFab)
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

        mainActivitySpinner.run {
            adapter = ArrayAdapter.createFromResource(supportActionBar!!.themedContext, R.array.main_activity_spinner, R.layout.custom_toolbar_spinner).apply {
                setDropDownViewResource(R.layout.custom_toolbar_spinner_dropdown)
            }

            setSelection(timeRange.ordinal)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(visibleTab.value!! == Tab.INSTANCES)

                    check(position >= 0)
                    check(position < 3)

                    val newTimeRange = TimeRange.values()[position]

                    if (newTimeRange != timeRange) {
                        timeRange = newTimeRange

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

                invalidateOptionsMenu()

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
                        R.id.main_drawer_sign_in -> {
                            val domainFactory = DomainFactory.getInstance()
                            val userInfo = domainFactory.userInfo
                            if (userInfo != null) {
                                domainFactory.updateUserInfo(SaveService.Source.GUI, userInfo.copy(token = null))

                                googleSigninClient.signOut()

                                FirebaseAuth.getInstance().signOut()

                                if (visibleTab.value!! == Tab.FRIENDS || visibleTab.value!! == Tab.PROJECTS) {
                                    mainActivityNavigation.setCheckedItem(R.id.main_drawer_instances)
                                    showTab(Tab.INSTANCES)
                                }
                            } else {
                                startActivityForResult(googleSigninClient.signInIntent, RC_SIGN_IN)
                            }
                        }
                        R.id.main_drawer_tutorial -> startActivity(TutorialActivity.newIntent())
                        R.id.main_drawer_debug -> showTab(Tab.DEBUG)
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
            calendarHeight = mainCalendar.height

            updateCalendarHeight()
        }

        showTab(visibleTab.value!!)

        TickJobIntentService.startServiceRegister(this, "MainActivity: TickJobIntentService.startServiceRegister")

        search.filter { visibleTab.value == Tab.TASKS }
                .subscribe {
                    val query = it.toString().toLowerCase()
                    if (query.isEmpty())
                        mainFab.show()
                    else
                        mainFab.hide()
                }
                .addTo(createDisposable)

        mainCalendar.minDate = DateTime.now().millis
        mainCalendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = LocalDate(year, month + 1, dayOfMonth)

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
        Preferences.logLineHour("MainModel.onStart")
        super.onStart()

        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        Preferences.logLineHour("MainModel.onPostCreate")
        super.onPostCreate(savedInstanceState)
    }

    override fun onStop() {
        super.onStop()

        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
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

            (mainDaysPager.layoutManager!!.findViewByPosition(mainDaysPager.currentPosition) as? DayFragment)?.saveState()

            putParcelableArrayList(DAY_STATES_KEY, ArrayList(states.map { ParcelableState(it.key.first, it.key.second, it.value) }))
        }
    }

    private fun showTab(tab: Tab) {
        val density = resources.displayMetrics.density

        if (tab == Tab.INSTANCES) {
            mainDaysPager.visibility = View.VISIBLE
            ViewCompat.setElevation(mainActivityAppBarLayout, INSTANCES_ELEVATION * density)
            mainActivitySpinner.visibility = View.VISIBLE
        } else {
            mainDaysPager.visibility = View.GONE
            ViewCompat.setElevation(mainActivityAppBarLayout, NORMAL_ELEVATION * density)
            mainActivitySpinner.visibility = View.GONE
            calendarOpen = false
        }

        if (tab == Tab.TASKS) {
            mainTaskListFrame.visibility = View.VISIBLE
        } else {
            mainTaskListFrame.visibility = View.GONE
            closeSearch()
        }

        if (tab == Tab.PROJECTS) {
            checkNotNull(DomainFactory.getInstance().userInfo)

            mainProjectListFrame.visibility = View.VISIBLE
        } else {
            mainProjectListFrame.visibility = View.GONE
        }

        if (tab == Tab.CUSTOM_TIMES) {
            mainCustomTimesFrame.visibility = View.VISIBLE
        } else {
            mainCustomTimesFrame.visibility = View.GONE
        }

        if (tab == Tab.FRIENDS) {
            checkNotNull(DomainFactory.getInstance().userInfo)

            mainFriendListFrame.visibility = View.VISIBLE
        } else {
            mainFriendListFrame.visibility = View.GONE
        }

        if (tab == Tab.DEBUG) {
            mainDebugFrame.visibility = View.VISIBLE
        } else {
            mainDebugFrame.visibility = View.GONE
        }

        supportActionBar!!.title = when (tab) {
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

                taskListFragment.setFab(mainFab)
            }
            MainActivity.Tab.PROJECTS -> {
                taskListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                projectListFragment.setFab(mainFab)
            }
            MainActivity.Tab.CUSTOM_TIMES -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                friendListFragment.clearFab()

                showCustomTimesFragment.setFab(mainFab)
            }
            MainActivity.Tab.FRIENDS -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()

                friendListFragment.setFab(mainFab)
            }
            MainActivity.Tab.DEBUG -> {
                taskListFragment.clearFab()
                projectListFragment.clearFab()
                showCustomTimesFragment.clearFab()
                friendListFragment.clearFab()

                mainFab.hide()
            }
        }

        visibleTab.accept(tab)

        updateCalendarHeight()
    }

    override fun onDestroy() {
        onPageChangeDisposable?.dispose()
        onPageChangeDisposable = null

        super.onDestroy()
    }

    override fun setTaskSelectAllVisibility(selectAllVisible: Boolean) {
        taskSelectAllVisible = selectAllVisible

        invalidateOptionsMenu()
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

    override fun setGroupSelectAllVisibility(position: Int?, selectAllVisible: Boolean) {
        checkNotNull(position)

        groupSelectAllVisible[position] = selectAllVisible

        invalidateOptionsMenu()
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

        invalidateOptionsMenu()
    }

    fun setUserSelectAllVisibility(selectAllVisible: Boolean) {
        userSelectAllVisible = selectAllVisible

        invalidateOptionsMenu()
    }

    override val snackbarParent get() = mainCoordinator!!

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

            if (googleSignInResult.isSuccess) {
                val googleSignInAccount = googleSignInResult.signInAccount!!

                val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)

                FirebaseAuth.getInstance()
                        .signInWithCredential(credential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, getString(R.string.signInAs) + " " + task.result!!.user.displayName, Toast.LENGTH_SHORT).show()
                            } else {
                                val exception = task.exception!!

                                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show()

                                MyCrashlytics.logException(exception)

                                googleSigninClient.signOut()
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

    private inner class MyFragmentStatePagerAdapter : RecyclerView.Adapter<Holder>() {

        override fun getItemCount() = Integer.MAX_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) = Holder(DayFragment(this@MainActivity))

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.dayFragment.setPosition(timeRange, position)
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
