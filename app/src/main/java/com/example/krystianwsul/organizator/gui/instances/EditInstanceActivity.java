package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.domainmodel.times.Time;
import com.example.krystianwsul.organizator.gui.tasks.DatePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.HourMinutePickerFragment;
import com.example.krystianwsul.organizator.gui.tasks.MessageDialogFragment;
import com.example.krystianwsul.organizator.gui.tasks.TimePickerView;

import junit.framework.Assert;

public class EditInstanceActivity extends AppCompatActivity implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener {
    private static final String INTENT_KEY = "instanceId";
    private static final String DATE_KEY = "date";

    private Date mDate;

    private Instance mInstance;
    private TextView mEditInstanceDate;
    private TimePickerView mEditInstanceTimePickerView;

    public static Intent getIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, EditInstanceActivity.class);
        intent.putExtra(INTENT_KEY, InstanceData.getBundle(instance));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_instance);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        Bundle bundle = intent.getParcelableExtra(INTENT_KEY);
        mInstance = InstanceData.getInstance(bundle);
        Assert.assertTrue(mInstance != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(DATE_KEY));

            mDate = savedInstanceState.getParcelable(DATE_KEY);
            Assert.assertTrue(mDate != null);
        } else {
            mDate = mInstance.getInstanceDate();
        }

        TextView editInstanceName = (TextView) findViewById(R.id.edit_instance_name);
        editInstanceName.setText(mInstance.getName());

        mEditInstanceDate = (TextView) findViewById(R.id.edit_instance_date);

        updateDateText();

        mEditInstanceDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(EditInstanceActivity.this, mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

        mEditInstanceTimePickerView = (TimePickerView) findViewById(R.id.edit_instance_timepickerview);
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

        Button editInstanceSave = (Button) findViewById(R.id.edit_instance_save);
        editInstanceSave.setOnClickListener(new View.OnClickListener() {
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

                mInstance.setInstanceDateTime(EditInstanceActivity.this, new DateTime(mDate, time));

                finish();
            }
        });
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
}
