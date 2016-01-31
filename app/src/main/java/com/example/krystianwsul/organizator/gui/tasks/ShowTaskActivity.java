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
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.DomainLoader;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<DomainFactory> {
    private RecyclerView mShowTaskRecycler;
    private TextView mTasksHeadingLabel;
    private TextView mTasksRowSchedule;
    private ImageView mShowTaskEdit;
    private FloatingActionButton mFloatingActionButton;

    private static final String INTENT_KEY = "taskId";

    public static Intent getIntent(Task task, Context context) {
        Intent intent = new Intent(context, ShowTaskActivity.class);
        intent.putExtra(INTENT_KEY, task.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_task);

        mTasksHeadingLabel = (TextView) findViewById(R.id.show_task_name);

        mTasksRowSchedule = (TextView) findViewById(R.id.show_task_schedule);

        mShowTaskRecycler = (RecyclerView) findViewById(R.id.show_task_recycler);
        mShowTaskRecycler.setLayoutManager(new LinearLayoutManager(this));

        mShowTaskEdit = (ImageView) findViewById(R.id.show_task_edit);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.show_task_fab);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, DomainFactory domainFactory) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        int taskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(taskId != -1);
        final Task task = domainFactory.getTaskFactory().getTask(taskId);
        Assert.assertTrue(task != null);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateChildTaskActivity.getCreateIntent(ShowTaskActivity.this, task));
            }
        });

        mShowTaskEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (task.isRootTask(TimeStamp.getNow()))
                    startActivity(CreateRootTaskActivity.getEditIntent(ShowTaskActivity.this, task));
                else
                    startActivity(CreateChildTaskActivity.getEditIntent(ShowTaskActivity.this, task));
            }
        });

        mShowTaskRecycler.setAdapter(new TaskAdapter(this, domainFactory, task.getChildTasks(TimeStamp.getNow())));

        mTasksHeadingLabel.setText(task.getName());
        String scheduleText = task.getScheduleText(this, TimeStamp.getNow());
        if (TextUtils.isEmpty(scheduleText))
            mTasksRowSchedule.setVisibility(View.GONE);
        else
            mTasksRowSchedule.setText(scheduleText);
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mFloatingActionButton.setOnClickListener(null);
        mShowTaskEdit.setOnClickListener(null);
        mShowTaskRecycler.setAdapter(null);
    }
}
