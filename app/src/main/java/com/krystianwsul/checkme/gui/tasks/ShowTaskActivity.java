package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowTaskLoader.Data>, TaskListFragment.TaskListListener {
    private static final String TASK_KEY_KEY = "taskKey";

    private TaskKey mTaskKey;

    private ActionBar mActionBar;

    private ShowTaskLoader.Data mData;

    private boolean mSelectAllVisible = false;

    private TaskListFragment mTaskListFragment;

    public static Intent newIntent(@NonNull Context context, @NonNull TaskKey taskKey) {
        Intent intent = new Intent(context, ShowTaskActivity.class);
        intent.putExtra(TASK_KEY_KEY, taskKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_task);

        Toolbar toolbar = (Toolbar) findViewById(R.id.show_task_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setTitle(null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TASK_KEY_KEY));

        mTaskKey = intent.getParcelableExtra(TASK_KEY_KEY);
        Assert.assertTrue(mTaskKey != null);

        mTaskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment);
        if (mTaskListFragment == null) {
            mTaskListFragment = TaskListFragment.getInstance(mTaskKey);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.show_task_fragment, mTaskListFragment)
                    .commit();
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_task_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.task_menu_edit).setVisible(mData != null);

        menu.findItem(R.id.task_menu_share).setVisible(mData != null);

        menu.findItem(R.id.task_menu_delete).setVisible(mData != null);

        menu.findItem(R.id.task_menu_select_all).setVisible(mSelectAllVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.task_menu_edit:
                if (mData.IsRootTask)
                    startActivity(CreateTaskActivity.getEditIntent(ShowTaskActivity.this, mData.mTaskKey));
                else
                    startActivity(CreateTaskActivity.getEditIntent(ShowTaskActivity.this, mData.mTaskKey));
                break;
            case R.id.task_menu_share:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mTaskListFragment != null);

                String shareData = mTaskListFragment.getShareData();
                if (TextUtils.isEmpty(shareData))
                    Utils.share(mData.Name, this);
                else
                    Utils.share(mData.Name + "\n" + shareData, this);

                Utils.share(mData.Name, this);
                break;
            case R.id.task_menu_delete: {
                TaskListFragment taskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment);
                Assert.assertTrue(taskListFragment != null);

                getSupportLoaderManager().destroyLoader(0);
                taskListFragment.destroyLoader();

                ArrayList<Integer> dataIds = new ArrayList<>();
                dataIds.add(mData.DataId);
                dataIds.add(taskListFragment.getDataId());
                DomainFactory.getDomainFactory(this).setTaskEndTimeStamp(this, dataIds, mData.mTaskKey.mLocalTaskId); // todo firebase
                TickService.startService(this);

                finish();
                break;
            }
            case R.id.task_menu_select_all: {
                TaskListFragment taskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment);
                Assert.assertTrue(taskListFragment != null);

                taskListFragment.selectAll();

                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    public Loader<ShowTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowTaskLoader(this, mTaskKey);
    }

    @Override
    public void onLoadFinished(Loader<ShowTaskLoader.Data> loader, final ShowTaskLoader.Data data) {
        mData = data;

        mActionBar.setTitle(data.Name);

        if (TextUtils.isEmpty(data.ScheduleText))
            mActionBar.setSubtitle(null);
        else
            mActionBar.setSubtitle(data.ScheduleText);

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ShowTaskLoader.Data> loader) {
    }

    @Override
    public void onCreateTaskActionMode(ActionMode actionMode) {

    }

    @Override
    public void onDestroyTaskActionMode() {

    }

    @Override
    public void setTaskSelectAllVisibility(boolean selectAllVisible) {
        mSelectAllVisible = selectAllVisible;

        invalidateOptionsMenu();
    }
}
