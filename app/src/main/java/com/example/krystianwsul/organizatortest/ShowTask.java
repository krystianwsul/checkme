package com.example.krystianwsul.organizatortest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

public class ShowTask extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_task);

        Bundle extras = getIntent().getExtras();
        int taskId = extras.getInt("taskId");
        Task task = Task.getTask(taskId);
        Assert.assertTrue(task != null);

        TextView tasksHeadingLabel = (TextView) findViewById(R.id.task_heading_name);
        tasksHeadingLabel.setText(task.getName());

        TextView tasksRowSchedule = (TextView) findViewById(R.id.task_heading_schedule);
        Schedule schedule = task.getSchedule();
        if (schedule == null)
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(schedule.getTaskText(this));
    }
}
