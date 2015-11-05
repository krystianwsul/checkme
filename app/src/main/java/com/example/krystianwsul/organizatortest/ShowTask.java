package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
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

        TextView tasksHeadingLabel = (TextView) findViewById(R.id.show_task_name);
        tasksHeadingLabel.setText(task.getName());

        TextView tasksRowSchedule = (TextView) findViewById(R.id.show_task_schedule);
        String scheduleText = task.getScheduleText(this);
        if (TextUtils.isEmpty(scheduleText))
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(scheduleText);

        ListView showTaskList = (ListView) findViewById(R.id.show_task_list);
        if (task.getChildTasks().isEmpty())
            showTaskList.setVisibility(View.GONE);
        else
            showTaskList.setAdapter(new TaskAdapter(this, task.getChildTasks()));

        showTaskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowTask.class);
                intent.putExtra("taskId", task.getId());
                startActivity(intent);
            }
        });
    }
}
