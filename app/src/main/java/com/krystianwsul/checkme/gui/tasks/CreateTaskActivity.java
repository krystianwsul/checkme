package com.krystianwsul.checkme.gui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.EditText;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public abstract class CreateTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {
    protected static final String DISCARD_TAG = "discard";

    protected static final String TASK_ID_KEY = "taskId";
    protected static final String TASK_IDS_KEY = "taskIds";

    protected static final String PARENT_TASK_ID_HINT_KEY = "parentTaskIdHint";
    protected static final String SCHEDULE_HINT_KEY = "scheduleHint";

    protected Bundle mSavedInstanceState;
    protected EditText mCreateTaskName;

    protected final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateTaskActivity.this::finish;

    protected Integer mTaskId;
    protected ArrayList<Integer> mTaskIds;

    protected CreateRootTaskActivity.ScheduleHint mScheduleHint;
    protected Integer mParentTaskIdHint = null;

    protected CreateTaskLoader.Data mData;

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
