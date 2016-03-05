package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.tasks.DatePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.HourMinutePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TimePickerView;
import com.example.krystianwsul.organizator.loaders.EditInstanceLoader;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.HashMap;

public class EditInstanceActivity extends AppCompatActivity implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener, LoaderManager.LoaderCallbacks<EditInstanceLoader.Data> {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String DATE_KEY = "date";

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
        mEditInstanceDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(EditInstanceActivity.this, mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

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
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        mDate = date;
        updateDateText();
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(mEditInstanceTimePickerView != null);

        mEditInstanceTimePickerView.setHourMinute(hourMinute);
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
            customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name, customTimeData.HourMinutes));
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

        mEditInstanceTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(int customTimeId) {
            }

            @Override
            public void onHourMinuteSelected(HourMinute hourMinute) {
            }

            @Override
            public void onHourMinuteClick() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(EditInstanceActivity.this, mEditInstanceTimePickerView.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }
        });

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

                DomainFactory.getDomainFactory(EditInstanceActivity.this).setInstanceDateTime(mData.DataId, EditInstanceActivity.this, mData.InstanceKey, mDate, new TimePair(mEditInstanceTimePickerView.getCustomTimeId(), mEditInstanceTimePickerView.getHourMinute()));

                finish();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<EditInstanceLoader.Data> loader) {
    }
}
