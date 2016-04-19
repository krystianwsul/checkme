package com.example.krystianwsul.organizator.gui;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.gui.customtimes.ShowCustomTimesActivity;
import com.example.krystianwsul.organizator.gui.instances.GroupListFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity implements TaskListFragment.TaskListListener {
    private DrawerLayout mMainActivityDrawer;
    //private ViewPager mViewPager;
    private FrameLayout mMainActivityFrame;

    //private ViewPager.OnPageChangeListener mOnPageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        /*
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        Assert.assertTrue(tabLayout != null);

        tabLayout.addTab(tabLayout.newTab().setText(R.string.instances));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tasks));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = (ViewPager) findViewById(R.id.list_fragment_pager);
        Assert.assertTrue(mViewPager != null);

        MyFragmentPagerAdapter myFragmentPagerAdapter = new MyFragmentPagerAdapter();
        mViewPager.setAdapter(myFragmentPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        */

        mMainActivityDrawer = (DrawerLayout) findViewById(R.id.main_activity_drawer);
        Assert.assertTrue(mMainActivityDrawer != null);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mMainActivityDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mMainActivityDrawer.addDrawerListener(toggle);
        toggle.syncState();

        final FragmentManager fragmentManager = getSupportFragmentManager();

        NavigationView mainActivityNavigation = (NavigationView) findViewById(R.id.main_activity_navigation);
        Assert.assertTrue(mainActivityNavigation != null);

        mainActivityNavigation.setNavigationItemSelectedListener(item -> {
            Fragment oldFragment = fragmentManager.findFragmentById(R.id.main_activity_frame);
            switch (item.getItemId()) {
                case R.id.main_drawer_instances:
                    if (!(oldFragment instanceof GroupListFragment))
                        fragmentManager.beginTransaction().replace(R.id.main_activity_frame, GroupListFragment.getGroupInstance()).commit();
                    break;
                case R.id.main_drawer_tasks:
                    if (!(oldFragment instanceof TaskListFragment))
                    fragmentManager.beginTransaction().replace(R.id.main_activity_frame, TaskListFragment.getInstance()).commit();
                    break;
                default:
                    throw new IndexOutOfBoundsException();

            }

            mMainActivityDrawer.closeDrawer(GravityCompat.START);
            return true;
        });

        mMainActivityFrame = (FrameLayout) findViewById(R.id.main_activity_frame);
        Assert.assertTrue(mMainActivityFrame != null);

        Fragment fragment = fragmentManager.findFragmentById(R.id.main_activity_frame);
        if (fragment == null)
            fragmentManager.beginTransaction().add(R.id.main_activity_frame, GroupListFragment.getGroupInstance()).commit();

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

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        public MyFragmentPagerAdapter() {
            super(MainActivity.this.getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position <= 1);

            switch (position) {
                case 0:
                    return GroupListFragment.getGroupInstance();
                case 1:
                    return TaskListFragment.getInstance();
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onCreateTaskActionMode(final ActionMode actionMode) {
    /*
        mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                actionMode.finish();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        */
    }

    @Override
    public void onDestroyTaskActionMode() {
/*
        Assert.assertTrue(mOnPageChangeListener != null);

        mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
        mOnPageChangeListener = null;
    */
    }
}
