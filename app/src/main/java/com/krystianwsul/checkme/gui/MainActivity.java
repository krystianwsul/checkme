package com.krystianwsul.checkme.gui;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;

import com.krystianwsul.checkme.EventBuffer;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment;
import com.krystianwsul.checkme.gui.instances.DayFragment;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.notifications.TickService;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity implements TaskListFragment.TaskListListener, GroupListFragment.GroupListListener, ShowCustomTimesFragment.CustomTimesListListener {
    private static final String VISIBLE_TAB_KEY = "visibleTab";
    private static final String IGNORE_FIRST_KEY = "ignoreFirst";

    private static final int INSTANCES_VISIBLE = 0;
    private static final int TASKS_VISIBLE = 1;
    private static final int CUSTOM_TIMES_VISIBLE = 2;
    private static final int DEBUG_VISIBLE = 3;

    private static final float NORMAL_ELEVATION = 6;
    private static final float INSTANCES_ELEVATION = 0;

    private AppBarLayout mMainActivityAppBarLayout;
    private ActionBar mActionBar;
    private ViewPager mDaysPager;
    private FrameLayout mMainTaskListFrame;
    private FrameLayout mMainCustomTimesFrame;
    private FrameLayout mMainDebugFrame;

    private DrawerLayout mMainActivityDrawer;

    private DrawerLayout.DrawerListener mDrawerTaskListener;

    private DrawerLayout.DrawerListener mDrawerGroupListener;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    private DrawerLayout.DrawerListener mDrawerCustomTimesListener;

    private int mVisibleTab = INSTANCES_VISIBLE;
    private boolean mIgnoreFirst = false;

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
            mVisibleTab = savedInstanceState.getInt(VISIBLE_TAB_KEY);

            if (savedInstanceState.containsKey(IGNORE_FIRST_KEY)) {
                Assert.assertTrue(mVisibleTab == 0);
                mIgnoreFirst = true;
            }
        }

        mMainActivityAppBarLayout = (AppBarLayout) findViewById(R.id.main_activity_app_bar_layout);
        Assert.assertTrue(mMainActivityAppBarLayout != null);

        mMainActivityDrawer = (DrawerLayout) findViewById(R.id.main_activity_drawer);
        Assert.assertTrue(mMainActivityDrawer != null);

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

        mDaysPager = (ViewPager) findViewById(R.id.main_pager);
        Assert.assertTrue(mDaysPager != null);

        mDaysPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return DayFragment.newInstance(position);
            }

            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }
        });

        mMainTaskListFrame = (FrameLayout) findViewById(R.id.main_task_list_frame);
        Assert.assertTrue(mMainTaskListFrame != null);

        mMainCustomTimesFrame = (FrameLayout) findViewById(R.id.main_custom_times_frame);
        Assert.assertTrue(mMainCustomTimesFrame != null);

        mMainDebugFrame = (FrameLayout) findViewById(R.id.main_debug_frame);
        Assert.assertTrue(mMainDebugFrame != null);

        NavigationView mainActivityNavigation = (NavigationView) findViewById(R.id.main_activity_navigation);
        Assert.assertTrue(mainActivityNavigation != null);

        mainActivityNavigation.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.main_drawer_instances:
                    if (mDrawerTaskListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerTaskListener);
                        mDrawerTaskListener = null;
                    }

                    showTab(INSTANCES_VISIBLE);

                    break;
                case R.id.main_drawer_tasks:
                    if (mDrawerGroupListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerGroupListener);
                        mDrawerGroupListener = null;
                    }

                    showTab(TASKS_VISIBLE);
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

                    showTab(CUSTOM_TIMES_VISIBLE);
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

                    showTab(DEBUG_VISIBLE);
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }

            mMainActivityDrawer.closeDrawer(GravityCompat.START);
            return true;
        });

        View headerView = mainActivityNavigation.getHeaderView(0);
        Assert.assertTrue(headerView != null);

        headerView.setOnLongClickListener(v -> {
            mainActivityNavigation.getMenu().findItem(R.id.main_drawer_debug).setVisible(true);
            return true;
        });

        showTab(mVisibleTab);

        TickService.register(this);
    }

    @Override
    protected void onResume() {
        EventBuffer.getInstance().add("MainActivity onResume");

        super.onResume();
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

        outState.putInt(VISIBLE_TAB_KEY, mVisibleTab);
        if (mVisibleTab == 0) {
            Assert.assertTrue(mDaysPager.getVisibility() == View.VISIBLE);
            if (mDaysPager.getCurrentItem() != 0 && mOnPageChangeListener != null)
                outState.putInt(IGNORE_FIRST_KEY, 1);
        }
    }

    private void showTab(int tab) {
        switch (tab) {
            case INSTANCES_VISIBLE:
                mActionBar.setTitle(getString(R.string.instances));
                mDaysPager.setVisibility(View.VISIBLE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, INSTANCES_ELEVATION);
                break;
            case TASKS_VISIBLE:
                mActionBar.setTitle(getString(R.string.tasks));
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.VISIBLE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION);
                break;
            case CUSTOM_TIMES_VISIBLE:
                mActionBar.setTitle(getString(R.string.times));
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.VISIBLE);
                mMainDebugFrame.setVisibility(View.GONE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION);
                break;
            case DEBUG_VISIBLE:
                mActionBar.setTitle("Debug");
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                mMainDebugFrame.setVisibility(View.VISIBLE);
                ViewCompat.setElevation(mMainActivityAppBarLayout, NORMAL_ELEVATION);
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
}
