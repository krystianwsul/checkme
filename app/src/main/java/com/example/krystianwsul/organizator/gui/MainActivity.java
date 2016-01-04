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
import com.example.krystianwsul.organizator.TickReceiver;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.gui.customtimes.ShowCustomTimesActivity;
import com.example.krystianwsul.organizator.gui.instances.GroupListFragment;
import com.example.krystianwsul.organizator.gui.tasks.CreateRootTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TaskListFragment;

import junit.framework.Assert;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private MyFragmentStatePagerAdapter mMyFragmentStatePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.instances));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tasks));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = (ViewPager) findViewById(R.id.list_fragment_pager);
        mMyFragmentStatePagerAdapter = new MyFragmentStatePagerAdapter();
        mViewPager.setAdapter(mMyFragmentStatePagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mMyFragmentStatePagerAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            public void onTabUnselected(TabLayout.Tab tab) {

            }

            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        TickReceiver.register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_tasks, menu);

        if (mViewPager.getCurrentItem() == 1) {
            MenuItem menuItem = menu.findItem(R.id.action_task_edit);
            menuItem.setVisible(true);
        }

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
            case R.id.action_task_edit:
                startSupportActionMode(new TaskEditCallback());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public interface RefreshFragment {
        void refresh();
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
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(Object object) {
            ((RefreshFragment) object).refresh();
            return super.getItemPosition(object);
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
        private ActionMode mActionMode;

        private TaskListFragment mTaskListFragment;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;

            actionMode.getMenuInflater().inflate(R.menu.menu_edit_tasks, menu);

            mTaskListFragment = (TaskListFragment) mMyFragmentStatePagerAdapter.getFragment();
            Assert.assertTrue(mTaskListFragment != null);

            mTaskListFragment.setEditing(true);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_task_join:
                    ArrayList<Task> tasks = mTaskListFragment.getSelected();
                    Assert.assertTrue(tasks != null);

                    if (tasks.size() < 2) {
                        MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.two_tasks_message));
                        messageDialogFragment.show(getSupportFragmentManager(), "two_tasks");
                    } else {
                        startActivity(CreateRootTaskActivity.getJoinIntent(MainActivity.this, tasks));
                        mActionMode.finish();
                    }

                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle(getString(R.string.join));
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Assert.assertTrue(mTaskListFragment != null);
            mTaskListFragment.setEditing(false);
        }
    }
}
