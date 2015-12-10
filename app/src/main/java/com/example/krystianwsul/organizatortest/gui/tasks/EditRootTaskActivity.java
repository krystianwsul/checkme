package com.example.krystianwsul.organizatortest.gui.tasks;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class EditRootTaskActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    private static String ROOT_TASK_ID_KEY = "rootTaskId";

    private RootTask mRootTask;

    public static Intent getIntent(Context context, RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        Intent intent = new Intent(context, EditRootTaskActivity.class);
        intent.putExtra(ROOT_TASK_ID_KEY, rootTask.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_root_task);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(ROOT_TASK_ID_KEY));
        final int rootTaskId = intent.getIntExtra(ROOT_TASK_ID_KEY, -1);
        Assert.assertTrue(rootTaskId != -1);
        mRootTask = (RootTask) TaskFactory.getInstance().getTask(rootTaskId);
        Assert.assertTrue(mRootTask != null);

        final EditText createRootTaskName = (EditText) findViewById(R.id.create_root_task_name);
        createRootTaskName.setText(mRootTask.getName());

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

                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                if (!schedulePickerFragment.isValidTime()) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.invalid_time_message));
                    messageDialogFragment.show(getSupportFragmentManager(), "invalid_time");
                    return;
                }

                mRootTask.setName(name);

                Schedule schedule = schedulePickerFragment.createSchedule(mRootTask);
                Assert.assertTrue(schedule != null);

                Assert.assertTrue(mRootTask.current(TimeStamp.getNow()));
                mRootTask.setNewestScheduleEndTimeStamp();
                mRootTask.addSchedule(schedule);

                finish();
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

    public static class MessageDialogFragment extends DialogFragment {
        private static String MESSAGE_KEY = "message";

        public static MessageDialogFragment newInstance(String message) {
            Assert.assertTrue(!TextUtils.isEmpty(message));

            MessageDialogFragment messageDialogFragment = new MessageDialogFragment();

            Bundle args = new Bundle();
            args.putString(MESSAGE_KEY, message);
            messageDialogFragment.setArguments(args);

            return messageDialogFragment;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();

            String message = args.getString(MESSAGE_KEY);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            return builder.create();
        }
    }
}