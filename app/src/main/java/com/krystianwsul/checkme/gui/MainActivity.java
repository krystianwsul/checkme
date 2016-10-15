package com.krystianwsul.checkme.gui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment;
import com.krystianwsul.checkme.gui.friends.FriendListFragment;
import com.krystianwsul.checkme.gui.instances.DayFragment;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.notifications.TickService;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AbstractActivity implements TaskListFragment.TaskListListener, GroupListFragment.GroupListListener, ShowCustomTimesFragment.CustomTimesListListener {
    private static final String VISIBLE_TAB_KEY = "visibleTab";
    private static final String IGNORE_FIRST_KEY = "ignoreFirst";
    private static final String TIME_RANGE_KEY = "timeRange";

    private static final int RC_SIGN_IN = 1000;

    private enum Tab {
        INSTANCES,
        TASKS,
        CUSTOM_TIMES,
        FRIENDS,
        DEBUG
    }

    private static final float NORMAL_ELEVATION = 6;
    private static final float INSTANCES_ELEVATION = 0;

    private AppBarLayout mMainActivityAppBarLayout;
    private ActionBar mActionBar;
    private Spinner mMainActivitySpinner;

    private ViewPager mDaysPager;
    private FrameLayout mMainTaskListFrame;
    private FrameLayout mMainCustomTimesFrame;
    private FriendListFragment mFriendListFragment;
    private FrameLayout mMainDebugFrame;

    private DrawerLayout mMainActivityDrawer;

    private DrawerLayout.DrawerListener mDrawerTaskListener;

    private DrawerLayout.DrawerListener mDrawerGroupListener;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    private DrawerLayout.DrawerListener mDrawerCustomTimesListener;

    private Tab mVisibleTab = Tab.INSTANCES;
    private boolean mIgnoreFirst = false;

    public enum TimeRange {
        DAY,
        WEEK,
        MONTH
    }

    private TimeRange mTimeRange = TimeRange.DAY;

    private final Map<Integer, Boolean> mGroupSelectAllVisible = new ArrayMap<>();
    private boolean mTaskSelectAllVisible = false;

    private GoogleApiClient mGoogleApiClient;

    private NavigationView mMainActivityNavigation;
    private TextView mNavHeaderName;
    private TextView mNavHeaderEmail;

    private FirebaseAuth mFirebaseAuth;
    private static UserData sUserData = null;

    private final FirebaseAuth.AuthStateListener mAuthStateListener = firebaseAuth -> {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            sUserData = new UserData(firebaseUser);

            DatabaseWrapper.setUserData(sUserData);

            DomainFactory.getDomainFactory(this).setUserData(sUserData);

            Log.e("asdf", "firebase logged in");
        } else {
            sUserData = null;

            DomainFactory.getDomainFactory(this).clearUserData();

            Log.e("asdf", "firebase logged out");
        }

        updateSignInState(firebaseUser);
    };

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_all, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mVisibleTab == Tab.INSTANCES) {
            boolean visible = false;
            if (mGroupSelectAllVisible.containsKey(mDaysPager.getCurrentItem()))
                visible = mGroupSelectAllVisible.get(mDaysPager.getCurrentItem());

            menu.findItem(R.id.action_select_all).setVisible(visible);
        } else if (mVisibleTab == Tab.TASKS) {
            menu.findItem(R.id.action_select_all).setVisible(mTaskSelectAllVisible);
        } else {
            menu.findItem(R.id.action_select_all).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Assert.assertTrue(item.getItemId() == R.id.action_select_all);

        if (mVisibleTab == Tab.INSTANCES) {
            MyFragmentStatePagerAdapter myFragmentStatePagerAdapter = (MyFragmentStatePagerAdapter) mDaysPager.getAdapter();
            Assert.assertTrue(myFragmentStatePagerAdapter != null);

            myFragmentStatePagerAdapter.getCurrentItem().selectAll();
        } else {
            Assert.assertTrue(mVisibleTab == Tab.TASKS);

            TaskListFragment taskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.main_task_list_frame);
            Assert.assertTrue(taskListFragment != null);

            taskListFragment.selectAll();
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(VISIBLE_TAB_KEY));
            mVisibleTab = (Tab) savedInstanceState.getSerializable(VISIBLE_TAB_KEY);

            if (savedInstanceState.containsKey(IGNORE_FIRST_KEY)) {
                Assert.assertTrue(mVisibleTab == Tab.INSTANCES);
                mIgnoreFirst = true;
            }

            Assert.assertTrue(savedInstanceState.containsKey(TIME_RANGE_KEY));
            mTimeRange = (TimeRange) savedInstanceState.getSerializable(TIME_RANGE_KEY);
        }

        mMainActivityAppBarLayout = (AppBarLayout) findViewById(R.id.main_activity_app_bar_layout);
        Assert.assertTrue(mMainActivityAppBarLayout != null);

        mMainActivityDrawer = (DrawerLayout) findViewById(R.id.main_activity_drawer);
        Assert.assertTrue(mMainActivityDrawer != null);

        mMainActivitySpinner = (Spinner) findViewById(R.id.main_activity_spinner);
        Assert.assertTrue(mMainActivitySpinner != null);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mActionBar.getThemedContext(), R.array.main_activity_spinner, R.layout.custom_toolbar_spinner);
        adapter.setDropDownViewResource(R.layout.custom_toolbar_spinner_dropdown);
        mMainActivitySpinner.setAdapter(adapter);

        mMainActivitySpinner.setSelection(mTimeRange.ordinal());

        mMainActivitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(mVisibleTab == Tab.INSTANCES);

                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                TimeRange newTimeRange = TimeRange.values()[position];

                if (newTimeRange != mTimeRange) {
                    mTimeRange = newTimeRange;
                    mDaysPager.setAdapter(new MyFragmentStatePagerAdapter(getSupportFragmentManager()));

                    mGroupSelectAllVisible.clear();
                    invalidateOptionsMenu();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mMainActivityDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mMainActivityDrawer.addDrawerListener(toggle);
        toggle.syncState();

        final FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment taskListFragment = fragmentManager.findFragmentById(R.id.main_task_list_frame);
        Fragment showCustomTimesFragment = fragmentManager.findFragmentById(R.id.main_custom_times_frame);
        Fragment debugFragment = fragmentManager.findFragmentById(R.id.main_debug_frame);
        Assert.assertTrue((taskListFragment == null) == (showCustomTimesFragment == null));
        Assert.assertTrue((taskListFragment == null) == (debugFragment == null));
        if (taskListFragment == null)
            fragmentManager.beginTransaction()
                    .add(R.id.main_task_list_frame, TaskListFragment.getInstance())
                    .add(R.id.main_custom_times_frame, ShowCustomTimesFragment.newInstance())
                    .add(R.id.main_debug_frame, DebugFragment.newInstance())
                    .commit();

        mFriendListFragment = (FriendListFragment) fragmentManager.findFragmentById(R.id.main_friend_list_fragment);
        Assert.assertTrue(mFriendListFragment != null);

        mDaysPager = (ViewPager) findViewById(R.id.main_pager);
        Assert.assertTrue(mDaysPager != null);

        mDaysPager.setAdapter(new MyFragmentStatePagerAdapter(getSupportFragmentManager()));

        mDaysPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mMainTaskListFrame = (FrameLayout) findViewById(R.id.main_task_list_frame);
        Assert.assertTrue(mMainTaskListFrame != null);

        mMainCustomTimesFrame = (FrameLayout) findViewById(R.id.main_custom_times_frame);
        Assert.assertTrue(mMainCustomTimesFrame != null);

        mMainDebugFrame = (FrameLayout) findViewById(R.id.main_debug_frame);
        Assert.assertTrue(mMainDebugFrame != null);

        mMainActivityNavigation = (NavigationView) findViewById(R.id.main_activity_navigation);
        Assert.assertTrue(mMainActivityNavigation != null);

        mMainActivityNavigation.setCheckedItem(R.id.main_drawer_instances);

        mMainActivityNavigation.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.main_drawer_instances:
                    if (mDrawerTaskListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerTaskListener);
                        mDrawerTaskListener = null;
                    }

                    showTab(Tab.INSTANCES);

                    break;
                case R.id.main_drawer_tasks:
                    if (mDrawerGroupListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerGroupListener);
                        mDrawerGroupListener = null;
                    }

                    showTab(Tab.TASKS);

                    break;
                case R.id.main_drawer_custom_times:
                    if (mDrawerTaskListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerTaskListener);
                        mDrawerTaskListener = null;
                    }
                    if (mDrawerGroupListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerGroupListener);
                        mDrawerGroupListener = null;
                    }

                    showTab(Tab.CUSTOM_TIMES);

                    break;
                case R.id.main_drawer_friends:
                    showTab(Tab.FRIENDS);

                    break;
                case R.id.main_drawer_sign_in:
                    if (sUserData != null) {
                        Auth.GoogleSignInApi.signOut(mGoogleApiClient);

                        mFirebaseAuth.signOut();

                        if (mVisibleTab == Tab.FRIENDS) {
                            mMainActivityNavigation.setCheckedItem(R.id.main_drawer_instances);
                            showTab(Tab.INSTANCES);
                        }
                    } else {
                        // todo block gui until sign in complete
                        startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient), RC_SIGN_IN);
                    }

                    break;
                case R.id.main_drawer_debug:
                    if (mDrawerTaskListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerTaskListener);
                        mDrawerTaskListener = null;
                    }
                    if (mDrawerGroupListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerGroupListener);
                        mDrawerGroupListener = null;
                    }

                    showTab(Tab.DEBUG);
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }

            mMainActivityDrawer.closeDrawer(GravityCompat.START);

            invalidateOptionsMenu();

            return true;
        });

        View headerView = mMainActivityNavigation.getHeaderView(0);
        Assert.assertTrue(headerView != null);

        headerView.setOnLongClickListener(v -> {
            mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_debug).setVisible(true);
            return true;
        });

        mNavHeaderName = (TextView) headerView.findViewById(R.id.nav_header_name);
        Assert.assertTrue(mNavHeaderName != null);

        mNavHeaderEmail = (TextView) headerView.findViewById(R.id.nav_header_email);
        Assert.assertTrue(mNavHeaderEmail != null);

        showTab(mVisibleTab);

        TickService.register(this);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, (@NonNull ConnectionResult connectionResult) -> mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_sign_in).setVisible(false))
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onBackPressed() {
        if (mMainActivityDrawer.isDrawerOpen(GravityCompat.START))
            mMainActivityDrawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(VISIBLE_TAB_KEY, mVisibleTab);

        if (mVisibleTab == Tab.INSTANCES) {
            Assert.assertTrue(mDaysPager.getVisibility() == View.VISIBLE);
            if (mDaysPager.getCurrentItem() != 0 && mOnPageChangeListener != null)
                outState.putInt(IGNORE_FIRST_KEY, 1);
        }

        outState.putSerializable(TIME_RANGE_KEY, mTimeRange);
    }

    private void showTab(@NonNull Tab tab) {
        float density = getResources().getDisplayMetrics().density;

        switch (tab) {
            case INSTANCES:
                mActionBar.setTitle(null);
                mDaysPager.setVisibility(View.VISIBLE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, INSTANCES_ELEVATION * density);
                mMainActivitySpinner.setVisibility(View.VISIBLE);
                mFriendListFragment.hide();

                break;
            case TASKS:
                mActionBar.setTitle(getString(R.string.tasks));
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.VISIBLE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION * density);
                mMainActivitySpinner.setVisibility(View.GONE);
                mFriendListFragment.hide();

                break;
            case CUSTOM_TIMES:
                mActionBar.setTitle(getString(R.string.times));
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.VISIBLE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION * density);
                mMainActivitySpinner.setVisibility(View.GONE);
                mFriendListFragment.hide();

                break;
            case FRIENDS:
                Assert.assertTrue(sUserData != null);

                mActionBar.setTitle(R.string.friends);
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION * density);
                mMainActivitySpinner.setVisibility(View.GONE);
                mFriendListFragment.show(sUserData);

                break;
            case DEBUG:
                mActionBar.setTitle("Debug");
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.VISIBLE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION * density);
                mMainActivitySpinner.setVisibility(View.GONE);
                mFriendListFragment.hide();

                break;
            default:
                throw new IllegalArgumentException();
        }
        mVisibleTab = tab;
    }

    @Override
    public void onCreateTaskActionMode(final ActionMode actionMode) {
        Assert.assertTrue(mDrawerTaskListener == null);
        mDrawerTaskListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    actionMode.finish();
                }
            }
        };
        mMainActivityDrawer.addDrawerListener(mDrawerTaskListener);
    }

    @Override
    public void onDestroyTaskActionMode() {
        Assert.assertTrue(mDrawerTaskListener != null);

        mMainActivityDrawer.removeDrawerListener(mDrawerTaskListener);
        mDrawerTaskListener = null;
    }

    @Override
    public void setTaskSelectAllVisibility(boolean selectAllVisible) {
        mTaskSelectAllVisible = selectAllVisible;

        invalidateOptionsMenu();
    }

    @Override
    public void onCreateGroupActionMode(final ActionMode actionMode) {
        Assert.assertTrue(mDrawerGroupListener == null);
        mDrawerGroupListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    actionMode.finish();
                }
            }
        };
        mMainActivityDrawer.addDrawerListener(mDrawerGroupListener);

        Assert.assertTrue(mOnPageChangeListener == null);
        mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mIgnoreFirst)
                    mIgnoreFirst = false;
                else
                    actionMode.finish();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        mDaysPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public void onDestroyGroupActionMode() {
        Assert.assertTrue(mDrawerGroupListener != null);
        Assert.assertTrue(mOnPageChangeListener != null);

        mMainActivityDrawer.removeDrawerListener(mDrawerGroupListener);
        mDrawerGroupListener = null;

        mDaysPager.removeOnPageChangeListener(mOnPageChangeListener);
        mOnPageChangeListener = null;
    }

    @Override
    public void setGroupSelectAllVisibility(Integer position, boolean selectAllVisible) {
        Assert.assertTrue(position != null);

        mGroupSelectAllVisible.put(position, selectAllVisible);

        invalidateOptionsMenu();
    }

    @Override
    public void onCreateCustomTimesActionMode(ActionMode actionMode) {
        Assert.assertTrue(mDrawerCustomTimesListener == null);
        mDrawerCustomTimesListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    actionMode.finish();
                }
            }
        };
        mMainActivityDrawer.addDrawerListener(mDrawerCustomTimesListener);
    }

    @Override
    public void onDestroyCustomTimesActionMode() {
        Assert.assertTrue(mDrawerCustomTimesListener != null);

        mMainActivityDrawer.removeDrawerListener(mDrawerCustomTimesListener);
        mDrawerCustomTimesListener = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Assert.assertTrue(googleSignInResult != null);

            if (googleSignInResult.isSuccess()) {
                GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
                Assert.assertTrue(googleSignInAccount != null);

                AuthCredential credential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);

                mFirebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            Log.e("asdf", "signInWithCredential:onComplete:" + task.isSuccessful());

                            if (!task.isSuccessful()) {
                                Exception exception = task.getException();
                                Assert.assertTrue(exception != null);

                                Log.e("asdf", "firebase signin error: " + exception);

                                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show();

                                MyCrashlytics.logException(exception);

                                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                            } else {
                                Toast.makeText(this, getString(R.string.signInAs) + " " + task.getResult().getUser().getDisplayName(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                String message = "google signin error: " + googleSignInResult;

                Log.e("asdf", message);

                Toast.makeText(this, R.string.signInFailed, Toast.LENGTH_SHORT).show();

                MyCrashlytics.logException(new GoogleSignInException("isSuccess: " + googleSignInResult.isSuccess() + ", status: " + googleSignInResult.getStatus()));
            }
        }
    }

    private void updateSignInState(@Nullable FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
            String displayName = firebaseUser.getDisplayName();
            Assert.assertTrue(!TextUtils.isEmpty(displayName));

            String email = firebaseUser.getEmail();
            Assert.assertTrue(!TextUtils.isEmpty(email));

            mNavHeaderName.setText(displayName);
            mNavHeaderEmail.setText(email);

            mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_sign_in).setTitle(R.string.signOut);
            mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_friends).setEnabled(true);
        } else {
            mNavHeaderName.setText(null);
            mNavHeaderEmail.setText(null);

            mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_sign_in).setTitle(R.string.signIn);
            mMainActivityNavigation.getMenu().findItem(R.id.main_drawer_friends).setEnabled(false);
        }
    }

    @Nullable
    public static UserData getUserData() {
        return sUserData;
    }

    private class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter {
        @SuppressLint("UseSparseArrays")
        private final Map<Integer, WeakReference<DayFragment>> mDayFragments = new HashMap<>();

        MyFragmentStatePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            DayFragment dayFragment = DayFragment.newInstance(mTimeRange, position);

            mDayFragments.put(position, new WeakReference<>(dayFragment));

            return dayFragment;
        }

        @Override
        public int getCount() {
            return Integer.MAX_VALUE;
        }

        @NonNull
        DayFragment getCurrentItem() {
            int position = mDaysPager.getCurrentItem();
            Assert.assertTrue(mDayFragments.containsKey(position));

            DayFragment dayFragment = mDayFragments.get(position).get();
            Assert.assertTrue(dayFragment != null);

            return dayFragment;
        }
    }

    private static class GoogleSignInException extends Exception {
        GoogleSignInException(@NonNull String message) {
            super(message);
        }
    }
}
