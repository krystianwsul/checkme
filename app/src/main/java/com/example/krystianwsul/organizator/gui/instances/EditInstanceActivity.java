package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TimePickerView;
import com.example.krystianwsul.organizator.loaders.EditInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.HashMap;

public class EditInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<EditInstanceLoader.Data> {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String DATE_KEY = "date";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_FRAGMENT_TAG = "timeFragment";

    private Date mDate;
    private EditInstanceLoader.Data mData;

    private TextView mEditInstanceDate;
    private TimePickerView mEditInstanceTimePickerView;
    private Bundle mSavedInstanceState;
    private TextView mEditInstanceName;
    private Button mEditInstanceSave;

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Intent intent = new Intent(context, EditInstanceActivity.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_instance);

        mSavedInstanceState = savedInstanceState;

        mEditInstanceName = (TextView) findViewById(R.id.edit_instance_name);

        mEditInstanceDate = (TextView) findViewById(R.id.edit_instance_date);
        Assert.assertTrue(mEditInstanceDate != null);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = new CalendarDatePickerDialogFragment.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                mDate = new Date(year, monthOfYear + 1, dayOfMonth);
                updateDateText();
            }
        };
        mEditInstanceDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = new CalendarDatePickerDialogFragment();
                calendarDatePickerDialogFragment.setDateRange(new MonthAdapter.CalendarDay(Calendar.getInstance()), null);
                calendarDatePickerDialogFragment.setPreselectedDate(mDate.getYear(), mDate.getMonth() - 1, mDate.getDay());
                calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
                calendarDatePickerDialogFragment.show(getSupportFragmentManager(), DATE_FRAGMENT_TAG);
            }
        });
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);

        mEditInstanceTimePickerView = (TimePickerView) findViewById(R.id.edit_instance_timepickerview);

        mEditInstanceSave = (Button) findViewById(R.id.edit_instance_save);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mEditInstanceDate != null);

        mEditInstanceDate.setText(mDate.getDisplayText(this));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mDate != null);

        outState.putParcelable(DATE_KEY, mDate);
    }

    @Override
    public Loader<EditInstanceLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);

        return new EditInstanceLoader(this, instanceKey);
    }

    @Override
    public void onLoadFinished(Loader<EditInstanceLoader.Data> loader, final EditInstanceLoader.Data data) {
        mData = data;

        HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
        for (EditInstanceLoader.CustomTimeData customTimeData : mData.CustomTimeDatas.values())
            customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name));
        mEditInstanceTimePickerView.setCustomTimeDatas(customTimeDatas);

        if (mSavedInstanceState != null) {
            Assert.assertTrue(mSavedInstanceState.containsKey(DATE_KEY));

            mDate = mSavedInstanceState.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);
        } else {
            mDate = mData.InstanceDate;
            mEditInstanceTimePickerView.setTimePair(mData.InstanceTimePair);
        }

        mEditInstanceName.setText(mData.Name);

        updateDateText();

        final RadialTimePickerDialogFragment.OnTimeSetListener onTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                Assert.assertTrue(mEditInstanceTimePickerView != null);
                mEditInstanceTimePickerView.setHourMinute(new HourMinute(hourOfDay, minute));
            }
        };
        mEditInstanceTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(int customTimeId) {
            }

            @Override
            public void onHourMinuteSelected(HourMinute hourMinute) {
            }

            @Override
            public void onHourMinuteClick() {
                RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
                HourMinute startTime = mEditInstanceTimePickerView.getHourMinute();
                radialTimePickerDialogFragment.setStartTime(startTime.getHour(), startTime.getMinute());
                radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);
                radialTimePickerDialogFragment.show(getSupportFragmentManager(), TIME_FRAGMENT_TAG);
            }
        });
        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(TIME_FRAGMENT_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);

        mEditInstanceSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mEditInstanceTimePickerView != null);
                Assert.assertTrue(mData != null);

                HourMinute hourMinute = mEditInstanceTimePickerView.getHourMinute();
                Integer customTimeId = mEditInstanceTimePickerView.getCustomTimeId();
                Assert.assertTrue((hourMinute == null) != (customTimeId == null));
                if (hourMinute == null) {
                    EditInstanceLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(customTimeId);
                    Assert.assertTrue(customTimeData != null);

                    hourMinute = customTimeData.HourMinutes.get(mDate.getDayOfWeek());
                    Assert.assertTrue(hourMinute != null);
                }

                if ((new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) <= 0)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.invalid_time_message));
                    messageDialogFragment.show(getSupportFragmentManager(), "invalid_time");
                    return;
                }

                DomainFactory.getDomainFactory(EditInstanceActivity.this).setInstanceDateTime(mData.DataId, mData.InstanceKey, mDate, new TimePair(mEditInstanceTimePickerView.getCustomTimeId(), mEditInstanceTimePickerView.getHourMinute()));

                TickService.startService(EditInstanceActivity.this);

                finish();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<EditInstanceLoader.Data> loader) {
    }
}
