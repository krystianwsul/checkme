package com.example.krystianwsul.organizatortest;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class ShowCustomTimeActivity extends AppCompatActivity {
    private static final String INTENT_KEY = "customTimeId";

    public static Intent getIntent(CustomTime customTime, Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(INTENT_KEY, customTime.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_time);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        int customTimeId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(customTimeId != -1);
        final CustomTime customTime = CustomTime.getCustomTime(customTimeId);
        Assert.assertTrue(customTime != null);

        TextView customTimeName = (TextView) findViewById(R.id.custom_time_name);
        customTimeName.setText(customTime.getName());

        initializeDay(customTime, DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time);
        initializeDay(customTime, DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time);
        initializeDay(customTime, DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time);
        initializeDay(customTime, DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time);
        initializeDay(customTime, DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time);
        initializeDay(customTime, DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time);
        initializeDay(customTime, DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time);
    }

    private void initializeDay(final CustomTime customTime, final DayOfWeek dayOfWeek, int nameId, int timeId) {
        Assert.assertTrue(customTime != null);
        Assert.assertTrue(dayOfWeek != null);

        TextView timeSundayName = (TextView) findViewById(nameId);
        timeSundayName.setText(dayOfWeek.toString());

        TextView timeSundayTime = (TextView) findViewById(timeId);
        timeSundayTime.setText(timeText(customTime.getTimeByDay(dayOfWeek)));
        timeSundayTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                TimePickerFragment timePickerFragment = TimePickerFragment.newInstance(customTime, dayOfWeek);
                timePickerFragment.show(fragmentManager, "tag");
            }
        });
    }

    private String timeText(HourMinute hourMinute) {
        if (hourMinute == null)
            return getResources().getString(R.string.none);
        else
            return hourMinute.toString();
    }

    public static class TimePickerFragment extends DialogFragment {
        public static TimePickerFragment newInstance(CustomTime customTime, DayOfWeek dayOfWeek) {
            Assert.assertTrue(customTime != null);
            Assert.assertTrue(dayOfWeek != null);

            TimePickerFragment timePickerFragment = new TimePickerFragment();

            Bundle args = new Bundle();
            args.putInt("customTimeId", customTime.getId());
            args.putSerializable("dayOfWeek", dayOfWeek);
            timePickerFragment.setArguments(args);

            return timePickerFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();

            int customTimeId = args.getInt("customTimeId");
            CustomTime customTime = CustomTime.getCustomTime(customTimeId);
            Assert.assertTrue(customTime != null);

            DayOfWeek dayOfWeek = (DayOfWeek) args.getSerializable("dayOfWeek");
            Assert.assertTrue(dayOfWeek != null);

            HourMinute hourMinute = customTime.getTimeByDay(dayOfWeek);

            return new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    //edit CustomTime
                }
            }, hourMinute.getHour(), hourMinute.getMinute(), DateFormat.is24HourFormat(getActivity()));
        }
    }
}
