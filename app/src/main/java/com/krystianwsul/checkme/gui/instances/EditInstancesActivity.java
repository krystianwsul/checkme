package com.krystianwsul.checkme.gui.instances;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.gui.MyCalendarFragment;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.EditInstancesLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class EditInstancesActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<EditInstancesLoader.Data> {
    private static final String INSTANCE_KEYS = "instanceKeys";

    private static final String DATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";
    private static final String INITIAL_HOUR_MINUTE_KEY = "initialHourMinute";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_FRAGMENT_TAG = "timeFragment";
    private static final String TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment";
    private static final String DISCARD_TAG = "discard";

    private Date mDate;
    private EditInstancesLoader.Data mData;

    private ActionBar mActionBar;

    private LinearLayout mEditInstanceLayout;
    private TextInputLayout mEditInstanceDateLayout;
    private TextView mEditInstanceDate;
    private TextInputLayout mEditInstanceTimeLayout;
    private TextView mEditInstanceTime;

    private Bundle mSavedInstanceState;

    private BroadcastReceiver mBroadcastReceiver;

    private TimePairPersist mTimePairPersist;

    private boolean mFirst = true;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(@NonNull CustomTimeKey customTimeKey) {
            Assert.assertTrue(mData != null);

            mTimePairPersist.setCustomTimeKey(customTimeKey);

            updateTimeText();

            updateError();
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mData != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mTimePairPersist.getHourMinute().getHour(), mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getSupportFragmentManager(), TIME_FRAGMENT_TAG);
        }

        @Override
        public void onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(EditInstancesActivity.this), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = (dialog, hourOfDay, minute) -> {
        Assert.assertTrue(mData != null);

        mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateTimeText();
        updateError();
    };

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = EditInstancesActivity.this::finish;

    private HourMinute mInitialHourMinute;

    public static Intent getIntent(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(instanceKeys.size() > 1);

        Intent intent = new Intent(context, EditInstancesActivity.class);
        intent.putParcelableArrayListExtra(INSTANCE_KEYS, instanceKeys);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_instance, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_edit_instance_save).setVisible(mData != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_instance_save:
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mData != null);

                if (!isValidDateTime())
                    break;

                DomainFactory.getDomainFactory(this).setInstancesDateTime(this, mData.DataId, mData.InstanceDatas.keySet(), mDate, mTimePairPersist.getTimePair());

                finish();
                break;
            case android.R.id.home:
                if (tryClose())
                    finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_instance);

        Toolbar toolbar = (Toolbar) findViewById(R.id.edit_instance_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        mActionBar.setTitle(null);

        mSavedInstanceState = savedInstanceState;

        mEditInstanceLayout = (LinearLayout) findViewById(R.id.edit_instance_layout);
        Assert.assertTrue(mEditInstanceLayout != null);

        mEditInstanceDateLayout = (TextInputLayout) findViewById(R.id.edit_instance_date_layout);
        Assert.assertTrue(mEditInstanceDateLayout != null);

        mEditInstanceDate = (TextView) findViewById(R.id.edit_instance_date);
        Assert.assertTrue(mEditInstanceDate != null);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = (dialog, year, monthOfYear, dayOfMonth) -> {
            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateDateText();
        };
        mEditInstanceDate.setOnClickListener(v -> {
            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mDate);
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getSupportFragmentManager(), DATE_FRAGMENT_TAG);
        });
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);

        mEditInstanceTimeLayout = (TextInputLayout) findViewById(R.id.edit_instance_time_layout);
        Assert.assertTrue(mEditInstanceTimeLayout != null);

        mEditInstanceTime = (TextView) findViewById(R.id.edit_instance_time);
        Assert.assertTrue(mEditInstanceTime != null);

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(DATE_KEY)) {
            mDate = mSavedInstanceState.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);

            Assert.assertTrue(mSavedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY));
            mTimePairPersist = mSavedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);

            Assert.assertTrue(mSavedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY));
            mInitialHourMinute = mSavedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY);
            Assert.assertTrue(mInitialHourMinute != null);
        }

        getSupportLoaderManager().initLoader(0, null, this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mData != null)
                    updateError();
            }
        };

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        if (mData != null)
            updateError();
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

            Assert.assertTrue(mTimePairPersist != null);
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist);

            Assert.assertTrue(mInitialHourMinute != null);
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, mInitialHourMinute);
        }
    }

    @Override
    public Loader<EditInstancesLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEYS));
        ArrayList<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCE_KEYS);

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(instanceKeys.size() > 1);

        return new EditInstancesLoader(this, instanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<EditInstancesLoader.Data> loader, final EditInstancesLoader.Data data) {
        mData = data;

        mEditInstanceLayout.setVisibility(View.VISIBLE);

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(DATE_KEY))) {
            Assert.assertTrue(mDate == null);
            Assert.assertTrue(mTimePairPersist == null);
            Assert.assertTrue(mInitialHourMinute == null);
            Assert.assertTrue(!mData.InstanceDatas.isEmpty());

            mFirst = false;

            mDate = Stream.of(mData.InstanceDatas.values())
                    .map(instanceData -> instanceData.InstanceDate)
                    .sorted()
                    .findFirst().get();
            Assert.assertTrue(mDate != null);

            mTimePairPersist = new TimePairPersist();

            mInitialHourMinute = mTimePairPersist.getHourMinute();
        }

        mActionBar.setTitle(Stream.of(mData.InstanceDatas.values())
            .map(instanceData -> instanceData.Name)
            .collect(Collectors.joining(", ")));

        invalidateOptionsMenu();

        updateDateText();

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(TIME_FRAGMENT_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        mEditInstanceTime.setOnClickListener(v -> {
            Assert.assertTrue(mData != null);
            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mData.CustomTimeDatas.values())
                    .filter(customTimeData -> customTimeData.mCustomTimeKey.mLocalCustomTimeId != null)
                    .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDate.getDayOfWeek()))
                    .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.mCustomTimeKey, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")"))
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
    public void onLoaderReset(Loader<EditInstancesLoader.Data> loader) {
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mEditInstanceDate != null);

        mEditInstanceDate.setText(mDate.getDisplayText(this));

        updateTimeText();

        updateError();
    }

    @SuppressLint("SetTextI18n")
    private void updateTimeText() {
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mEditInstanceTime != null);
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mDate != null);

        if (mTimePairPersist.getCustomTimeKey() != null) {
            EditInstancesLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeKey());
            Assert.assertTrue(customTimeData != null);

            mEditInstanceTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")");
        } else {
            mEditInstanceTime.setText(mTimePairPersist.getHourMinute().toString());
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isValidDate() {
        if (mData != null) {
            return (mDate.compareTo(Date.today()) >= 0);
        } else {
            return false;
        }
    }

    private boolean isValidDateTime() {
        if (mData != null) {
            HourMinute hourMinute;
            if (mTimePairPersist.getCustomTimeKey() != null) {
                if (!mData.CustomTimeDatas.containsKey(mTimePairPersist.getCustomTimeKey()))
                    return false; //cached data doesn't contain new custom time

                hourMinute = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeKey()).HourMinutes.get(mDate.getDayOfWeek());
            } else {
                hourMinute = mTimePairPersist.getHourMinute();
            }

            return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
        } else {
            return false;
        }
    }

    private void updateError() {
        if (isValidDate()) {
            mEditInstanceDateLayout.setError(null);
            mEditInstanceTimeLayout.setError(isValidDateTime() ? null : getString(R.string.error_time));
        } else {
            mEditInstanceDateLayout.setError(getString(R.string.error_date));
            mEditInstanceTimeLayout.setError(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    private boolean tryClose() {
        if (dataChanged()) {
            DiscardDialogFragment discardDialogFragment = DiscardDialogFragment.newInstance();
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
            discardDialogFragment.show(getSupportFragmentManager(), DISCARD_TAG);

            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean dataChanged() {
        if (mData == null)
            return false;

        Assert.assertTrue(!mData.InstanceDatas.isEmpty());

        Date date = Stream.of(mData.InstanceDatas.values())
                .map(instanceData -> instanceData.InstanceDate)
                .sorted()
                .findFirst().get();
        Assert.assertTrue(date != null);

        if (!date.equals(mDate))
            return true;

        if (mTimePairPersist.getCustomTimeKey() != null)
            return true;

        Assert.assertTrue(mInitialHourMinute != null);

        if (!mTimePairPersist.getHourMinute().equals(mInitialHourMinute))
            return true;

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);
        Assert.assertTrue(mTimePairPersist != null);

        if (resultCode > 0)
            mTimePairPersist.setCustomTimeKey(new CustomTimeKey(resultCode));
    }
}
