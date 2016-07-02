package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateRootTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateRootTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateRootTaskLoader.Data> {
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String POSITION_KEY = "position";
    private static final String SCHEDULE_TYPE_CHANGED_KEY = "scheduleTypeChanged";

    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String DISCARD_TAG = "discard";

    private ScheduleHint mScheduleHint;

    private Spinner mCreateRootTaskSpinner;
    private EditText mCreateRootTaskName;
    private Bundle mSavedInstanceState;

    private Integer mRootTaskId;
    private ArrayList<Integer> mTaskIds;

    private boolean mIsTimeValid = false;

    private CreateRootTaskLoader.Data mData;

    private boolean mScheduleTypeChanged = false;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateRootTaskActivity.this::finish;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateRootTaskActivity.class);
    }

    public static Intent getCreateIntent(Context context, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getEditIntent(Context context, int rootTaskId) {
        Assert.assertTrue(context != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(ROOT_TASK_ID_KEY, rootTaskId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_root_task, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Assert.assertTrue(mCreateRootTaskName != null);

        menu.findItem(R.id.action_create_root_task_save).setVisible((mRootTaskId == null) || (mData != null));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_root_task_save:
                Assert.assertTrue(mCreateRootTaskName != null);

                if (!mIsTimeValid)
                    break;

                String name = mCreateRootTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
                Assert.assertTrue(scheduleFragment != null);

                if (mRootTaskId != null) {
                    scheduleFragment.updateRootTask(mRootTaskId, name);
                } else if (mTaskIds != null) {
                    scheduleFragment.createRootJoinTask(name, mTaskIds);
                } else {
                    scheduleFragment.createRootTask(name);
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
        setContentView(R.layout.activity_create_root_task);

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_root_task_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Assert.assertTrue(actionBar != null);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        mSavedInstanceState = savedInstanceState;

        Intent intent = getIntent();
        if (intent.hasExtra(SCHEDULE_HINT_KEY)) {
            mScheduleHint = intent.getParcelableExtra(SCHEDULE_HINT_KEY);
            Assert.assertTrue(mScheduleHint != null);
        }

        mCreateRootTaskName = (EditText) findViewById(R.id.create_root_task_name);
        Assert.assertTrue(mCreateRootTaskName != null);

        mCreateRootTaskSpinner = (Spinner) findViewById(R.id.create_root_task_spinner);
        Assert.assertTrue(mCreateRootTaskSpinner != null);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateRootTaskSpinner.setAdapter(adapter);

        if (intent.hasExtra(ROOT_TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));

            mRootTaskId = intent.getIntExtra(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(mRootTaskId != -1);
        } else {
            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }

            if (savedInstanceState == null)
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        MyCrashlytics.log("CreateRootTaskActivity.onResume");

        super.onResume();
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
                if (mScheduleHint != null) {
                    return SingleScheduleFragment.newInstance(mScheduleHint);
                } else {
                    return SingleScheduleFragment.newInstance();
                }
            case 1:
                if (mScheduleHint != null) {
                    return DailyScheduleFragment.newInstance(mScheduleHint);
                } else  {
                    return DailyScheduleFragment.newInstance();
                }
            case 2:
                if (mScheduleHint != null) {
                    return WeeklyScheduleFragment.newInstance(mScheduleHint);
                } else {
                    return WeeklyScheduleFragment.newInstance();
                }
            default:
                return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            outState.putInt(POSITION_KEY, mCreateRootTaskSpinner.getSelectedItemPosition());
            outState.putBoolean(SCHEDULE_TYPE_CHANGED_KEY, mScheduleTypeChanged);
        }
    }

    @Override
    public Loader<CreateRootTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new CreateRootTaskLoader(this, mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateRootTaskLoader.Data> loader, final CreateRootTaskLoader.Data data) {
        mData = data;

        mCreateRootTaskName.setVisibility(View.VISIBLE);
        mCreateRootTaskSpinner.setVisibility(View.VISIBLE);

        int spinnerPosition;
        int count = 0;
        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(POSITION_KEY)) {
            count++;

            spinnerPosition = mSavedInstanceState.getInt(POSITION_KEY, -1);
            Assert.assertTrue(spinnerPosition != -1);
            if (spinnerPosition > 0)
                count++;

            mScheduleTypeChanged = mSavedInstanceState.getBoolean(SCHEDULE_TYPE_CHANGED_KEY);
        } else if (mRootTaskId != null) {
            Assert.assertTrue(mData.RootTaskData != null);

            mCreateRootTaskName.setText(mData.RootTaskData.Name);

            ScheduleType scheduleType = mData.RootTaskData.ScheduleType;

            Fragment fragment;
            if (scheduleType == ScheduleType.SINGLE) {
                fragment = SingleScheduleFragment.newInstance(mRootTaskId);
                spinnerPosition = 0;
            } else if (scheduleType == ScheduleType.DAILY) {
                count++;

                fragment = DailyScheduleFragment.newInstance(mRootTaskId);
                spinnerPosition = 1;
            } else if (scheduleType == ScheduleType.WEEKLY) {
                count++;

                fragment = WeeklyScheduleFragment.newInstance(mRootTaskId);
                spinnerPosition = 2;
            } else {
                throw new IndexOutOfBoundsException("unknown schedule type");
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.create_root_task_frame, fragment).commitAllowingStateLoss();
        } else {
            spinnerPosition = 0;
            loadFragment(0);
        }
        final int finalCount = count;

        invalidateOptionsMenu();

        mCreateRootTaskSpinner.setSelection(spinnerPosition);

        Log.e("asdf", "count: " + count);
        mCreateRootTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e("asdf", "onItemSelected");
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                mScheduleTypeChanged = true;

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

    public void setTimeValid(boolean valid) {
        mIsTimeValid = valid;
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

        if (mRootTaskId == null) {
            Assert.assertTrue(mData.RootTaskData == null);

            if (!TextUtils.isEmpty(mCreateRootTaskName.getText()))
                return true;

            ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
            Assert.assertTrue(scheduleFragment != null);

            if (!(scheduleFragment instanceof SingleScheduleFragment))
                return true;

            if (scheduleFragment.dataChanged())
                return true;

            return false;
        } else {
            Assert.assertTrue(mData.RootTaskData != null);

            if (!mCreateRootTaskName.getText().toString().equals(mData.RootTaskData.Name))
                return true;

            if (mScheduleTypeChanged)
                return true;

            ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.create_root_task_frame);
            Assert.assertTrue(scheduleFragment != null);

            if (scheduleFragment.dataChanged())
                return true;

            return false;
        }
    }

    public static class ScheduleHint implements Parcelable {
        public final Date mDate;
        public final HourMinute mHourMinute;

        public ScheduleHint(Date date) {
            Assert.assertTrue(date != null);

            mDate = date;
            mHourMinute = null;
        }

        public ScheduleHint(TimeStamp timeStamp) {
            Assert.assertTrue(timeStamp != null);

            mDate = timeStamp.getDate();
            Assert.assertTrue(mDate != null);

            mHourMinute = timeStamp.getHourMinute();
            Assert.assertTrue(mHourMinute != null);
        }

        private ScheduleHint(Date date, HourMinute hourMinute) {
            Assert.assertTrue(date != null);

            mDate = date;
            mHourMinute = hourMinute;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mDate, 0);
            dest.writeInt(mHourMinute != null ? 1 : 0);
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<ScheduleHint>() {
            @Override
            public ScheduleHint createFromParcel(Parcel source) {
                Date date = source.readParcelable(Date.class.getClassLoader());
                Assert.assertTrue(date != null);

                boolean hasHourMinute = (source.readInt() == 1);
                HourMinute hourMinute = (hasHourMinute ? source.readParcelable(HourMinute.class.getClassLoader()) : null);

                return new ScheduleHint(date, hourMinute);
            }

            @Override
            public ScheduleHint[] newArray(int size) {
                return new ScheduleHint[size];
            }
        };
    }
}