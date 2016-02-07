package com.example.krystianwsul.organizator.gui.customtimes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import com.example.krystianwsul.organizator.loaders.ShowCustomTimeLoader;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, LoaderManager.LoaderCallbacks<ShowCustomTimeLoader.Data> {
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

    private Integer mCustomTimeId;
    private ShowCustomTimeLoader.Data mData;

    private final HashMap<DayOfWeek, TextView> mTimeViews = new HashMap<>();
    private final HashMap<DayOfWeek, HourMinute> mHourMinutes = new HashMap<>();

    private DayOfWeek editedDayOfWeek = null;

    private EditText mCustomTimeName;
    private Button mCustomTimeSave;

    private Bundle mSavedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_time);

        mSavedInstanceState = savedInstanceState;

        mCustomTimeName = (EditText) findViewById(R.id.custom_time_name);
        mCustomTimeSave = (Button) findViewById(R.id.custom_time_save);

        initializeDay(DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time);
        initializeDay(DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time);
        initializeDay(DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time);
        initializeDay(DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time);
        initializeDay(DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time);
        initializeDay(DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time);
        initializeDay(DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time);

        if (mSavedInstanceState != null) {
            extractKey(HOUR_MINUTE_SUNDAY_KEY, DayOfWeek.SUNDAY);
            extractKey(HOUR_MINUTE_MONDAY_KEY, DayOfWeek.MONDAY);
            extractKey(HOUR_MINUTE_TUESDAY_KEY, DayOfWeek.TUESDAY);
            extractKey(HOUR_MINUTE_WEDNESDAY_KEY, DayOfWeek.WEDNESDAY);
            extractKey(HOUR_MINUTE_THURSDAY_KEY, DayOfWeek.THURSDAY);
            extractKey(HOUR_MINUTE_FRIDAY_KEY, DayOfWeek.FRIDAY);
            extractKey(HOUR_MINUTE_SATURDAY_KEY, DayOfWeek.SATURDAY);

            updateGui();
        } else {
            Intent intent = getIntent();
            if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
                Assert.assertTrue(!intent.hasExtra(NEW_KEY));

                mCustomTimeId = intent.getIntExtra(CUSTOM_TIME_ID_KEY, -1);
                Assert.assertTrue(mCustomTimeId != -1);

                getSupportLoaderManager().initLoader(0, null, this);
            } else {
                Assert.assertTrue(intent.hasExtra(NEW_KEY));

                for (DayOfWeek dayOfWeek : DayOfWeek.values())
                    mHourMinutes.put(dayOfWeek, new HourMinute(9, 0));

                updateGui();
            }
        }
    }

    private void extractKey(String key, DayOfWeek dayOfWeek) {
        Assert.assertTrue(mSavedInstanceState != null);
        Assert.assertTrue(!TextUtils.isEmpty(key));
        Assert.assertTrue(dayOfWeek != null);

        Assert.assertTrue(mSavedInstanceState.containsKey(key));

        HourMinute hourMinute = mSavedInstanceState.getParcelable(key);
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

        mHourMinutes.put(editedDayOfWeek, hourMinute);
        mTimeViews.get(editedDayOfWeek).setText(hourMinute.toString());
    }

    @Override
    public Loader<ShowCustomTimeLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowCustomTimeLoader(this, mCustomTimeId);
    }

    public void updateGui() {
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

        mCustomTimeSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mCustomTimeName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.task_name_toast));
                    messageDialogFragment.show(getSupportFragmentManager(), "empty_name");
                    return;
                }

                if (mData != null)
                    DomainFactory.getDomainFactory(ShowCustomTimeActivity.this).updateCustomTime(mData.DataId, mData.Id, name, mHourMinutes);
                else
                    DomainFactory.getDomainFactory(ShowCustomTimeActivity.this).createCustomTime(name, mHourMinutes);

                finish();
            }
        });
    }

    @Override
    public void onLoadFinished(Loader<ShowCustomTimeLoader.Data> loader, ShowCustomTimeLoader.Data data) {
        mData = data;

        mCustomTimeName.setText(mData.Name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            mHourMinutes.put(dayOfWeek, mData.HourMinutes.get(dayOfWeek));

        updateGui();
    }

    @Override
    public void onLoaderReset(Loader<ShowCustomTimeLoader.Data> data) {
    }
}