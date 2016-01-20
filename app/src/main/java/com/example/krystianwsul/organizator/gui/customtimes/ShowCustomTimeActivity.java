package com.example.krystianwsul.organizator.gui.customtimes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.tasks.HourMinutePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener {
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String NEW_KEY = "new";

    private static final String HOUR_MINUTE_SUNDAY_KEY = "hourMinuteSunday";
    private static final String HOUR_MINUTE_MONDAY_KEY = "hourMinuteMonday";
    private static final String HOUR_MINUTE_TUESDAY_KEY = "hourMinuteTuesday";
    private static final String HOUR_MINUTE_WEDNESDAY_KEY = "hourMinuteWednesday";
    private static final String HOUR_MINUTE_THURSDAY_KEY = "hourMinuteThursday";
    private static final String HOUR_MINUTE_FRIDAY_KEY = "hourMinuteFriday";
    private static final String HOUR_MINUTE_SATURDAY_KEY = "hourMinuteSaturday";

    public static Intent getEditIntent(CustomTime customTime, Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(CUSTOM_TIME_ID_KEY, customTime.getId());
        return intent;
    }

    public static Intent getCreateIntent(Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(NEW_KEY, true);
        return intent;
    }

    private CustomTime mCustomTime;
    private final HashMap<DayOfWeek, TextView> mTimeViews = new HashMap<>();
    private final HashMap<DayOfWeek, HourMinute> mHourMinutes = new HashMap<>();

    private DayOfWeek editedDayOfWeek = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_time);

        final EditText customTimeName = (EditText) findViewById(R.id.custom_time_name);

        initializeDay(DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time);
        initializeDay(DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time);
        initializeDay(DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time);
        initializeDay(DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time);
        initializeDay(DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time);
        initializeDay(DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time);
        initializeDay(DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time);

        final DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        Intent intent = getIntent();
        if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(NEW_KEY));

            int customTimeId = intent.getIntExtra(CUSTOM_TIME_ID_KEY, -1);
            Assert.assertTrue(customTimeId != -1);

            mCustomTime = domainFactory.getCustomTimeFactory().getCustomTime(customTimeId);
            Assert.assertTrue(mCustomTime != null);

            customTimeName.setText(mCustomTime.getName());
        } else {
            Assert.assertTrue(intent.hasExtra(NEW_KEY));
        }

        if (savedInstanceState != null) {
            extractKey(savedInstanceState, HOUR_MINUTE_SUNDAY_KEY, DayOfWeek.SUNDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_MONDAY_KEY, DayOfWeek.MONDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_TUESDAY_KEY, DayOfWeek.TUESDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_WEDNESDAY_KEY, DayOfWeek.WEDNESDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_THURSDAY_KEY, DayOfWeek.THURSDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_FRIDAY_KEY, DayOfWeek.FRIDAY);
            extractKey(savedInstanceState, HOUR_MINUTE_SATURDAY_KEY, DayOfWeek.SATURDAY);
        } else {
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                if (mCustomTime != null)
                    mHourMinutes.put(dayOfWeek, mCustomTime.getHourMinute(dayOfWeek));
                else
                    mHourMinutes.put(dayOfWeek, new HourMinute(9, 0));
            }
        }

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            TextView timeView = mTimeViews.get(dayOfWeek);
            Assert.assertTrue(timeView != null);

            HourMinute hourMinute = mHourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            timeView.setText(hourMinute.toString());

            final DayOfWeek finalDayOfWeek = dayOfWeek;
            timeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editedDayOfWeek = finalDayOfWeek;

                    HourMinute hourMinute = mHourMinutes.get(finalDayOfWeek);

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(ShowCustomTimeActivity.this, hourMinute);
                    hourMinutePickerFragment.show(fragmentManager, "tag");
                }
            });
        }

        Button customTimeSave = (Button) findViewById(R.id.custom_time_save);
        customTimeSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = customTimeName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.task_name_toast));
                    messageDialogFragment.show(getSupportFragmentManager(), "empty_name");
                    return;
                }

                if (mCustomTime != null) {
                    mCustomTime.setName(name);

                    for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                        HourMinute hourMinute = mHourMinutes.get(dayOfWeek);
                        Assert.assertTrue(hourMinute != null);

                        if (hourMinute.compareTo(mCustomTime.getHourMinute(dayOfWeek)) != 0)
                            mCustomTime.setHourMinute(dayOfWeek, hourMinute);
                    }
                    domainFactory.getPersistenceManager().save();
                } else {
                    domainFactory.getCustomTimeFactory().createCustomTime(name, mHourMinutes);
                }

                finish();
            }
        });
    }

    private void extractKey(Bundle bundle, String key, DayOfWeek dayOfWeek) {
        Assert.assertTrue(bundle != null);
        Assert.assertTrue(!TextUtils.isEmpty(key));
        Assert.assertTrue(dayOfWeek != null);

        Assert.assertTrue(bundle.containsKey(key));

        HourMinute hourMinute = bundle.getParcelable(key);
        Assert.assertTrue(hourMinute != null);

        mHourMinutes.put(dayOfWeek, hourMinute);
    }

    private void initializeDay(final DayOfWeek dayOfWeek, int nameId, int timeId) {
        Assert.assertTrue(dayOfWeek != null);

        TextView timeName = (TextView) findViewById(nameId);
        timeName.setText(dayOfWeek.toString());

        TextView timeView = (TextView) findViewById(timeId);
        mTimeViews.put(dayOfWeek, timeView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(HOUR_MINUTE_SUNDAY_KEY, mHourMinutes.get(DayOfWeek.SUNDAY));
        outState.putParcelable(HOUR_MINUTE_MONDAY_KEY, mHourMinutes.get(DayOfWeek.MONDAY));
        outState.putParcelable(HOUR_MINUTE_TUESDAY_KEY, mHourMinutes.get(DayOfWeek.TUESDAY));
        outState.putParcelable(HOUR_MINUTE_WEDNESDAY_KEY, mHourMinutes.get(DayOfWeek.WEDNESDAY));
        outState.putParcelable(HOUR_MINUTE_THURSDAY_KEY, mHourMinutes.get(DayOfWeek.THURSDAY));
        outState.putParcelable(HOUR_MINUTE_FRIDAY_KEY, mHourMinutes.get(DayOfWeek.FRIDAY));
        outState.putParcelable(HOUR_MINUTE_SATURDAY_KEY, mHourMinutes.get(DayOfWeek.SATURDAY));
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(editedDayOfWeek != null);
        Assert.assertTrue(mTimeViews.containsKey(editedDayOfWeek));
        Assert.assertTrue(mHourMinutes.containsKey(editedDayOfWeek));

        //mCustomTime.setHourMinute(editedDayOfWeek, hourMinute);

        mHourMinutes.put(editedDayOfWeek, hourMinute);
        mTimeViews.get(editedDayOfWeek).setText(hourMinute.toString());
    }
}