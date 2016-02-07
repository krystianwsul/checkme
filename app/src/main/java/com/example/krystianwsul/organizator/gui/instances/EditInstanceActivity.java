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
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.tasks.DatePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.HourMinutePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TimePickerView;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

public class EditInstanceActivity extends AppCompatActivity implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener, LoaderManager.LoaderCallbacks<DomainFactory> {
    private static final String INTENT_KEY = "instanceId";
    private static final String DATE_KEY = "date";

    private Date mDate;
    private Instance mInstance;

    private TextView mEditInstanceDate;
    private TimePickerView mEditInstanceTimePickerView;
    private Bundle mSavedInstanceState;
    private TextView mEditInstanceName;
    private Button mEditInstanceSave;

    public static Intent getIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, EditInstanceActivity.class);
        intent.putExtra(INTENT_KEY, InstanceData.getBundle(instance));
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
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, final DomainFactory domainFactory) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        Bundle bundle = intent.getParcelableExtra(INTENT_KEY);
        mInstance = InstanceData.getInstance(domainFactory, bundle);
        Assert.assertTrue(mInstance != null);

        if (mSavedInstanceState != null) {
            Assert.assertTrue(mSavedInstanceState.containsKey(DATE_KEY));

            mDate = mSavedInstanceState.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);
        } else {
            mDate = mInstance.getInstanceDate();
        }

        mEditInstanceName.setText(mInstance.getName());

        updateDateText();

        mEditInstanceTimePickerView.setDomainFactory(domainFactory);
        mEditInstanceTimePickerView.setTime(mInstance.getInstanceTime());
        mEditInstanceTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(CustomTime customTime) {
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
                Assert.assertTrue(mInstance != null);

                Time time = mEditInstanceTimePickerView.getTime();
                Assert.assertTrue(time != null);

                if ((new TimeStamp(mDate, time.getHourMinute(mDate.getDayOfWeek())).compareTo(TimeStamp.getNow()) <= 0)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.invalid_time_message));
                    messageDialogFragment.show(getSupportFragmentManager(), "invalid_time");
                    return;
                }

                domainFactory.setInstanceDateTime(EditInstanceActivity.this, mInstance, new DateTime(mDate, time));

                domainFactory.save();

                TickService.startService(EditInstanceActivity.this);

                finish();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mInstance = null;
        mEditInstanceTimePickerView.setOnTimeSelectedListener(null);
        mEditInstanceSave.setOnClickListener(null);
    }
}
