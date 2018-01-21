package com.krystianwsul.checkme.gui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.MyApplication;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

public class ShowTaskActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowTaskLoader.Data>, TaskListFragment.TaskListListener {
    public static final String TASK_KEY_KEY = "taskKey";

    public static final int REQUEST_EDIT_TASK = 1;

    private TaskKey mTaskKey;

    private ActionBar mActionBar;

    private ShowTaskLoader.Data mData;

    private boolean mSelectAllVisible = false;

    private TaskListFragment mTaskListFragment;

    public static Intent newIntent(@NonNull TaskKey taskKey) {
        Intent intent = new Intent(MyApplication.instance, ShowTaskActivity.class);
        intent.putExtra(TASK_KEY_KEY, (Parcelable) taskKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_task);

        Toolbar toolbar = findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setTitle(null);

        FloatingActionButton showTaskFab = findViewById(R.id.show_task_fab);
        Assert.assertTrue(showTaskFab != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(TASK_KEY_KEY));

            mTaskKey = savedInstanceState.getParcelable(TASK_KEY_KEY);
            Assert.assertTrue(mTaskKey != null);
        } else {
            Intent intent = getIntent();
            Assert.assertTrue(intent.hasExtra(TASK_KEY_KEY));

            mTaskKey = intent.getParcelableExtra(TASK_KEY_KEY);
            Assert.assertTrue(mTaskKey != null);
        }

        mTaskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment);
        if (mTaskListFragment == null) {
            mTaskListFragment = TaskListFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.show_task_fragment, mTaskListFragment)
                    .commit();
        }
        mTaskListFragment.setFab(showTaskFab);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == REQUEST_EDIT_TASK);

        if (resultCode == RESULT_OK) {
            Assert.assertTrue(data.hasExtra(TASK_KEY_KEY));

            mTaskKey = data.getParcelableExtra(TASK_KEY_KEY);
            Assert.assertTrue(mTaskKey != null);

            Intent result = new Intent();
            result.putExtra(ShowTaskActivity.TASK_KEY_KEY, (Parcelable) mTaskKey);

            setResult(RESULT_OK, result);
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
                getSupportLoaderManager().destroyLoader(0);

                startActivityForResult(CreateTaskActivity.Companion.getEditIntent(mTaskKey), REQUEST_EDIT_TASK);
                break;
            case R.id.task_menu_share:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mTaskListFragment != null);

                String shareData = mTaskListFragment.getShareData();
                if (TextUtils.isEmpty(shareData))
                    Utils.share(mData.getName());
                else
                    Utils.share(mData.getName() + "\n" + shareData);

                Utils.share(mData.getName());
                break;
            case R.id.task_menu_delete: {
                TaskListFragment taskListFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment);
                Assert.assertTrue(taskListFragment != null);

                getSupportLoaderManager().destroyLoader(0);

                DomainFactory.getDomainFactory().setTaskEndTimeStamp(this, mData.getDataId(), SaveService.Source.GUI, mTaskKey);

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

        mActionBar.setTitle(data.getName());

        if (TextUtils.isEmpty(data.getScheduleText()))
            mActionBar.setSubtitle(null);
        else
            mActionBar.setSubtitle(data.getScheduleText());

        invalidateOptionsMenu();

        mTaskListFragment.setTaskKey(mTaskKey, data.getDataId(), data.getTaskData());
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(TASK_KEY_KEY, mTaskKey);
    }
}
