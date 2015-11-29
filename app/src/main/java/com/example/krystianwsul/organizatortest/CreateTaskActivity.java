package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class CreateTaskActivity extends AppCompatActivity implements TimePickerFragment.TimePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    private static final String INTENT_KEY = "parentTaskId";

    public static Intent getIntent(Context context) {
        return new Intent(context, CreateTaskActivity.class);
    }

    public static Intent getIntent(Context context, Task task) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(INTENT_KEY, task.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        Integer parentTaskId;
        if (getIntent().hasExtra(INTENT_KEY)) {
            parentTaskId = getIntent().getIntExtra(INTENT_KEY, -1);
            Assert.assertTrue(parentTaskId != -1);

            Task parentTask = TaskFactory.getInstance().getTask(parentTaskId);
            Assert.assertTrue(parentTask != null);
        }

        if (savedInstanceState != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Assert.assertTrue(fragmentManager != null);

            Fragment fragment = fragmentManager.getFragment(savedInstanceState, "fragment");
            Assert.assertTrue(fragment != null);

            fragmentManager.beginTransaction().replace(R.id.create_task_frame, fragment).commit();
        } else {
            loadFragment(0);
        }

        Spinner createTaskSpinner = (Spinner) findViewById(R.id.create_task_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        createTaskSpinner.setAdapter(adapter);

        createTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean mFirst = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (!mFirst)
                    loadFragment(position);

                mFirst = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.create_task_frame);
        Assert.assertTrue(fragment != null);

        getSupportFragmentManager().putFragment(outState, "fragment", fragment);
    }

    private Fragment createFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        switch (position) {
            case 0:
                return SingleScheduleFragment.newInstance();
            case 1:
                return DailyScheduleFragment.newInstance();
            case 2:
                return WeeklyScheduleFragment.newInstance();
            default:
                return null;
        }
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getSupportFragmentManager().beginTransaction().replace(R.id.create_task_frame, fragment).commit();
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        SingleScheduleFragment singleScheduleFragment = (SingleScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_frame);
        Assert.assertTrue(singleScheduleFragment != null);

        singleScheduleFragment.onDatePickerFragmentResult(date);
    }

    @Override
    public void onTimePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        SingleScheduleFragment singleScheduleFragment = (SingleScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_frame);
        Assert.assertTrue(singleScheduleFragment != null);

        singleScheduleFragment.onTimePickerFragmentResult(hourMinute);
    }
}