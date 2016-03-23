package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.ShowTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowTaskLoader.Data> {
    private RecyclerView mShowTaskRecycler;
    private TextView mTasksHeadingLabel;
    private TextView mTasksRowSchedule;
    private FloatingActionButton mFloatingActionButton;

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

        mShowTaskRecycler = (RecyclerView) findViewById(R.id.show_task_recycler);
        mShowTaskRecycler.setLayoutManager(new LinearLayoutManager(this));

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.show_task_fab);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        mTaskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(mTaskId != -1);

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

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateChildTaskActivity.getCreateIntent(ShowTaskActivity.this, data.TaskId));
            }
        });

        ArrayList<TaskAdapter.Data> taskDatas = new ArrayList<>();
        for (ShowTaskLoader.ChildTaskData childTaskData : data.ChildTaskDatas)
            taskDatas.add(new TaskAdapter.Data(childTaskData.TaskId, childTaskData.Name, null, childTaskData.HasChildTasks));

        mShowTaskRecycler.setAdapter(new TaskAdapter(this, taskDatas, data.DataId, null));

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
}
