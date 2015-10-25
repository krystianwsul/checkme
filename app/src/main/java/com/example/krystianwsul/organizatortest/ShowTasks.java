package com.example.krystianwsul.organizatortest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.domain.instances.TopInstance;
import com.example.krystianwsul.organizatortest.domain.tasks.TopTask;
import com.example.krystianwsul.organizatortest.timing.Date;
import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.schedules.Schedule;
import com.example.krystianwsul.organizatortest.timing.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.timing.schedules.WeeklySchedule;
import com.example.krystianwsul.organizatortest.timing.times.CustomTime;
import com.example.krystianwsul.organizatortest.timing.times.HourMinute;
import com.example.krystianwsul.organizatortest.timing.times.Time;
import com.example.krystianwsul.organizatortest.domain.Root;
import com.example.krystianwsul.organizatortest.domain.tasks.StubTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class ShowTasks extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_tasks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        loadTasksTest();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadTasksTest()
    {
        Root root = Root.getInstance();

        ArrayList<TopInstance> instances = new ArrayList<>();
        for (TopTask child : root)
            instances.addAll(child.getInstances(null, new TimeStamp(Calendar.getInstance())));

        Collections.sort(instances);

        ListView showTasksList = (ListView) findViewById(R.id.show_tasks_list);
        showTasksList.setAdapter(new TestAdapter(this, instances));
    }
}
