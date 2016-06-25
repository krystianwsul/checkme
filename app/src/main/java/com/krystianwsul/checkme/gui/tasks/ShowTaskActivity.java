package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.notifications.TickService;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowTaskLoader.Data>, TaskListFragment.TaskListListener {
    private static final String INTENT_KEY = "taskId";

    private int mTaskId;

    private ActionBar mActionBar;

    private ShowTaskLoader.Data mData;

    public static Intent getIntent(int taskId, Context context) {
        Intent intent = new Intent(context, ShowTaskActivity.class);
        intent.putExtra(INTENT_KEY, taskId);
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

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        mTaskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(mTaskId != -1);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentById(R.id.show_task_fragment) == null)
            fragmentManager.beginTransaction().add(R.id.show_task_fragment, TaskListFragment.getInstance(mTaskId)).commit();

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
        menu.findItem(R.id.task_menu_delete).setVisible(mData != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.task_menu_edit:
                if (mData.IsRootTask)
                    startActivity(CreateRootTaskActivity.getEditIntent(ShowTaskActivity.this, mData.TaskId));
                else
                    startActivity(CreateChildTaskActivity.getEditIntent(ShowTaskActivity.this, mData.TaskId));
                break;
            case R.id.task_menu_delete:
                ArrayList<Integer> dataIds = new ArrayList<>();
                dataIds.add(mData.DataId);
                dataIds.add(((TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.show_task_fragment)).getDataId());
                DomainFactory.getDomainFactory(this).setTaskEndTimeStamp(dataIds, mData.TaskId);
                TickService.startService(this);
                finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    protected void onResume() {
        MyCrashlytics.log("ShowTaskActivity.onResume");

        super.onResume();
    }

    @Override
    public Loader<ShowTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowTaskLoader(this, mTaskId);
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
}
