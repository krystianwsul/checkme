package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class CreateRootTaskActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    public static Intent getIntent(Context context) {
        return new Intent(context, CreateRootTaskActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_root_task);

        final EditText createTaskName = (EditText) findViewById(R.id.create_root_task_name);

        Button createTaskSave = (Button) findViewById(R.id.create_root_task_save);
        createTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = createTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(v.getContext(), R.string.task_name_toast, Toast.LENGTH_SHORT).show();
                    createTaskName.requestFocus();
                    return;
                }

                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                schedulePickerFragment.createRootTask(name);

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
}