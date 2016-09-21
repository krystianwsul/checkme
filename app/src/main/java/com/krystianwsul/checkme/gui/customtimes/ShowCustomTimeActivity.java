package com.krystianwsul.checkme.gui.customtimes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowCustomTimeLoader.Data> {
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String NEW_KEY = "new";

    private static final String HOUR_MINUTE_SUNDAY_KEY = "hourMinuteSunday";
    private static final String HOUR_MINUTE_MONDAY_KEY = "hourMinuteMonday";
    private static final String HOUR_MINUTE_TUESDAY_KEY = "hourMinuteTuesday";
    private static final String HOUR_MINUTE_WEDNESDAY_KEY = "hourMinuteWednesday";
    private static final String HOUR_MINUTE_THURSDAY_KEY = "hourMinuteThursday";
    private static final String HOUR_MINUTE_FRIDAY_KEY = "hourMinuteFriday";
    private static final String HOUR_MINUTE_SATURDAY_KEY = "hourMinuteSaturday";

    private static final String EDITED_DAY_OF_WEEK_KEY = "editedDayOfWeek";

    private static final String TIME_PICKER_TAG = "timePicker";
    private static final String DISCARD_TAG = "discard";

    public static final int CREATE_CUSTOM_TIME_REQUEST_CODE = 1;

    private Integer mCustomTimeId;
    private ShowCustomTimeLoader.Data mData;

    private final HashMap<DayOfWeek, TextView> mTimeViews = new HashMap<>();
    private HashMap<DayOfWeek, HourMinute> mHourMinutes;

    private DayOfWeek mEditedDayOfWeek = null;

    private TextInputLayout mToolbarLayout;
    private EditText mToolbarEditText;

    private Bundle mSavedInstanceState;

    private static final HourMinute sDefaultHourMinute = new HourMinute(9, 0);

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = ShowCustomTimeActivity.this::finish;

    private LinearLayout mShowCustomTimeContainer;

    public static Intent getEditIntent(int customTimeId, Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(CUSTOM_TIME_ID_KEY, customTimeId);
        return intent;
    }

    public static Intent getCreateIntent(Context context) {
        Intent intent = new Intent(context, ShowCustomTimeActivity.class);
        intent.putExtra(NEW_KEY, true);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_time, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Assert.assertTrue(mToolbarEditText != null);

        menu.findItem(R.id.action_custom_time_save).setVisible((mCustomTimeId == null) || (mData != null));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_custom_time_save:
                Assert.assertTrue(!mHourMinutes.isEmpty());

                updateError();

                String name = mToolbarEditText.getText().toString().trim();
                if (TextUtils.isEmpty(name))
                    break;

                getSupportLoaderManager().destroyLoader(0);

                if (mData != null) {
                    DomainFactory.getDomainFactory(ShowCustomTimeActivity.this).updateCustomTime(ShowCustomTimeActivity.this, mData.DataId, mData.Id, name, mHourMinutes);
                } else {
                    int customTimeId = DomainFactory.getDomainFactory(ShowCustomTimeActivity.this).createCustomTime(ShowCustomTimeActivity.this, name, mHourMinutes);
                    Assert.assertTrue(customTimeId > 0);

                    setResult(customTimeId);
                }

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
        setContentView(R.layout.activity_show_custom_time);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Assert.assertTrue(actionBar != null);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        mSavedInstanceState = savedInstanceState;

        mToolbarLayout = (TextInputLayout) findViewById(R.id.toolbar_layout);
        Assert.assertTrue(mToolbarLayout != null);

        mToolbarEditText = (EditText) findViewById(R.id.toolbar_edit_text);
        Assert.assertTrue(mToolbarEditText != null);

        mShowCustomTimeContainer = (LinearLayout) findViewById(R.id.show_custom_time_container);
        Assert.assertTrue(mShowCustomTimeContainer != null);

        initializeDay(DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time);
        initializeDay(DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time);
        initializeDay(DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time);
        initializeDay(DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time);
        initializeDay(DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time);
        initializeDay(DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time);
        initializeDay(DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time);

        Intent intent = getIntent();

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(HOUR_MINUTE_SUNDAY_KEY)) {
            Assert.assertTrue(mSavedInstanceState.containsKey(EDITED_DAY_OF_WEEK_KEY));
            Assert.assertTrue(mHourMinutes == null);
            mHourMinutes = new HashMap<>();

            extractKey(HOUR_MINUTE_SUNDAY_KEY, DayOfWeek.SUNDAY);
            extractKey(HOUR_MINUTE_MONDAY_KEY, DayOfWeek.MONDAY);
            extractKey(HOUR_MINUTE_TUESDAY_KEY, DayOfWeek.TUESDAY);
            extractKey(HOUR_MINUTE_WEDNESDAY_KEY, DayOfWeek.WEDNESDAY);
            extractKey(HOUR_MINUTE_THURSDAY_KEY, DayOfWeek.THURSDAY);
            extractKey(HOUR_MINUTE_FRIDAY_KEY, DayOfWeek.FRIDAY);
            extractKey(HOUR_MINUTE_SATURDAY_KEY, DayOfWeek.SATURDAY);

            mEditedDayOfWeek = (DayOfWeek) mSavedInstanceState.getSerializable(EDITED_DAY_OF_WEEK_KEY);

            updateGui();
        } else {
            if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
                Assert.assertTrue(!intent.hasExtra(NEW_KEY));
            } else {
                Assert.assertTrue(intent.hasExtra(NEW_KEY));
                Assert.assertTrue(mHourMinutes == null);
                mHourMinutes = new HashMap<>();

                for (DayOfWeek dayOfWeek : DayOfWeek.values())
                    mHourMinutes.put(dayOfWeek, sDefaultHourMinute);

                updateGui();
            }
        }

        if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(NEW_KEY));

            mCustomTimeId = intent.getIntExtra(CUSTOM_TIME_ID_KEY, -1);
            Assert.assertTrue(mCustomTimeId != -1);

            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            Assert.assertTrue(intent.hasExtra(NEW_KEY));
        }

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
    }

    @Override
    protected void onResume() {
        MyCrashlytics.log("ShowCustomTimeActivity.onResume");

        super.onResume();
    }

    private void extractKey(String key, DayOfWeek dayOfWeek) {
        Assert.assertTrue(mSavedInstanceState != null);
        Assert.assertTrue(!TextUtils.isEmpty(key));
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(mHourMinutes != null);

        Assert.assertTrue(mSavedInstanceState.containsKey(key));

        HourMinute hourMinute = mSavedInstanceState.getParcelable(key);
        Assert.assertTrue(hourMinute != null);

        mHourMinutes.put(dayOfWeek, hourMinute);
    }

    private void initializeDay(final DayOfWeek dayOfWeek, int nameId, int timeId) {
        Assert.assertTrue(dayOfWeek != null);

        TextView timeName = (TextView) findViewById(nameId);
        Assert.assertTrue(timeName != null);

        timeName.setText(dayOfWeek.toString());

        TextView timeView = (TextView) findViewById(timeId);
        Assert.assertTrue(timeView != null);

        mTimeViews.put(dayOfWeek, timeView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mHourMinutes != null) {
            outState.putParcelable(HOUR_MINUTE_SUNDAY_KEY, mHourMinutes.get(DayOfWeek.SUNDAY));
            outState.putParcelable(HOUR_MINUTE_MONDAY_KEY, mHourMinutes.get(DayOfWeek.MONDAY));
            outState.putParcelable(HOUR_MINUTE_TUESDAY_KEY, mHourMinutes.get(DayOfWeek.TUESDAY));
            outState.putParcelable(HOUR_MINUTE_WEDNESDAY_KEY, mHourMinutes.get(DayOfWeek.WEDNESDAY));
            outState.putParcelable(HOUR_MINUTE_THURSDAY_KEY, mHourMinutes.get(DayOfWeek.THURSDAY));
            outState.putParcelable(HOUR_MINUTE_FRIDAY_KEY, mHourMinutes.get(DayOfWeek.FRIDAY));
            outState.putParcelable(HOUR_MINUTE_SATURDAY_KEY, mHourMinutes.get(DayOfWeek.SATURDAY));

            outState.putSerializable(EDITED_DAY_OF_WEEK_KEY, mEditedDayOfWeek);
        }
    }

    @Override
    public Loader<ShowCustomTimeLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowCustomTimeLoader(this, mCustomTimeId);
    }

    private void updateGui() {
        Assert.assertTrue(mHourMinutes != null);

        mToolbarLayout.setVisibility(View.VISIBLE);
        mShowCustomTimeContainer.setVisibility(View.VISIBLE);

        final RadialTimePickerDialogFragment.OnTimeSetListener onTimeSetListener = (dialog, hourOfDay, minute) -> {
            Assert.assertTrue(mEditedDayOfWeek != null);
            Assert.assertTrue(mTimeViews.containsKey(mEditedDayOfWeek));
            Assert.assertTrue(mHourMinutes.containsKey(mEditedDayOfWeek));

            HourMinute hourMinute = new HourMinute(hourOfDay, minute);
            mHourMinutes.put(mEditedDayOfWeek, hourMinute);
            mTimeViews.get(mEditedDayOfWeek).setText(hourMinute.toString());

            mEditedDayOfWeek = null;
        };

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            TextView timeView = mTimeViews.get(dayOfWeek);
            Assert.assertTrue(timeView != null);

            HourMinute hourMinute = mHourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            timeView.setText(hourMinute.toString());

            final DayOfWeek finalDayOfWeek = dayOfWeek;
            timeView.setOnClickListener(v -> {
                mEditedDayOfWeek = finalDayOfWeek;

                HourMinute currHourMinute = mHourMinutes.get(finalDayOfWeek);

                RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
                radialTimePickerDialogFragment.setStartTime(currHourMinute.getHour(), currHourMinute.getMinute());
                radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);
                radialTimePickerDialogFragment.show(getSupportFragmentManager(), TIME_PICKER_TAG);
            });
        }

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(onTimeSetListener);

        mToolbarEditText.addTextChangedListener(new TextWatcher() {
            private boolean mSkip = (mSavedInstanceState != null);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mSkip) {
                    mSkip = false;
                    return;
                }

                updateError();
            }
        });
    }

    @Override
    public void onLoadFinished(Loader<ShowCustomTimeLoader.Data> loader, ShowCustomTimeLoader.Data data) {
        mData = data;

        if (mSavedInstanceState == null || !mSavedInstanceState.containsKey(HOUR_MINUTE_SUNDAY_KEY)) {
            Assert.assertTrue(mHourMinutes == null);
            mHourMinutes = new HashMap<>();

            mToolbarEditText.setText(mData.Name);

            for (DayOfWeek dayOfWeek : DayOfWeek.values())
                mHourMinutes.put(dayOfWeek, mData.HourMinutes.get(dayOfWeek));

            updateGui();
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ShowCustomTimeLoader.Data> data) {
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

    private boolean dataChanged() {
        if (mCustomTimeId == null) {
            Assert.assertTrue(mData == null);

            if (!TextUtils.isEmpty(mToolbarEditText.getText()))
                return true;

            for (DayOfWeek dayOfWeek : DayOfWeek.values())
                if (!mHourMinutes.get(dayOfWeek).equals(sDefaultHourMinute))
                    return true;

            return false;
        } else {
            if (mData == null)
                return false;

            if (!mToolbarEditText.getText().toString().equals(mData.Name))
                return true;

            for (DayOfWeek dayOfWeek : DayOfWeek.values())
                if (!mHourMinutes.get(dayOfWeek).equals(mData.HourMinutes.get(dayOfWeek)))
                    return true;

            return false;
        }
    }

    private void updateError() {
        if (TextUtils.isEmpty(mToolbarEditText.getText())) {
            mToolbarLayout.setError(getString(R.string.nameError));
            mToolbarLayout.setPadding(0, 0, 0, 0);
        } else {
            mToolbarLayout.setError(null);
        }
    }
}