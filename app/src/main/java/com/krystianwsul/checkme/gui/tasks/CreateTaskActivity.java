package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class CreateTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {
    protected static final String DISCARD_TAG = "discard";

    protected static final String TASK_ID_KEY = "taskId";
    protected static final String TASK_IDS_KEY = "taskIds";

    protected static final String PARENT_TASK_ID_HINT_KEY = "parentTaskIdHint";
    protected static final String SCHEDULE_HINT_KEY = "scheduleHint";

    protected Bundle mSavedInstanceState;
    protected EditText mCreateTaskName;

    protected FrameLayout mCreateTaskParentFrame;
    protected FrameLayout mCreateTaskScheduleFrame;

    protected final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateTaskActivity.this::finish;

    protected Integer mTaskId;
    protected ArrayList<Integer> mTaskIds;

    protected CreateTaskActivity.ScheduleHint mScheduleHint;
    protected Integer mParentTaskIdHint = null;

    protected CreateTaskLoader.Data mData;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateTaskActivity.class);
    }

    public static Intent getCreateIntent(Context context, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getCreateIntent(Context context, int parentTaskIdHint) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, int parentTaskIdHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getEditIntent(Context context, int taskId) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_task, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Assert.assertTrue(mCreateTaskName != null);

        menu.findItem(R.id.action_create_task_save).setVisible(mData != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mCreateTaskName != null);

                String name = mCreateTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                boolean finish;
                if ((mData.TaskData != null && mData.TaskData.ParentTaskId != null) || (mParentTaskIdHint != null)) {
                    ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                    Assert.assertTrue(parentFragment != null);

                    if (mTaskId != null) {
                        Assert.assertTrue(mData.TaskData != null);
                        Assert.assertTrue(mTaskIds == null);

                        finish = parentFragment.updateTask(mTaskId, name);
                    } else if (mTaskIds != null) {
                        Assert.assertTrue(mData.TaskData == null);

                        finish = parentFragment.createJoinTask(name, mTaskIds);
                    } else {
                        Assert.assertTrue(mData.TaskData == null);

                        finish = parentFragment.createTask(name);
                    }
                } else {
                    SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                    Assert.assertTrue(schedulePickerFragment != null);

                    if (mTaskId != null) {
                        Assert.assertTrue(mData.TaskData != null);
                        Assert.assertTrue(mTaskIds == null);

                        finish = schedulePickerFragment.updateTask(mTaskId, name);
                    } else if (mTaskIds != null) {
                        Assert.assertTrue(mData.TaskData == null);

                        finish = schedulePickerFragment.createJoinTask(name, mTaskIds);
                    } else {
                        Assert.assertTrue(mData.TaskData == null);

                        finish = schedulePickerFragment.createTask(name);
                    }
                }

                if (finish)
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

        mCreateTaskName = (EditText) findViewById(R.id.create_task_name);
        Assert.assertTrue(mCreateTaskName != null);

        mCreateTaskParentFrame = (FrameLayout) findViewById(R.id.create_task_parent_frame);
        Assert.assertTrue(mCreateTaskParentFrame != null);

        mCreateTaskScheduleFrame = (FrameLayout) findViewById(R.id.create_task_schedule_frame);
        Assert.assertTrue(mCreateTaskScheduleFrame != null);

        Intent intent = getIntent();
        if (intent.hasExtra(TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));
            Assert.assertTrue(!intent.hasExtra(PARENT_TASK_ID_HINT_KEY));
            Assert.assertTrue(!intent.hasExtra(SCHEDULE_HINT_KEY));

            mTaskId = intent.getIntExtra(TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else {
            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }

            if (intent.hasExtra(PARENT_TASK_ID_HINT_KEY)) {
                Assert.assertTrue(!intent.hasExtra(SCHEDULE_HINT_KEY));

                mParentTaskIdHint = intent.getIntExtra(PARENT_TASK_ID_HINT_KEY, -1);
                Assert.assertTrue(mParentTaskIdHint != -1);
            } else if (intent.hasExtra(SCHEDULE_HINT_KEY)) {
                mScheduleHint = intent.getParcelableExtra(SCHEDULE_HINT_KEY);
                Assert.assertTrue(mScheduleHint != null);
            }
        }

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        MyCrashlytics.log("CreateTaskActivity.onResume");

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

        if (mData.TaskData != null && mSavedInstanceState == null) {
            Assert.assertTrue(mTaskId != null);

            mCreateTaskName.setText(mData.TaskData.Name);
        }

        Assert.assertTrue(mData.TaskData != null || (mParentTaskIdHint != null || mScheduleHint != null));

        if ((mData.TaskData != null && mData.TaskData.ParentTaskId != null) || (mParentTaskIdHint != null)) {
            mCreateTaskParentFrame.setVisibility(View.VISIBLE);

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
            if (parentFragment == null) {
                if (mTaskId != null) {
                    Assert.assertTrue(mTaskIds == null);
                    Assert.assertTrue(mParentTaskIdHint == null);

                    parentFragment = ParentFragment.getEditInstance(mTaskId);
                } else if (mTaskIds != null) {
                    Assert.assertTrue(mParentTaskIdHint != null);

                    parentFragment = ParentFragment.getJoinInstance(mParentTaskIdHint, mTaskIds);
                } else {
                    Assert.assertTrue(mParentTaskIdHint != null);

                    parentFragment = ParentFragment.getCreateInstance(mParentTaskIdHint);
                }

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.create_task_parent_frame, parentFragment)
                        .commitAllowingStateLoss();
            }
        } else {
            mCreateTaskScheduleFrame.setVisibility(View.VISIBLE);

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
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {

    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    protected boolean tryClose() {
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

        if (mTaskId != null) {
            Assert.assertTrue(mData.TaskData != null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskIdHint == null);
            Assert.assertTrue(mScheduleHint == null);

            if (!mCreateTaskName.getText().toString().equals(mData.TaskData.Name))
                return true;

            if ((mData.TaskData != null && mData.TaskData.ParentTaskId != null) || (mParentTaskIdHint != null)) {
                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                Assert.assertTrue(parentFragment != null);

                if (parentFragment.dataChanged())
                    return true;
            } else {
                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                if (schedulePickerFragment.dataChanged())
                    return true;
            }
        } else {
            if (!TextUtils.isEmpty(mCreateTaskName.getText()))
                return true;

            if (mParentTaskIdHint != null) {
                Assert.assertTrue(mScheduleHint == null);

                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                Assert.assertTrue(parentFragment != null);

                if (parentFragment.dataChanged())
                    return true;
            } else {
                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                if (schedulePickerFragment.dataChanged())
                    return true;
            }
        }

        return false;
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
