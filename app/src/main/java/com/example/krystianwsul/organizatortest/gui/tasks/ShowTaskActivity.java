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
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskActivity extends AppCompatActivity {
    private RecyclerView mShowTaskRecycler;
    private Task mTask;

    private static final String INTENT_KEY = "taskId";
    private static final int ADD_CHILD_KEY = 2;
    public static final int SHOW_CHILD = 3;

    public static final int TASK_UPDATED = 2;
    public static final String UPDATED_TASK_ID_KEY = "updatedTaskId";

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

        TextView tasksHeadingLabel = (TextView) findViewById(R.id.show_task_name);
        tasksHeadingLabel.setText(mTask.getName());

        TextView tasksRowSchedule = (TextView) findViewById(R.id.show_task_schedule);
        String scheduleText = mTask.getScheduleText(this);
        if (TextUtils.isEmpty(scheduleText))
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(scheduleText);

        mShowTaskRecycler = (RecyclerView) findViewById(R.id.show_task_recycler);
        mShowTaskRecycler.setLayoutManager(new LinearLayoutManager(this));

        final AppCompatActivity activity = this;

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.show_task_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(CreateChildTaskActivity.getIntent(activity, mTask), ADD_CHILD_KEY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_CHILD_KEY) {
            if (resultCode == CreateChildTaskActivity.CHILD_TASK_CREATED) {
                Assert.assertTrue(data.hasExtra(CreateChildTaskActivity.NEW_CHILD_TASK_ID_KEY));

                int newChildTaskId = data.getIntExtra(CreateChildTaskActivity.NEW_CHILD_TASK_ID_KEY, -1);
                Assert.assertTrue(newChildTaskId != -1);

                mTask = TaskFactory.getInstance().getTask(newChildTaskId).getParentTask();

                Intent result = new Intent();
                result.putExtra(UPDATED_TASK_ID_KEY, mTask.getId());
                setResult(TASK_UPDATED, result);
            }
        } else if (requestCode == SHOW_CHILD) {
            if (resultCode == TASK_UPDATED) {
                Assert.assertTrue(data.hasExtra(UPDATED_TASK_ID_KEY));

                int newChildTaskId = data.getIntExtra(UPDATED_TASK_ID_KEY, -1);
                Assert.assertTrue(newChildTaskId != -1);

                mTask = TaskFactory.getInstance().getTask(newChildTaskId).getParentTask();

                Intent result = new Intent();
                result.putExtra(UPDATED_TASK_ID_KEY, mTask.getId());
                setResult(TASK_UPDATED, result);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mTask.getChildTasks().isEmpty()) {
            mShowTaskRecycler.setVisibility(View.GONE);
        } else {
            mShowTaskRecycler.setVisibility(View.VISIBLE);
            mShowTaskRecycler.setAdapter(new TaskAdapter(this, new ArrayList<Task>(mTask.getChildTasks())));
        }
    }
}
