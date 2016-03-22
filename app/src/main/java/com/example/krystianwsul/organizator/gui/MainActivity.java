package com.example.krystianwsul.organizator.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
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
import com.example.krystianwsul.organizator.gui.tasks.CreateRootTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskAdapter;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnCheckedChangedListener {
    private ViewPager mViewPager;
    private MyFragmentStatePagerAdapter mMyFragmentStatePagerAdapter;

    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        Assert.assertTrue(tabLayout != null);

        tabLayout.addTab(tabLayout.newTab().setText(R.string.instances));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tasks));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = (ViewPager) findViewById(R.id.list_fragment_pager);
        Assert.assertTrue(mViewPager != null);

        mMyFragmentStatePagerAdapter = new MyFragmentStatePagerAdapter();
        mViewPager.setAdapter(mMyFragmentStatePagerAdapter);

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
        getMenuInflater().inflate(R.menu.menu_show_tasks, menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mActionMode != null)
            mActionMode.finish();
    }

    private class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter {
        private Fragment mFragment;

        public MyFragmentStatePagerAdapter() {
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
                    return new TaskListFragment();
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

    private class TaskEditCallback implements ActionMode.Callback {
        private TaskListFragment mTaskListFragment;

        private ViewPager.OnPageChangeListener mOnPageChangeListener;

        @Override
        public boolean onCreateActionMode(final ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;

            actionMode.getMenuInflater().inflate(R.menu.menu_edit_tasks, menu);

            mTaskListFragment = (TaskListFragment) mMyFragmentStatePagerAdapter.getFragment();
            Assert.assertTrue(mTaskListFragment != null);

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

            actionMode.setTitle(getString(R.string.join));

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            ArrayList<Integer> taskIds = mTaskListFragment.getSelected();
            Assert.assertTrue(taskIds != null);
            Assert.assertTrue(!taskIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_task_join:
                    if (taskIds.size() == 1) {
                        MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.two_tasks_message));
                        messageDialogFragment.show(getSupportFragmentManager(), "two_tasks");
                    } else {
                        startActivity(CreateRootTaskActivity.getJoinIntent(MainActivity.this, taskIds));
                        actionMode.finish();
                    }

                    return true;
                case R.id.action_task_delete:
                    mTaskListFragment.removeSelected();
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Assert.assertTrue(mTaskListFragment != null);

            mActionMode = null;
            mViewPager.removeOnPageChangeListener(mOnPageChangeListener);

            mTaskListFragment.uncheck();
        }
    }

    @Override
    public void OnCheckedChanged() {
        TaskListFragment taskListFragment = (TaskListFragment) mMyFragmentStatePagerAdapter.getFragment();
        Assert.assertTrue(taskListFragment != null);

        ArrayList<Integer> taskIds = taskListFragment.getSelected();
        if (taskIds.isEmpty()) {
            if (mActionMode != null)
                mActionMode.finish();
        } else {
            if (mActionMode == null)
                startSupportActionMode(new TaskEditCallback());
        }
    }
}
