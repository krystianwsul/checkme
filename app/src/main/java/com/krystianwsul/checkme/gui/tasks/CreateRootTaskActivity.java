package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateRootTaskActivity extends CreateTaskActivity {
    private static final String TASK_ID_KEY = "taskId";
    private static final String TASK_IDS_KEY = "taskIds";

    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String DISCARD_TAG = "discard";

    private Bundle mSavedInstanceState;
    private EditText mCreateTaskName;

    private Integer mTaskId;
    private ArrayList<Integer> mTaskIds;

    private ScheduleHint mScheduleHint;

    private boolean mIsTimeValid = false;

    private CreateTaskLoader.Data mData;

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

    public static Intent getEditIntent(Context context, int taskId) {
        Assert.assertTrue(context != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_root_task, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Assert.assertTrue(mCreateTaskName != null);

        menu.findItem(R.id.action_create_root_task_save).setVisible((mTaskId == null) || (mData != null));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_root_task_save:
                Assert.assertTrue(mCreateTaskName != null);

                if (!mIsTimeValid)
                    break;

                String name = mCreateTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                if (mTaskId != null) {
                    schedulePickerFragment.updateRootTask(mTaskId, name);
                } else if (mTaskIds != null) {
                    schedulePickerFragment.createRootJoinTask(name, mTaskIds);
                } else {
                    schedulePickerFragment.createRootTask(name);
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
        setContentView(R.layout.activity_create_task);

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_task_toolbar);
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

        mCreateTaskName = (EditText) findViewById(R.id.create_task_name);
        Assert.assertTrue(mCreateTaskName != null);

        FrameLayout createTaskScheduleFrame = (FrameLayout) findViewById(R.id.create_task_schedule_frame);
        Assert.assertTrue(createTaskScheduleFrame != null);

        createTaskScheduleFrame.setVisibility(View.VISIBLE);

        if (intent.hasExtra(TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));
            Assert.assertTrue(!intent.hasExtra(SCHEDULE_HINT_KEY));

            mTaskId = intent.getIntExtra(TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else {
            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }

            if (intent.hasExtra(SCHEDULE_HINT_KEY)) {
                mScheduleHint = intent.getParcelableExtra(SCHEDULE_HINT_KEY);
                Assert.assertTrue(mScheduleHint != null);
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

    @Override
    public Loader<CreateTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new CreateTaskLoader(this, mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateTaskLoader.Data> loader, final CreateTaskLoader.Data data) {
        mData = data;

        mCreateTaskName.setVisibility(View.VISIBLE);

        if (mTaskId != null && mSavedInstanceState == null) {
            Assert.assertTrue(mData.TaskData != null);

            mCreateTaskName.setText(mData.TaskData.Name);
        }

        SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
        if (schedulePickerFragment == null) {
            if (mTaskId != null) {
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mScheduleHint == null);

                schedulePickerFragment = SchedulePickerFragment.getEditInstance(mTaskId);
            } else if (mTaskIds != null) {
                if (mScheduleHint == null)
                    schedulePickerFragment = SchedulePickerFragment.getJoinInstance(mTaskIds);
                else
                    schedulePickerFragment = SchedulePickerFragment.getJoinInstance(mTaskIds, mScheduleHint);
            } else {
                if (mScheduleHint == null)
                    schedulePickerFragment = SchedulePickerFragment.getCreateInstance();
                else
                    schedulePickerFragment = SchedulePickerFragment.getCreateInstance(mScheduleHint);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.create_task_schedule_frame, schedulePickerFragment)
                    .commitAllowingStateLoss();
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {

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

        if (mTaskId == null) {
            Assert.assertTrue(mData.TaskData == null);

            if (!TextUtils.isEmpty(mCreateTaskName.getText()))
                return true;

            SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
            Assert.assertTrue(schedulePickerFragment != null);

            if (schedulePickerFragment.dataChanged())
                return true;

            return false;
        } else {
            Assert.assertTrue(mData.TaskData != null);

            if (!mCreateTaskName.getText().toString().equals(mData.TaskData.Name))
                return true;

            SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
            Assert.assertTrue(schedulePickerFragment != null);

            if (schedulePickerFragment.dataChanged())
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