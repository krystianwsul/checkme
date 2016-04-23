package com.example.krystianwsul.organizator.gui;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.gui.customtimes.ShowCustomTimesFragment;
import com.example.krystianwsul.organizator.gui.instances.DayFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity implements TaskListFragment.TaskListListener {
    private static final String VISIBLE_TAB_KEY = "visibleTab";
    private static final int INSTANCES_VISIBLE = 0;
    private static final int TASKS_VISIBLE = 1;
    private static final int CUSTOM_TIMES_VISIBLE = 2;

    private ViewPager mDaysPager;
    private FrameLayout mMainTaskListFrame;
    private FrameLayout mMainCustomTimesFrame;

    private DrawerLayout mMainActivityDrawer;

    private DrawerLayout.DrawerListener mDrawerListener;

    private int mVisibleTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(VISIBLE_TAB_KEY));
            mVisibleTab = savedInstanceState.getInt(VISIBLE_TAB_KEY);
        }

        mMainActivityDrawer = (DrawerLayout) findViewById(R.id.main_activity_drawer);
        Assert.assertTrue(mMainActivityDrawer != null);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mMainActivityDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mMainActivityDrawer.addDrawerListener(toggle);
        toggle.syncState();

        final FragmentManager fragmentManager = getSupportFragmentManager();

        TaskListFragment taskListFragment = (TaskListFragment) fragmentManager.findFragmentById(R.id.main_task_list_frame);
        if (taskListFragment == null)
            fragmentManager.beginTransaction().add(R.id.main_task_list_frame, TaskListFragment.getInstance()).commit();

        ShowCustomTimesFragment showCustomTimesFragment = (ShowCustomTimesFragment) fragmentManager.findFragmentById(R.id.main_custom_times_frame);
        if (showCustomTimesFragment == null)
            fragmentManager.beginTransaction().add(R.id.main_custom_times_frame, ShowCustomTimesFragment.newInstance()).commit();

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

        NavigationView mainActivityNavigation = (NavigationView) findViewById(R.id.main_activity_navigation);
        Assert.assertTrue(mainActivityNavigation != null);

        mainActivityNavigation.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.main_drawer_instances:
                    if (mDrawerListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerListener);
                        mDrawerListener = null;
                    }

                    showTab(INSTANCES_VISIBLE);

                    break;
                case R.id.main_drawer_tasks:
                    showTab(TASKS_VISIBLE);
                    break;
                case R.id.main_drawer_custom_times:
                    showTab(CUSTOM_TIMES_VISIBLE);
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }

            mMainActivityDrawer.closeDrawer(GravityCompat.START);
            return true;
        });

        showTab(mVisibleTab);

        TickService.register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_debug:
                startActivity(DebugActivity.getIntent(this));
                return true;
            default:
                throw new UnsupportedOperationException();
        }
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
    }

    private void showTab(int tab) {
        switch (tab) {
            case INSTANCES_VISIBLE:
                mDaysPager.setVisibility(View.VISIBLE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                break;
            case TASKS_VISIBLE:
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.VISIBLE);
                mMainCustomTimesFrame.setVisibility(View.GONE);
                break;
            case CUSTOM_TIMES_VISIBLE:
                mDaysPager.setVisibility(View.GONE);
                mMainTaskListFrame.setVisibility(View.GONE);
                mMainCustomTimesFrame.setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalArgumentException();
        }
        mVisibleTab = tab;
    }

    @Override
    public void onCreateTaskActionMode(final ActionMode actionMode) {
        Assert.assertTrue(mDrawerListener == null);
        mDrawerListener = new DrawerLayout.DrawerListener() {
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
        mMainActivityDrawer.addDrawerListener(mDrawerListener);
    }

    @Override
    public void onDestroyTaskActionMode() {
        Assert.assertTrue(mDrawerListener != null);

        mMainActivityDrawer.removeDrawerListener(mDrawerListener);
        mDrawerListener = null;
    }
}
