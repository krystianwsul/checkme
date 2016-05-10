package com.example.krystianwsul.organizator.gui.instances;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.TimeDialogFragment;
import com.example.krystianwsul.organizator.loaders.EditInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;

public class EditInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<EditInstanceLoader.Data> {
    private static final String INSTANCE_KEY = "instanceKey";

    private static final String DATE_KEY = "date";
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String HOUR_MINUTE_KEY = "hourMinute";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_FRAGMENT_TAG = "timeFragment";
    private static final String TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment";

    private Date mDate;
    private EditInstanceLoader.Data mData;

    private TextView mEditInstanceDate;
    private Bundle mSavedInstanceState;
    private TextView mEditInstanceName;
    private Button mEditInstanceSave;
    private TextView mEditInstanceTime;

    private BroadcastReceiver mBroadcastReceiver;

    private Integer mCustomTimeId;
    private HourMinute mHourMinute = HourMinute.getNextHour();

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mData != null);

            mCustomTimeId = customTimeId;

            updateTimeText();

            setValidTime();
        }

        @Override
        public void onHourMinuteSelected() {
            Assert.assertTrue(mData != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mHourMinute.getHour(), mHourMinute.getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getSupportFragmentManager(), TIME_FRAGMENT_TAG);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = (dialog, hourOfDay, minute) -> {
        Assert.assertTrue(mData != null);

        mCustomTimeId = null;
        mHourMinute = new HourMinute(hourOfDay, minute);

        updateTimeText();

        setValidTime();
    };

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
        Assert.assertTrue(mEditInstanceName != null);

        mEditInstanceDate = (TextView) findViewById(R.id.edit_instance_date);
        Assert.assertTrue(mEditInstanceDate != null);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = (dialog, year, monthOfYear, dayOfMonth) -> {
            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateDateText();
        };
        mEditInstanceDate.setOnClickListener(v -> {
            CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = new CalendarDatePickerDialogFragment();
            calendarDatePickerDialogFragment.setDateRange(new MonthAdapter.CalendarDay(Calendar.getInstance()), null);
            calendarDatePickerDialogFragment.setPreselectedDate(mDate.getYear(), mDate.getMonth() - 1, mDate.getDay());
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getSupportFragmentManager(), DATE_FRAGMENT_TAG);
        });
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);

        mEditInstanceSave = (Button) findViewById(R.id.edit_instance_save);
        Assert.assertTrue(mEditInstanceSave != null);

        mEditInstanceTime = (TextView) findViewById(R.id.edit_instance_time);
        Assert.assertTrue(mEditInstanceTime != null);

        getSupportLoaderManager().initLoader(0, null, this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setValidTime();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        setValidTime();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mDate != null);
            outState.putParcelable(DATE_KEY, mDate);

            if (mCustomTimeId != null)
                outState.putInt(CUSTOM_TIME_ID_KEY, mCustomTimeId);

            Assert.assertTrue(mHourMinute != null);
            outState.putParcelable(HOUR_MINUTE_KEY, mHourMinute);
        }
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

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(DATE_KEY)) {
            mDate = mSavedInstanceState.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);

            Assert.assertTrue(mSavedInstanceState.containsKey(HOUR_MINUTE_KEY));
            mHourMinute = mSavedInstanceState.getParcelable(HOUR_MINUTE_KEY);
            Assert.assertTrue(mHourMinute != null);

            if (mSavedInstanceState.containsKey(CUSTOM_TIME_ID_KEY)) {
                mCustomTimeId = mSavedInstanceState.getInt(CUSTOM_TIME_ID_KEY, -1);
                Assert.assertTrue(mCustomTimeId != -1);
            }
        } else {
            mDate = mData.InstanceDate;

            if (mData.InstanceTimePair.CustomTimeId != null) {
                Assert.assertTrue(mData.InstanceTimePair.HourMinute == null);

                mCustomTimeId = mData.InstanceTimePair.CustomTimeId;
            } else {
                Assert.assertTrue(mData.InstanceTimePair.HourMinute != null);
                Assert.assertTrue(mCustomTimeId == null);

                mHourMinute = mData.InstanceTimePair.HourMinute;
            }
        }

        mEditInstanceName.setText(mData.Name);

        updateDateText();

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(TIME_FRAGMENT_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        mEditInstanceSave.setOnClickListener(v -> {
            Assert.assertTrue(mDate != null);
            Assert.assertTrue(mData != null);

            TimePair timePair;
            if (mCustomTimeId != null)
                timePair = new TimePair(mCustomTimeId);
            else
                timePair = new TimePair(mHourMinute);

            DomainFactory.getDomainFactory(EditInstanceActivity.this).setInstanceDateTime(mData.DataId, mData.InstanceKey, mDate, timePair);

            TickService.startService(EditInstanceActivity.this);

            finish();
        });

        mEditInstanceTime.setOnClickListener(v -> {
            Assert.assertTrue(mDate != null);
            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mData.CustomTimeDatas.values())
                    .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")"))
                    .collect(Collectors.toList()));

            TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
            Assert.assertTrue(timeDialogFragment != null);

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

            timeDialogFragment.show(getSupportFragmentManager(), TIME_DIALOG_FRAGMENT_TAG);
        });

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getSupportFragmentManager().findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);
    }

    @Override
    public void onLoaderReset(Loader<EditInstanceLoader.Data> loader) {
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mEditInstanceDate != null);

        mEditInstanceDate.setText(mDate.getDisplayText(this));

        updateTimeText();

        setValidTime();
    }

    @SuppressLint("SetTextI18n")
    private void updateTimeText() {
        Assert.assertTrue(mHourMinute != null);
        Assert.assertTrue(mEditInstanceTime != null);
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mDate != null);

        if (mCustomTimeId != null) {
            EditInstanceLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(mCustomTimeId);
            Assert.assertTrue(customTimeData != null);

            mEditInstanceTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mEditInstanceTime.setText(mHourMinute.toString());
        }
    }

    private void setValidTime() {
        boolean valid;

        if (mData != null) {
            HourMinute hourMinute;

            if (mCustomTimeId != null)
                hourMinute = mData.CustomTimeDatas.get(mCustomTimeId).HourMinutes.get(mDate.getDayOfWeek());
            else
                hourMinute = mHourMinute;

            valid = (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
        } else {
            valid = false;
        }
        mEditInstanceSave.setEnabled(valid);
    }
}
