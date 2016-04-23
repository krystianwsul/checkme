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
import com.example.krystianwsul.organizator.gui.customtimes.ShowCustomTimesActivity;
import com.example.krystianwsul.organizator.gui.instances.DayFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity implements TaskListFragment.TaskListListener {
    private DrawerLayout mMainActivityDrawer;

    private DrawerLayout.DrawerListener mDrawerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mMainActivityDrawer = (DrawerLayout) findViewById(R.id.main_activity_drawer);
        Assert.assertTrue(mMainActivityDrawer != null);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mMainActivityDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mMainActivityDrawer.addDrawerListener(toggle);
        toggle.syncState();

        final FragmentManager fragmentManager = getSupportFragmentManager();

        TaskListFragment taskListFragment = (TaskListFragment) fragmentManager.findFragmentById(R.id.main_frame);
        if (taskListFragment == null)
            fragmentManager.beginTransaction().add(R.id.main_frame, TaskListFragment.getInstance()).commit();

        ViewPager daysPager = (ViewPager) findViewById(R.id.main_pager);
        Assert.assertTrue(daysPager != null);

        daysPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return DayFragment.newInstance(position);
            }

            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }
        });

        final FrameLayout mainFrame = (FrameLayout) findViewById(R.id.main_frame);
        Assert.assertTrue(mainFrame != null);

        NavigationView mainActivityNavigation = (NavigationView) findViewById(R.id.main_activity_navigation);
        Assert.assertTrue(mainActivityNavigation != null);

        mainActivityNavigation.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.main_drawer_instances:
                    if (mDrawerListener != null) {
                        mMainActivityDrawer.removeDrawerListener(mDrawerListener);
                        mDrawerListener = null;
                    }

                    daysPager.setVisibility(View.VISIBLE);
                    mainFrame.setVisibility(View.GONE);

                    break;
                case R.id.main_drawer_tasks:

                    daysPager.setVisibility(View.GONE);
                    mainFrame.setVisibility(View.VISIBLE);

                    break;
                default:
                    throw new IndexOutOfBoundsException();

            }

            mMainActivityDrawer.closeDrawer(GravityCompat.START);
            return true;
        });

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
            case R.id.action_times:
                startActivity(ShowCustomTimesActivity.getIntent(this));
                return true;
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
