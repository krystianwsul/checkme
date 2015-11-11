package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

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
        CustomTime customTime = CustomTime.getCustomTime(customTimeId);
        Assert.assertTrue(customTime != null);

        TextView customTimeName = (TextView) findViewById(R.id.custom_time_name);
        customTimeName.setText(customTime.getName());

        TextView timeSundayName = (TextView) findViewById(R.id.time_sunday_name);
        timeSundayName.setText(DayOfWeek.SUNDAY.toString());

        TextView timeSundayTime = (TextView) findViewById(R.id.time_sunday_time);
        timeSundayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.SUNDAY)));

        TextView timeMondayName = (TextView) findViewById(R.id.time_monday_name);
        timeMondayName.setText(DayOfWeek.MONDAY.toString());

        TextView timeMondayTime = (TextView) findViewById(R.id.time_monday_time);
        timeMondayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.MONDAY)));

        TextView timeTuesdayName = (TextView) findViewById(R.id.time_tuesday_name);
        timeTuesdayName.setText(DayOfWeek.TUESDAY.toString());

        TextView timeTuesdayTime = (TextView) findViewById(R.id.time_tuesday_time);
        timeTuesdayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.TUESDAY)));

        TextView timeWednesdayName = (TextView) findViewById(R.id.time_wednesday_name);
        timeWednesdayName.setText(DayOfWeek.WEDNESDAY.toString());

        TextView timeWednesdayTime = (TextView) findViewById(R.id.time_wednesday_time);
        timeWednesdayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.WEDNESDAY)));

        TextView timeThursdayName = (TextView) findViewById(R.id.time_thursday_name);
        timeThursdayName.setText(DayOfWeek.THURSDAY.toString());

        TextView timeThursdayTime = (TextView) findViewById(R.id.time_thursday_time);
        timeThursdayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.THURSDAY)));

        TextView timeFridayName = (TextView) findViewById(R.id.time_friday_name);
        timeFridayName.setText(DayOfWeek.FRIDAY.toString());

        TextView timeFridayTime = (TextView) findViewById(R.id.time_friday_time);
        timeFridayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.FRIDAY)));

        TextView timeSaturdayName = (TextView) findViewById(R.id.time_saturday_name);
        timeSaturdayName.setText(DayOfWeek.SATURDAY.toString());

        TextView timeSaturdayTime = (TextView) findViewById(R.id.time_saturday_time);
        timeSaturdayTime.setText(timeText(customTime.getTimeByDay(DayOfWeek.SATURDAY)));
    }

    private String timeText(HourMinute hourMinute) {
        if (hourMinute == null)
            return getResources().getString(R.string.none);
        else
            return hourMinute.toString();
    }
}
