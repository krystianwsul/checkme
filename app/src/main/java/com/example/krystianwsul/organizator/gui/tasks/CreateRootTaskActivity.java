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
import com.example.krystianwsul.organizator.domainmodel.DailySchedule;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Schedule;
import com.example.krystianwsul.organizator.domainmodel.SingleSchedule;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.domainmodel.WeeklySchedule;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateRootTaskActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String POSITION_KEY = "position";

    private Spinner mCreateRootTaskSpinner;

    private Task mRootTask;

    private ArrayList<Task> mJoinTasks;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateRootTaskActivity.class);
    }

    public static Intent getJoinIntent(Context context, ArrayList<Task> joinTasks) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTasks != null);
        Assert.assertTrue(joinTasks.size() > 1);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);

        ArrayList<Integer> taskIds = new ArrayList<>();
        for (Task task : joinTasks)
            taskIds.add(task.getId());

        intent.putIntegerArrayListExtra(TASK_IDS_KEY, taskIds);
        return intent;
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

        final DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        Intent intent = getIntent();
        if (intent.hasExtra(ROOT_TASK_ID_KEY)) {
            int rootTaskId = intent.getIntExtra(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(rootTaskId != -1);

            mRootTask = domainFactory.getTaskFactory().getTask(rootTaskId);
            Assert.assertTrue(mRootTask != null);
        } else if (intent.hasExtra(TASK_IDS_KEY)) {
            ArrayList<Integer> taskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
            Assert.assertTrue(taskIds != null);
            Assert.assertTrue(taskIds.size() > 1);

            mJoinTasks = new ArrayList<>();
            for (Integer taskId : taskIds) {
                Task task = domainFactory.getTaskFactory().getTask(taskId);
                Assert.assertTrue(task != null);
                Assert.assertTrue(task.isRootTask(TimeStamp.getNow()));

                mJoinTasks.add(task);
            }
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
                    rootTask = domainFactory.getTaskFactory().createRootTask(name, timeStamp);
                    Assert.assertTrue(rootTask != null);
                }

                Schedule schedule = scheduleFragment.createSchedule(rootTask, timeStamp);
                Assert.assertTrue(schedule != null);

                rootTask.addSchedule(schedule);

                if (mJoinTasks != null) {
                    Assert.assertTrue(mJoinTasks.size() > 1);
                    domainFactory.getTaskFactory().joinTasks(rootTask, mJoinTasks, timeStamp);
                }

                domainFactory.getPersistenceManager().save();

                TickService.startService(CreateRootTaskActivity.this);

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