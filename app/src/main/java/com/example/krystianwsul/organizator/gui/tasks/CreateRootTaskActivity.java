package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.CreateRootTaskLoader;
import com.example.krystianwsul.organizator.utils.ScheduleType;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateRootTaskActivity extends AppCompatActivity implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener, LoaderManager.LoaderCallbacks<CreateRootTaskLoader.Data> {
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String POSITION_KEY = "position";

    private Spinner mCreateRootTaskSpinner;
    private EditText mCreateRootTaskName;
    private Button mCreateRootTaskSave;
    private Bundle mSavedInstanceState;

    private Integer mRootTaskId;
    private ArrayList<Integer> mTaskIds;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateRootTaskActivity.class);
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    public static Intent getEditIntent(Context context, int rootTaskId) {
        Assert.assertTrue(context != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(ROOT_TASK_ID_KEY, rootTaskId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_root_task);

        mSavedInstanceState = savedInstanceState;

        mCreateRootTaskName = (EditText) findViewById(R.id.create_root_task_name);
        Assert.assertTrue(mCreateRootTaskName != null);

        mCreateRootTaskName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mCreateRootTaskSave.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });

        mCreateRootTaskSave = (Button) findViewById(R.id.create_root_task_save);
        Assert.assertTrue(mCreateRootTaskSave != null);

        mCreateRootTaskSpinner = (Spinner) findViewById(R.id.create_root_task_spinner);
        Assert.assertTrue(mCreateRootTaskSpinner != null);

        Intent intent = getIntent();
        if (intent.hasExtra(ROOT_TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));

            mRootTaskId = intent.getIntExtra(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(mRootTaskId != -1);

            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
            updateGui(null);

            if (savedInstanceState == null)
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        DatePickerFragment.DatePickerFragmentListener datePickerFragmentListener = (DatePickerFragment.DatePickerFragmentListener) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
        Assert.assertTrue(datePickerFragmentListener != null);

        datePickerFragmentListener.onDatePickerFragmentResult(date);
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        HourMinutePickerFragment.HourMinutePickerFragmentListener hourMinutePickerFragmentListener = (HourMinutePickerFragment.HourMinutePickerFragmentListener) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
        Assert.assertTrue(hourMinutePickerFragmentListener != null);

        hourMinutePickerFragmentListener.onHourMinutePickerFragmentResult(hourMinute);
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getSupportFragmentManager().beginTransaction().replace(R.id.create_root_task_frame, fragment).commitAllowingStateLoss();
    }

    private Fragment createFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        switch (position) {
            case 0:
                return SingleScheduleFragment.newInstance();
            case 1:
                return DailyScheduleFragment.newInstance();
            case 2:
                return WeeklyScheduleFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(POSITION_KEY, mCreateRootTaskSpinner.getSelectedItemPosition());
    }

    @Override
    public Loader<CreateRootTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new CreateRootTaskLoader(this, mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateRootTaskLoader.Data> loader, final CreateRootTaskLoader.Data data) {
        updateGui(data);
    }

    private void updateGui(final CreateRootTaskLoader.Data data) {
        int spinnerPosition = 0;
        int count = 1;
        if (mSavedInstanceState != null) {
            int position = mSavedInstanceState.getInt(POSITION_KEY, -1);
            Assert.assertTrue(position != -1);
            if (position > 0)
                count = 2;
        } else if (mRootTaskId != null) {
            Assert.assertTrue(data != null);
            mCreateRootTaskName.setText(data.Name);

            ScheduleType scheduleType = data.ScheduleType;

            Fragment fragment;
            if (scheduleType == ScheduleType.SINGLE) {
                fragment = SingleScheduleFragment.newInstance(mRootTaskId);
            } else if (scheduleType == ScheduleType.DAILY) {
                fragment = DailyScheduleFragment.newInstance(mRootTaskId);
                spinnerPosition = 1;
            } else if (scheduleType == ScheduleType.WEEKLY) {
                fragment = WeeklyScheduleFragment.newInstance(mRootTaskId);
                spinnerPosition = 2;
            } else {
                throw new IndexOutOfBoundsException("unknown schedule type");
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.create_root_task_frame, fragment).commitAllowingStateLoss();
        } else {
            loadFragment(0);
        }
        final int finalCount = count;

        mCreateRootTaskSave.setEnabled(!TextUtils.isEmpty(mCreateRootTaskName.getText().toString().trim()));
        mCreateRootTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mCreateRootTaskName.getText().toString().trim();

                ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
                Assert.assertTrue(scheduleFragment != null);

                if (!scheduleFragment.isValidTime()) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.invalid_time_message));
                    messageDialogFragment.show(getSupportFragmentManager(), "invalid_time");
                    return;
                }

                if (mRootTaskId != null) {
                    scheduleFragment.updateRootTask(mRootTaskId, name);
                } else if (mTaskIds != null) {
                    scheduleFragment.createRootJoinTask(name, mTaskIds);
                } else {
                    scheduleFragment.createRootTask(name);
                }

                finish();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateRootTaskSpinner.setAdapter(adapter);

        mCreateRootTaskSpinner.setSelection(spinnerPosition);

        mCreateRootTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                loadFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onLoaderReset(Loader<CreateRootTaskLoader.Data> loader) {
    }
}