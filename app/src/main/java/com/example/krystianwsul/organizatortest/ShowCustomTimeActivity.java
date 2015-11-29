package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeActivity extends AppCompatActivity implements HourMinutePickerFragment.TimePickerFragmentListener {
    private static final String INTENT_KEY = "customTimeId";

    public static Intent getIntent(CustomTime customTime, Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(INTENT_KEY, customTime.getId());
        return intent;
    }

    private CustomTime mCustomTime;
    private HashMap<DayOfWeek, TextView> mTimes = new HashMap<>();

    private DayOfWeek editedDayOfWeek = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_time);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        int customTimeId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(customTimeId != -1);
        mCustomTime = CustomTimeFactory.getInstance().getCustomTime(customTimeId);
        Assert.assertTrue(mCustomTime != null);

        TextView customTimeName = (TextView) findViewById(R.id.custom_time_name);
        customTimeName.setText(mCustomTime.getName());

        initializeDay(DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time);
        initializeDay(DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time);
        initializeDay(DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time);
        initializeDay(DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time);
        initializeDay(DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time);
        initializeDay(DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time);
        initializeDay(DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time);
    }

    private void initializeDay(final DayOfWeek dayOfWeek, int nameId, int timeId) {
        Assert.assertTrue(dayOfWeek != null);

        TextView timeSundayName = (TextView) findViewById(nameId);
        timeSundayName.setText(dayOfWeek.toString());

        TextView timeSundayTime = (TextView) findViewById(timeId);
        timeSundayTime.setText(mCustomTime.getHourMinute(dayOfWeek).toString());
        timeSundayTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editedDayOfWeek = dayOfWeek;
                FragmentManager fragmentManager = getSupportFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(mCustomTime.getHourMinute(dayOfWeek));
                hourMinutePickerFragment.show(fragmentManager, "tag");
            }
        });
        mTimes.put(dayOfWeek, timeSundayTime);
    }

    public void onTimePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(editedDayOfWeek != null);
        Assert.assertTrue(mTimes.containsKey(editedDayOfWeek));

        mCustomTime.setHourMinute(editedDayOfWeek, hourMinute);
        mTimes.get(editedDayOfWeek).setText(mCustomTime.getHourMinute(editedDayOfWeek).toString());
    }
}