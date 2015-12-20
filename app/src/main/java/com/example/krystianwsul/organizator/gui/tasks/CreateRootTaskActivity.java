package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizator.domainmodel.tasks.DailySchedule;
import com.example.krystianwsul.organizator.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizator.domainmodel.tasks.SingleSchedule;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizator.domainmodel.tasks.WeeklySchedule;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class CreateRootTaskActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";
    private static final String POSITION_KEY = "position";

    private Spinner mCreateRootTaskSpinner;

    private Task mRootTask;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateRootTaskActivity.class);
    }

    public static Intent getEditIntent(Context context, Task rootTask) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(rootTask != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(ROOT_TASK_ID_KEY, rootTask.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_root_task);

        final EditText createRootTaskName = (EditText) findViewById(R.id.create_root_task_name);

        Intent intent = getIntent();
        if (intent.hasExtra(ROOT_TASK_ID_KEY)) {
            int rootTaskId = intent.getIntExtra(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(rootTaskId != -1);

            mRootTask = TaskFactory.getInstance().getTask(rootTaskId);
            Assert.assertTrue(mRootTask != null);
        }

        int spinnerPosition = 0;
        int count = 1;
        if (savedInstanceState != null) {
            int position = savedInstanceState.getInt(POSITION_KEY, -1);
            Assert.assertTrue(position != -1);
            if (position > 0)
                count = 2;
        } else if (mRootTask != null) {
            createRootTaskName.setText(mRootTask.getName());

            Schedule schedule = mRootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(schedule != null);

            Fragment fragment;
            if (schedule instanceof SingleSchedule) {
                fragment = SingleScheduleFragment.newInstance(mRootTask);
            } else if (schedule instanceof DailySchedule) {
                fragment = DailyScheduleFragment.newInstance(mRootTask);
                spinnerPosition = 1;
            } else if (schedule instanceof WeeklySchedule) {
                fragment = WeeklyScheduleFragment.newInstance(mRootTask);
                spinnerPosition = 2;
            } else {
                throw new IndexOutOfBoundsException("unknown schedule type");
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.create_root_task_frame, fragment).commit();
        } else {
            loadFragment(0);
        }
        final int finalCount = count;

        Button createRootTaskSave = (Button) findViewById(R.id.create_root_task_save);
        createRootTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = createRootTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.task_name_toast));
                    messageDialogFragment.show(getSupportFragmentManager(), "empty_name");
                    return;
                }

                ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
                Assert.assertTrue(scheduleFragment != null);

                if (!scheduleFragment.isValidTime()) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.invalid_time_message));
                    messageDialogFragment.show(getSupportFragmentManager(), "invalid_time");
                    return;
                }

                Task rootTask;
                TimeStamp timeStamp = TimeStamp.getNow();
                if (mRootTask != null) {
                    mRootTask.setName(name);

                    Assert.assertTrue(mRootTask.current(timeStamp));
                    mRootTask.setScheduleEndTimeStamp(timeStamp);

                    rootTask = mRootTask;
                } else {
                    rootTask = TaskFactory.getInstance().createRootTask(name, timeStamp);
                    Assert.assertTrue(rootTask != null);
                }

                Schedule schedule = scheduleFragment.createSchedule(rootTask, timeStamp);
                Assert.assertTrue(schedule != null);

                rootTask.addSchedule(schedule);

                finish();
            }
        });
        mCreateRootTaskSpinner = (Spinner) findViewById(R.id.create_root_task_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateRootTaskSpinner.setAdapter(adapter);

        mCreateRootTaskSpinner.setSelection(spinnerPosition);

        mCreateRootTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                loadFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        DatePickerFragment.DatePickerFragmentListener datePickerFragmentListener = (DatePickerFragment.DatePickerFragmentListener) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
        Assert.assertTrue(datePickerFragmentListener != null);

        datePickerFragmentListener.onDatePickerFragmentResult(date);
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        HourMinutePickerFragment.HourMinutePickerFragmentListener hourMinutePickerFragmentListener = (HourMinutePickerFragment.HourMinutePickerFragmentListener) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
        Assert.assertTrue(hourMinutePickerFragmentListener != null);

        hourMinutePickerFragmentListener.onHourMinutePickerFragmentResult(hourMinute);
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getSupportFragmentManager().beginTransaction().replace(R.id.create_root_task_frame, fragment).commit();
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(POSITION_KEY, mCreateRootTaskSpinner.getSelectedItemPosition());
    }
}