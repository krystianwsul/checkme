package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

public class ShowTaskActivity extends AppCompatActivity {
    private RecyclerView mShowTaskRecycler;
    private TextView mTasksHeadingLabel;
    private TextView mTasksRowSchedule;

    private Task mTask;

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

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        int taskId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(taskId != -1);
        mTask = TaskFactory.getInstance().getTask(taskId);
        Assert.assertTrue(mTask != null);

        mTasksHeadingLabel = (TextView) findViewById(R.id.show_task_name);

        mTasksRowSchedule = (TextView) findViewById(R.id.show_task_schedule);

        mShowTaskRecycler = (RecyclerView) findViewById(R.id.show_task_recycler);
        mShowTaskRecycler.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.show_task_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateChildTaskActivity.getCreateIntent(ShowTaskActivity.this, mTask));
            }
        });

        ImageView showTaskEdit = (ImageView) findViewById(R.id.show_task_edit);
        showTaskEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTask.isRootTask(TimeStamp.getNow()))
                    startActivity(CreateRootTaskActivity.getEditIntent(ShowTaskActivity.this, mTask));
                else
                    startActivity(CreateChildTaskActivity.getEditIntent(ShowTaskActivity.this, mTask));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mShowTaskRecycler.setAdapter(new TaskAdapter(this, mTask.getChildTasks(TimeStamp.getNow())));

        mTasksHeadingLabel.setText(mTask.getName());
        String scheduleText = mTask.getScheduleText(this, TimeStamp.getNow());
        if (TextUtils.isEmpty(scheduleText))
            mTasksRowSchedule.setVisibility(View.GONE);
        else
            mTasksRowSchedule.setText(scheduleText);
    }
}
