package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.ShowTaskLoader;

import junit.framework.Assert;

public class ShowTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowTaskLoader.Data>, TaskListFragment.TaskListListener {
    private TextView mTasksHeadingLabel;
    private TextView mTasksRowSchedule;

    private static final String INTENT_KEY = "taskId";

    private int mTaskId;

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
        setSupportActionBar(toolbar);

        mTasksHeadingLabel = (TextView) findViewById(R.id.show_task_name);

        mTasksRowSchedule = (TextView) findViewById(R.id.show_task_schedule);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        mTaskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(mTaskId != -1);

        TaskListFragment taskListFragment = TaskListFragment.getInstance(mTaskId);
        getSupportFragmentManager().beginTransaction().add(R.id.show_task_fragment, taskListFragment).commit();

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_task_menu, menu);
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<ShowTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowTaskLoader(this, mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<ShowTaskLoader.Data> loader, final ShowTaskLoader.Data data) {
        mData = data;

        mTasksHeadingLabel.setText(data.Name);
        String scheduleText = data.ScheduleText;
        if (TextUtils.isEmpty(scheduleText))
            mTasksRowSchedule.setVisibility(View.GONE);
        else
            mTasksRowSchedule.setText(scheduleText);
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
