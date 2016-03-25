package com.example.krystianwsul.organizator.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.gui.customtimes.ShowCustomTimesActivity;
import com.example.krystianwsul.organizator.gui.instances.GroupListFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

public class MainActivity extends AppCompatActivity implements TaskListFragment.TaskListListener {
    private ViewPager mViewPager;
    private MyFragmentPagerAdapter mMyFragmentPagerAdapter;

    private ViewPager.OnPageChangeListener mOnPageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        Assert.assertTrue(tabLayout != null);

        tabLayout.addTab(tabLayout.newTab().setText(R.string.instances));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tasks));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = (ViewPager) findViewById(R.id.list_fragment_pager);
        Assert.assertTrue(mViewPager != null);

        mMyFragmentPagerAdapter = new MyFragmentPagerAdapter();
        mViewPager.setAdapter(mMyFragmentPagerAdapter);

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
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_debug:
                startActivity(DebugActivity.getIntent(this));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private Fragment mFragment;

        public MyFragmentPagerAdapter() {
            super(MainActivity.this.getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position <= 1);

            switch (position) {
                case 0:
                    return new GroupListFragment();
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

        @Override
        public void setPrimaryItem(ViewGroup viewGroup, int position, Object object) {
            mFragment = (Fragment) object;
            super.setPrimaryItem(viewGroup, position, object);
        }

        public Fragment getFragment() {
            Assert.assertTrue(mFragment != null);
            return mFragment;
        }
    }

    @Override
    public void onCreateTaskActionMode(final ActionMode actionMode) {
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
    }

    @Override
    public void onDestroyTaskActionMode() {
        Assert.assertTrue(mOnPageChangeListener != null);

        mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
        mOnPageChangeListener = null;
    }
}
