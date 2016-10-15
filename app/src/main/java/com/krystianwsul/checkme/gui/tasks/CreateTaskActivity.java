package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.RemoteMonthlyWeekScheduleRecord;
import com.krystianwsul.checkme.firebase.RemoteSingleScheduleRecord;
import com.krystianwsul.checkme.firebase.RemoteTaskRecord;
import com.krystianwsul.checkme.firebase.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CreateTaskActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data>, FriendPickerFragment.Listener {
    private static final String DISCARD_TAG = "discard";

    private static final String TASK_ID_KEY = "taskId";
    private static final String TASK_IDS_KEY = "taskIds";

    private static final String PARENT_TASK_KEY_HINT_KEY = "parentTaskKeyHint";
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String PARENT_ID = "parentId";
    private static final String PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment";

    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";
    private static final String SCHEDULE_ENTRIES_KEY = "scheduleEntries";
    private static final String NOTE_KEY = "note";
    private static final String NOTE_HAS_FOCUS_KEY = "noteHasFocus";

    private static final String SCHEDULE_DIALOG_TAG = "scheduleDialog";

    private static final String FRIEND_PICKER_DIALOG_TAG = "friendPickerDialog";
    private static final String FRIEND_POSITION_KEY = "friendPickerPosition";
    private static final String FRIEND_ENTRIES_KEY = "friendEntries";

    private Bundle mSavedInstanceState;

    private TextInputLayout mToolbarLayout;
    private EditText mToolbarEditText;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateTaskActivity.this::finish;

    private Integer mTaskId;
    private ArrayList<Integer> mTaskIds;

    @Nullable
    private CreateTaskActivity.ScheduleHint mScheduleHint;
    private TaskKey mParentTaskKeyHint = null;
    private String mNameHint = null;

    private CreateTaskLoader.Data mData;

    private CreateTaskLoader.TaskTreeData mParent;

    private RecyclerView mScheduleTimes;
    private CreateTaskAdapter mCreateTaskAdapter;

    private Integer mHourMinutePickerPosition = null;
    private Integer mFriendPosition = null;

    private List<ScheduleEntry> mScheduleEntries;

    private boolean mFirst = true;

    private final ParentPickerFragment.Listener mParentFragmentListener = new ParentPickerFragment.Listener() {
        @Override
        public void onTaskSelected(@NonNull CreateTaskLoader.TaskTreeData taskTreeData) {
            clearSchedulesFriends();

            mParent = taskTreeData;

            updateParentView();
        }

        @Override
        public void onTaskDeleted() {
            Assert.assertTrue(mParent != null);

            mParent = null;

            View view = mScheduleTimes.getChildAt(0);
            Assert.assertTrue(view != null);

            CreateTaskAdapter.ScheduleHolder scheduleHolder = (CreateTaskAdapter.ScheduleHolder) mScheduleTimes.getChildViewHolder(view);
            Assert.assertTrue(scheduleHolder != null);

            scheduleHolder.mScheduleText.setText(null);
        }
    };

    private final ScheduleDialogFragment.ScheduleDialogListener mScheduleDialogListener = new ScheduleDialogFragment.ScheduleDialogListener() {
        @Override
        public void onScheduleDialogResult(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
            Assert.assertTrue(mData != null);

            if (scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_DAY) {
                Assert.assertTrue(scheduleDialogData.mMonthlyDay);
            } else if (scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_WEEK) {
                Assert.assertTrue(!scheduleDialogData.mMonthlyDay);
            }

            if (mHourMinutePickerPosition == null) {
                clearParent();

                mCreateTaskAdapter.addScheduleEntry(ScheduleEntry.fromScheduleDialogData(scheduleDialogData));
            } else {
                Assert.assertTrue(mHourMinutePickerPosition > 0);

                mScheduleEntries.set(mHourMinutePickerPosition - 1, ScheduleEntry.fromScheduleDialogData(scheduleDialogData));

                mCreateTaskAdapter.notifyItemChanged(mHourMinutePickerPosition);

                mHourMinutePickerPosition = null;
            }
        }

        @Override
        public void onScheduleDialogDelete() {
            Assert.assertTrue(mHourMinutePickerPosition != null);
            Assert.assertTrue(mHourMinutePickerPosition > 0);
            Assert.assertTrue(mData != null);

            mScheduleEntries.remove(mHourMinutePickerPosition - 1);

            mCreateTaskAdapter.notifyItemRemoved(mHourMinutePickerPosition);

            mHourMinutePickerPosition = null;
        }

        @Override
        public void onScheduleDialogCancel() {
            if (mHourMinutePickerPosition != null) {
                Assert.assertTrue(mHourMinutePickerPosition > 0);

                mHourMinutePickerPosition = null;
            }
        }
    };

    private String mNote = null;

    private boolean mNoteHasFocus = false; // keyboard hack

    private final RecyclerView.OnChildAttachStateChangeListener mOnChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() { // keyboard hack
        @Override
        public void onChildViewAttachedToWindow(View view) {
            EditText noteText = (EditText) view.findViewById(R.id.note_text);
            if (noteText != null) {
                removeListenerHelper();

                noteText.requestFocus();

                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.showSoftInput(noteText, InputMethodManager.SHOW_FORCED);
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {

        }
    };

    private List<UserData> mFriendEntries = new ArrayList<>(); // todo friend

    @NonNull
    public static Intent getCreateIntent(@NonNull Context context) {
        return new Intent(context, CreateTaskActivity.class);
    }

    @NonNull
    public static Intent getCreateIntent(@NonNull Context context, @NonNull ScheduleHint scheduleHint) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    @NonNull
    public static Intent getCreateIntent(@NonNull Context context, @NonNull TaskKey parentTaskKeyHint) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint);
        return intent;
    }

    @NonNull
    public static Intent getJoinIntent(@NonNull Context context, @NonNull ArrayList<TaskKey> joinTaskKeys) {
        Assert.assertTrue(joinTaskKeys.size() > 1);

        //todo firebase

        ArrayList<Integer> joinTaskIds = Stream.of(joinTaskKeys).map(joinTaskKey -> {
            Assert.assertTrue(joinTaskKey.mLocalTaskId != null);

            return joinTaskKey.mLocalTaskId;
        }).collect(Collectors.toCollection(ArrayList::new));

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    @NonNull
    public static Intent getJoinIntent(@NonNull Context context, @NonNull ArrayList<TaskKey> joinTaskKeys, int parentTaskIdHint) {
        Assert.assertTrue(joinTaskKeys.size() > 1);

        //todo firebase

        ArrayList<Integer> joinTaskIds = Stream.of(joinTaskKeys).map(joinTaskKey -> {
            Assert.assertTrue(joinTaskKey.mLocalTaskId != null);

            return joinTaskKey.mLocalTaskId;
        }).collect(Collectors.toCollection(ArrayList::new));

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(PARENT_TASK_KEY_HINT_KEY, new TaskKey(parentTaskIdHint)); // todo firebase
        return intent;
    }

    @NonNull
    public static Intent getJoinIntent(@NonNull Context context, @NonNull ArrayList<Integer> joinTaskIds, @NonNull ScheduleHint scheduleHint) {
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    @NonNull
    public static Intent getEditIntent(@NonNull Context context, @NonNull TaskKey taskKey) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        // todo firebase

        Integer taskId = taskKey.mLocalTaskId;
        Assert.assertTrue(taskId != null);

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
        Assert.assertTrue(mToolbarEditText != null);

        menu.findItem(R.id.action_create_task_save).setVisible(mData != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Assert.assertTrue(!hasValueParent() || !hasValueSchedule());

        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mToolbarEditText != null);

                if (updateError())
                    break;

                String name = mToolbarEditText.getText().toString().trim();
                Assert.assertTrue(!TextUtils.isEmpty(name));

                if (hasValueSchedule()) {
                    Assert.assertTrue(!hasValueParent());

                    if (mTaskId != null) {
                        Assert.assertTrue(mData.TaskData != null);
                        Assert.assertTrue(mTaskIds == null);
                        Assert.assertTrue(!hasValueFriends()); // todo friends

                        DomainFactory.getDomainFactory(this).updateScheduleTask(this, mData.DataId, mTaskId, name, getScheduleDatas(), mNote);
                    } else if (mTaskIds != null) {
                        Assert.assertTrue(mData.TaskData == null);
                        Assert.assertTrue(mTaskIds.size() > 1);
                        Assert.assertTrue(!hasValueFriends()); // todo friends

                        DomainFactory.getDomainFactory(this).createScheduleJoinRootTask(this, mData.DataId, name, getScheduleDatas(), mTaskIds, mNote);
                    } else {
                        Assert.assertTrue(mData.TaskData == null);

                        if (hasValueFriends()) {
                            ExactTimeStamp now = ExactTimeStamp.getNow();

                            List<RemoteSingleScheduleRecord> remoteSingleScheduleRecords = new ArrayList<>();
                            List<RemoteDailyScheduleRecord> remoteDailyScheduleRecords = new ArrayList<>();
                            List<RemoteWeeklyScheduleRecord> remoteWeeklyScheduleRecords = new ArrayList<>();
                            List<RemoteMonthlyDayScheduleRecord> remoteMonthlyDayScheduleRecords = new ArrayList<>();
                            List<RemoteMonthlyWeekScheduleRecord> remoteMonthlyWeekScheduleRecords = new ArrayList<>();

                            for (CreateTaskLoader.ScheduleData scheduleData : getScheduleDatas()) {
                                Assert.assertTrue(scheduleData != null);

                                switch (scheduleData.getScheduleType()) {
                                    case SINGLE: {
                                        CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;

                                        Date date = singleScheduleData.Date;

                                        Assert.assertTrue(singleScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                                        HourMinute hourMinute = singleScheduleData.TimePair.mHourMinute;
                                        Assert.assertTrue(hourMinute != null);

                                        remoteSingleScheduleRecords.add(new RemoteSingleScheduleRecord(now.getLong(), null, date.getYear(), date.getMonth(), date.getDay(), null, hourMinute.getHour(), hourMinute.getMinute()));
                                        break;
                                    }
                                    case DAILY: {
                                        CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;

                                        Assert.assertTrue(dailyScheduleData.TimePair.mCustomTimeId == null); // todo custom time

                                        HourMinute hourMinute = dailyScheduleData.TimePair.mHourMinute;
                                        Assert.assertTrue(hourMinute != null);

                                        remoteDailyScheduleRecords.add(new RemoteDailyScheduleRecord(now.getLong(), null, null, hourMinute.getHour(), hourMinute.getMinute()));
                                        break;
                                    }
                                    case WEEKLY: {
                                        CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;

                                        DayOfWeek dayOfWeek = weeklyScheduleData.DayOfWeek;

                                        Assert.assertTrue(weeklyScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                                        HourMinute hourMinute = weeklyScheduleData.TimePair.mHourMinute;
                                        Assert.assertTrue(hourMinute != null);

                                        remoteWeeklyScheduleRecords.add(new RemoteWeeklyScheduleRecord(now.getLong(), null, dayOfWeek.ordinal(), null, hourMinute.getHour(), hourMinute.getMinute()));
                                        break;
                                    }
                                    case MONTHLY_DAY: {
                                        CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;

                                        Assert.assertTrue(monthlyDayScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                                        HourMinute hourMinute = monthlyDayScheduleData.TimePair.mHourMinute;
                                        Assert.assertTrue(hourMinute != null);

                                        remoteMonthlyDayScheduleRecords.add(new RemoteMonthlyDayScheduleRecord(now.getLong(), null, monthlyDayScheduleData.mDayOfMonth, monthlyDayScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute()));
                                        break;
                                    }
                                    case MONTHLY_WEEK: {
                                        CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;

                                        Assert.assertTrue(monthlyWeekScheduleData.TimePair.mCustomTimeId == null); // todo custom time
                                        HourMinute hourMinute = monthlyWeekScheduleData.TimePair.mHourMinute;
                                        Assert.assertTrue(hourMinute != null);

                                        remoteMonthlyWeekScheduleRecords.add(new RemoteMonthlyWeekScheduleRecord(now.getLong(), null, monthlyWeekScheduleData.mDayOfMonth, monthlyWeekScheduleData.mDayOfWeek.ordinal(), monthlyWeekScheduleData.mBeginningOfMonth, null, hourMinute.getHour(), hourMinute.getMinute()));
                                        break;
                                    }
                                    default:
                                        throw new UnsupportedOperationException();
                                }
                            }

                            RemoteTaskRecord remoteTaskRecord = new RemoteTaskRecord(name, now.getLong(), null, null, null, null, mNote, remoteSingleScheduleRecords, remoteDailyScheduleRecords, remoteWeeklyScheduleRecords, remoteMonthlyDayScheduleRecords, remoteMonthlyWeekScheduleRecords);

                            UserData userData = MainActivity.getUserData();
                            Assert.assertTrue(userData != null);

                            DatabaseWrapper.addTask(userData, remoteTaskRecord, mFriendEntries);
                        } else {
                            DomainFactory.getDomainFactory(this).createScheduleRootTask(this, mData.DataId, name, getScheduleDatas(), mNote);
                        }
                    }
                } else if (hasValueParent()) {
                    Assert.assertTrue(mParent != null);
                    Assert.assertTrue(!hasValueFriends());

                    if (mTaskId != null) {
                        Assert.assertTrue(mData.TaskData != null);
                        Assert.assertTrue(mTaskIds == null);

                        DomainFactory.getDomainFactory(this).updateChildTask(this, mData.DataId, mTaskId, name, mParent.mTaskKey.mLocalTaskId, mNote); // todo firebase
                    } else if (mTaskIds != null) {
                        Assert.assertTrue(mData.TaskData == null);
                        Assert.assertTrue(mTaskIds.size() > 1);

                        DomainFactory.getDomainFactory(this).createJoinChildTask(this, mData.DataId, mParent.mTaskKey.mLocalTaskId, name, mTaskIds, mNote); // todo firebase
                    } else {
                        Assert.assertTrue(mData.TaskData == null);

                        DomainFactory.getDomainFactory(this).createChildTask(this, mData.DataId, mParent.mTaskKey.mLocalTaskId, name, mNote); // todo firebase
                    }
                } else {  // no reminder
                    Assert.assertTrue(!hasValueFriends()); // todo friends

                    if (mTaskId != null) {
                        Assert.assertTrue(mData.TaskData != null);
                        Assert.assertTrue(mTaskIds == null);

                        DomainFactory.getDomainFactory(this).updateRootTask(this, mData.DataId, mTaskId, name, mNote);
                    } else if (mTaskIds != null) {
                        Assert.assertTrue(mData.TaskData == null);

                        DomainFactory.getDomainFactory(this).createJoinRootTask(this, mData.DataId, name, mTaskIds, mNote);
                    } else {
                        Assert.assertTrue(mData.TaskData == null);

                        DomainFactory.getDomainFactory(this).createRootTask(this, mData.DataId, name, mNote);
                    }
                }

                ArrayList<TaskKey> taskKeys = new ArrayList<>();

                // this task
                if (mTaskId != null)
                    taskKeys.add(new TaskKey(mTaskId)); // todo firebase

                // new parent
                if (mParent != null)
                    taskKeys.add(mParent.mTaskKey); // todo firebase

                // old parent of single task
                if (mData.TaskData != null && mData.TaskData.mParentTaskKey != null)
                    taskKeys.add(mData.TaskData.mParentTaskKey);

                // old parent of multiple tasks
                if (mParentTaskKeyHint != null)
                    taskKeys.add(mParentTaskKeyHint);

                TickService.startService(this, taskKeys);

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

        mScheduleTimes = (RecyclerView) findViewById(R.id.schedule_recycler);
        Assert.assertTrue(mScheduleTimes != null);

        mScheduleTimes.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        if (intent.hasExtra(TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));
            Assert.assertTrue(!intent.hasExtra(PARENT_TASK_KEY_HINT_KEY));
            Assert.assertTrue(!intent.hasExtra(SCHEDULE_HINT_KEY));

            mTaskId = intent.getIntExtra(TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else if ((intent.getAction() != null) && intent.getAction().equals(Intent.ACTION_SEND)) {
            Assert.assertTrue(intent.getType().equals("text/plain"));

            mNameHint = intent.getStringExtra(Intent.EXTRA_TEXT);
            Assert.assertTrue(!TextUtils.isEmpty(mNameHint));
        } else {
            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }

            if (intent.hasExtra(PARENT_TASK_KEY_HINT_KEY)) {
                Assert.assertTrue(!intent.hasExtra(SCHEDULE_HINT_KEY));

                mParentTaskKeyHint = intent.getParcelableExtra(PARENT_TASK_KEY_HINT_KEY);
                Assert.assertTrue(mParentTaskKeyHint != null);
            } else if (intent.hasExtra(SCHEDULE_HINT_KEY)) {
                mScheduleHint = intent.getParcelableExtra(SCHEDULE_HINT_KEY);
                Assert.assertTrue(mScheduleHint != null);
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(SCHEDULE_ENTRIES_KEY)) {
            mScheduleEntries = savedInstanceState.getParcelableArrayList(SCHEDULE_ENTRIES_KEY);
            Assert.assertTrue(mScheduleEntries != null);

            Assert.assertTrue(savedInstanceState.containsKey(FRIEND_ENTRIES_KEY));

            mFriendEntries = savedInstanceState.getParcelableArrayList(FRIEND_ENTRIES_KEY);
            Assert.assertTrue(mFriendEntries != null);

            if (savedInstanceState.containsKey(HOUR_MINUTE_PICKER_POSITION_KEY)) {
                mHourMinutePickerPosition = savedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -1);

                Assert.assertTrue(mHourMinutePickerPosition != -1);
                Assert.assertTrue(mHourMinutePickerPosition > 0);
            }

            if (savedInstanceState.containsKey(FRIEND_POSITION_KEY)) {
                mFriendPosition = savedInstanceState.getInt(FRIEND_POSITION_KEY, -1);
                Assert.assertTrue(mFriendPosition >= 0);
            }
        }

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);

        if (!mNoteHasFocus) { // keyboard hack
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mCreateTaskAdapter != null);
            Assert.assertTrue(mScheduleEntries != null);
            Assert.assertTrue(mFriendEntries != null);

            outState.putParcelableArrayList(SCHEDULE_ENTRIES_KEY, new ArrayList<>(mScheduleEntries));
            outState.putParcelableArrayList(FRIEND_ENTRIES_KEY, new ArrayList<>(mFriendEntries));

            if (mHourMinutePickerPosition != null) {
                Assert.assertTrue(mHourMinutePickerPosition > 0);

                outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
            }

            if (mFriendPosition != null) {
                Assert.assertTrue(mFriendPosition >= 0);

                outState.putInt(FRIEND_POSITION_KEY, mFriendPosition);
            }

            if (mParent != null) {
                outState.putInt(PARENT_ID, mParent.mTaskKey.mLocalTaskId); // todo firebase
            }

            if (!TextUtils.isEmpty(mNote))
                outState.putString(NOTE_KEY, mNote);

            outState.putBoolean(NOTE_HAS_FOCUS_KEY, mNoteHasFocus);
        }
    }

    @Override
    public Loader<CreateTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        List<TaskKey> excludedTaskKeys = new ArrayList<>();

        if (mTaskId != null) {
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskKeyHint == null);

            excludedTaskKeys.add(new TaskKey(mTaskId));
        } else if (mTaskIds != null) {
            excludedTaskKeys.addAll(Stream.of(mTaskIds)
                    .map(TaskKey::new)
                    .collect(Collectors.toList()));
        }

        return new CreateTaskLoader(this, mTaskId, excludedTaskKeys);
    }

    @Override
    public void onLoadFinished(Loader<CreateTaskLoader.Data> loader, final CreateTaskLoader.Data data) {
        mData = data;

        mToolbarLayout.setVisibility(View.VISIBLE);

        if (mSavedInstanceState == null) {
            if (mData.TaskData != null) {
                Assert.assertTrue(mTaskId != null);

                mToolbarEditText.setText(mData.TaskData.Name);
            } else if (!TextUtils.isEmpty(mNameHint)) {
                Assert.assertTrue(mTaskId == null);
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mParentTaskKeyHint == null);
                Assert.assertTrue(mScheduleHint == null);

                mToolbarEditText.setText(mNameHint);
            }
        }

        mToolbarLayout.setHintAnimationEnabled(true);

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

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(SCHEDULE_ENTRIES_KEY)) {
            if (mSavedInstanceState.containsKey(PARENT_ID)) {
                int parentId = mSavedInstanceState.getInt(PARENT_ID);
                mParent = findTaskData(new TaskKey(parentId)); // todo firebase
            }

            if (mSavedInstanceState.containsKey(NOTE_KEY)) {
                mNote = mSavedInstanceState.getString(NOTE_KEY);
                Assert.assertTrue(!TextUtils.isEmpty(mNote));
            }

            Assert.assertTrue(mSavedInstanceState.containsKey(NOTE_HAS_FOCUS_KEY));

            mNoteHasFocus = mSavedInstanceState.getBoolean(NOTE_HAS_FOCUS_KEY);
        } else {
            if (mData.TaskData != null && mData.TaskData.mParentTaskKey != null) {
                Assert.assertTrue(mParentTaskKeyHint == null);
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mTaskId != null);

                mParent = findTaskData(mData.TaskData.mParentTaskKey);
            } else if (mParentTaskKeyHint != null) {
                Assert.assertTrue(mTaskId == null);

                mParent = findTaskData(mParentTaskKeyHint);
            }

            if (mData.TaskData != null)
                mNote = mData.TaskData.mNote;
        }

        ParentPickerFragment parentPickerFragment = (ParentPickerFragment) getSupportFragmentManager().findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG);
        if (parentPickerFragment != null)
            parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);

        invalidateOptionsMenu();

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(SCHEDULE_ENTRIES_KEY))) {
            Assert.assertTrue(mScheduleEntries == null);

            mFirst = false;

            mScheduleEntries = new ArrayList<>();

            if (mData.TaskData != null) {
                if (mData.TaskData.ScheduleDatas != null) {
                    Assert.assertTrue(!mData.TaskData.ScheduleDatas.isEmpty());

                    mScheduleEntries = Stream.of(mData.TaskData.ScheduleDatas)
                            .map(scheduleData -> {
                                switch (scheduleData.getScheduleType()) {
                                    case SINGLE:
                                        CreateTaskLoader.SingleScheduleData singleScheduleData = (CreateTaskLoader.SingleScheduleData) scheduleData;
                                        return new SingleScheduleEntry(singleScheduleData);
                                    case DAILY:
                                        CreateTaskLoader.DailyScheduleData dailyScheduleData = (CreateTaskLoader.DailyScheduleData) scheduleData;
                                        return new DailyScheduleEntry(dailyScheduleData);
                                    case WEEKLY:
                                        CreateTaskLoader.WeeklyScheduleData weeklyScheduleData = (CreateTaskLoader.WeeklyScheduleData) scheduleData;
                                        return new WeeklyScheduleEntry(weeklyScheduleData);
                                    case MONTHLY_DAY:
                                        CreateTaskLoader.MonthlyDayScheduleData monthlyDayScheduleData = (CreateTaskLoader.MonthlyDayScheduleData) scheduleData;
                                        return new MonthlyDayScheduleEntry(monthlyDayScheduleData);
                                    case MONTHLY_WEEK:
                                        CreateTaskLoader.MonthlyWeekScheduleData monthlyWeekScheduleData = (CreateTaskLoader.MonthlyWeekScheduleData) scheduleData;
                                        return new MonthlyWeekScheduleEntry(monthlyWeekScheduleData);
                                    default:
                                        throw new UnsupportedOperationException();
                                }
                            })
                            .collect(Collectors.toList());
                }
            } else {
                if (mParentTaskKeyHint == null)
                    mScheduleEntries.add(firstScheduleEntry());
            }
        }

        ScheduleDialogFragment singleDialogFragment = (ScheduleDialogFragment) getSupportFragmentManager().findFragmentByTag(SCHEDULE_DIALOG_TAG);
        if (singleDialogFragment != null)
            singleDialogFragment.initialize(mData.CustomTimeDatas, mScheduleDialogListener);

        mCreateTaskAdapter = new CreateTaskAdapter();
        mScheduleTimes.setAdapter(mCreateTaskAdapter);

        if (mNoteHasFocus) { // keyboard hack
            int notePosition = mScheduleEntries.size() + 2;

            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mScheduleTimes.getLayoutManager();

            mScheduleTimes.addOnChildAttachStateChangeListener(mOnChildAttachStateChangeListener);

            linearLayoutManager.scrollToPosition(notePosition);
        }

        Assert.assertTrue(!hasValueParent() || !hasValueSchedule());
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {

    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    private boolean tryClose() {
        Assert.assertTrue(!hasValueParent() || !hasValueSchedule());

        if (dataChanged()) {
            DiscardDialogFragment discardDialogFragment = DiscardDialogFragment.newInstance();
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
            discardDialogFragment.show(getSupportFragmentManager(), DISCARD_TAG);

            return false;
        } else {
            return true;
        }
    }

    private boolean updateError() {
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mScheduleEntries != null);
        Assert.assertTrue(mToolbarEditText != null);
        Assert.assertTrue(mToolbarLayout != null);
        Assert.assertTrue(mScheduleTimes != null);

        boolean hasError = false;

        if (TextUtils.isEmpty(mToolbarEditText.getText())) {
            mToolbarLayout.setError(getString(R.string.nameError));

            hasError = true;
        } else {
            mToolbarLayout.setError(null);
        }

        for (ScheduleEntry scheduleEntry : mScheduleEntries) {
            Assert.assertTrue(scheduleEntry != null);

            if (scheduleEntry.getScheduleType() != ScheduleType.SINGLE)
                continue;

            SingleScheduleEntry singleScheduleEntry = (SingleScheduleEntry) scheduleEntry;

            if ((mData.TaskData != null) && (mData.TaskData.ScheduleDatas != null) && mData.TaskData.ScheduleDatas.contains(scheduleEntry.getScheduleData()))
                continue;

            if (singleScheduleEntry.mDate.compareTo(Date.today()) > 0)
                continue;

            if (singleScheduleEntry.mDate.compareTo(Date.today()) < 0) {
                setScheduleEntryError(scheduleEntry, R.string.error_date);

                hasError = true;
                continue;
            }

            HourMinute hourMinute;
            TimePair timePair = singleScheduleEntry.mTimePair;
            if (timePair.mCustomTimeId != null) {
                Assert.assertTrue(timePair.mHourMinute == null);

                hourMinute = mData.CustomTimeDatas.get(timePair.mCustomTimeId).HourMinutes.get(singleScheduleEntry.mDate.getDayOfWeek());
            } else {
                Assert.assertTrue(timePair.mHourMinute != null);

                hourMinute = timePair.mHourMinute;
            }

            Assert.assertTrue(hourMinute != null);

            if (hourMinute.compareTo(HourMinute.getNow()) <= 0) {
                setScheduleEntryError(scheduleEntry, R.string.error_time);

                hasError = true;
            }
        }

        return hasError;
    }

    private void setScheduleEntryError(@NonNull ScheduleEntry scheduleEntry, int stringId) {
        scheduleEntry.mError = getString(stringId);
        Assert.assertTrue(!TextUtils.isEmpty(scheduleEntry.mError));

        int index = mScheduleEntries.indexOf(scheduleEntry);
        Assert.assertTrue(index >= 0);

        View view = mScheduleTimes.getChildAt(index + 1);
        if (view != null) {
            CreateTaskAdapter.ScheduleHolder scheduleHolder = (CreateTaskAdapter.ScheduleHolder) mScheduleTimes.getChildViewHolder(view);
            Assert.assertTrue(scheduleHolder != null);

            scheduleHolder.mScheduleLayout.setError(scheduleEntry.mError);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean dataChanged() {
        if (mData == null)
            return false;

        Assert.assertTrue(!hasValueParent() || !hasValueSchedule());
        Assert.assertTrue(!hasValueParent() || !hasValueFriends());

        if (hasValueFriends()) // todo friend
            return true;

        if (mTaskId != null) {
            Assert.assertTrue(mData.TaskData != null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskKeyHint == null);
            Assert.assertTrue(mScheduleHint == null);

            if (!mToolbarEditText.getText().toString().equals(mData.TaskData.Name))
                return true;

            if (mData.TaskData.mParentTaskKey != null) {
                if (!hasValueParent())
                    return true;

                if (!mParent.mTaskKey.equals(mData.TaskData.mParentTaskKey))
                    return true;

                return false;
            } else if (mData.TaskData.ScheduleDatas != null) {
                if (!hasValueSchedule())
                    return true;

                if (scheduleDataChanged())
                    return true;

                return false;
            } else {
                if (hasValueParent() || hasValueSchedule())
                    return true;

                return false;
            }
        } else {
            if (!TextUtils.isEmpty(mToolbarEditText.getText()))
                return true;

            if (mParentTaskKeyHint != null) {
                Assert.assertTrue(mScheduleHint == null);

                if (!hasValueParent())
                    return true;

                if (mParent == null || !mParent.mTaskKey.equals(mParentTaskKeyHint))
                    return true;

                return false;
            } else {
                if (!hasValueSchedule())
                    return true;

                if (scheduleDataChanged())
                    return true;

                return false;
            }
        }
    }

    @NonNull
    private CreateTaskLoader.TaskTreeData findTaskData(@NonNull TaskKey taskKey) {
        Assert.assertTrue(mData != null);

        List<CreateTaskLoader.TaskTreeData> taskTreeDatas = findTaskDataHelper(mData.TaskTreeDatas, taskKey)
                .collect(Collectors.toList());

        Assert.assertTrue(taskTreeDatas.size() == 1);
        return taskTreeDatas.get(0);
    }

    @NonNull
    private Stream<CreateTaskLoader.TaskTreeData> findTaskDataHelper(@NonNull Map<TaskKey, CreateTaskLoader.TaskTreeData> taskDatas, @NonNull TaskKey taskKey) {
        if (taskDatas.containsKey(taskKey)) {
            List<CreateTaskLoader.TaskTreeData> ret = new ArrayList<>();
            ret.add(taskDatas.get(taskKey));
            return Stream.of(ret);
        }

        return Stream.of(taskDatas.values())
                .map(taskData -> findTaskDataHelper(taskData.TaskDatas, taskKey))
                .flatMap(stream -> stream);
    }

    private void clearParent() {
        Assert.assertTrue(mCreateTaskAdapter != null);

        mParent = null;

        updateParentView();
    }

    private void updateParentView() {
        View view = mScheduleTimes.getChildAt(0);
        if (view == null)
            return;

        CreateTaskAdapter.ScheduleHolder scheduleHolder = (CreateTaskAdapter.ScheduleHolder) mScheduleTimes.getChildViewHolder(view);
        Assert.assertTrue(scheduleHolder != null);

        scheduleHolder.mScheduleText.setText(mParent != null ? mParent.Name : null);
    }

    private boolean hasValueParent() {
        return (mParent != null);
    }

    private boolean hasValueSchedule() {
        return !mScheduleEntries.isEmpty();
    }

    private boolean hasValueFriends() {
        return !mFriendEntries.isEmpty();
    }

    private ScheduleEntry firstScheduleEntry() {
        return new SingleScheduleEntry(mScheduleHint);
    }

    @NonNull
    private List<CreateTaskLoader.ScheduleData> getScheduleDatas() {
        List<CreateTaskLoader.ScheduleData> scheduleDatas = Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList());

        Assert.assertTrue(!scheduleDatas.isEmpty());

        return scheduleDatas;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean scheduleDataChanged() {
        if (mData == null)
            return false;

        Assert.assertTrue(mCreateTaskAdapter != null);

        Multiset<CreateTaskLoader.ScheduleData> oldScheduleDatas;
        if (mData.TaskData != null) {
            if (mData.TaskData.ScheduleDatas != null) {
                oldScheduleDatas = HashMultiset.create(mData.TaskData.ScheduleDatas);
            } else {
                oldScheduleDatas = HashMultiset.create();
            }
        } else {
            oldScheduleDatas = HashMultiset.create(Collections.singletonList(firstScheduleEntry().getScheduleData()));
        }

        Multiset<CreateTaskLoader.ScheduleData> newScheduleDatas = HashMultiset.create(Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList()));

        if (!oldScheduleDatas.equals(newScheduleDatas))
            return true;

        return false;
    }

    private void clearSchedulesFriends() {
        int scheduleCount = mScheduleEntries.size();

        mScheduleEntries = new ArrayList<>();
        mCreateTaskAdapter.notifyItemRangeRemoved(1, scheduleCount);

        int friendCount = mFriendEntries.size();

        mFriendEntries = new ArrayList<>();
        mCreateTaskAdapter.notifyItemRangeRemoved(1 + 1, friendCount);
    }

    private void removeListenerHelper() { // keyboard hack
        Assert.assertTrue(mScheduleTimes != null);

        mScheduleTimes.removeOnChildAttachStateChangeListener(mOnChildAttachStateChangeListener);
    }

    @Override
    public void onFriendSelected(@NonNull UserData userData) {
        Assert.assertTrue(mData != null);

        if (mFriendPosition == null) {
            clearParent();

            mCreateTaskAdapter.addFriendEntry(userData);
        } else {
            Assert.assertTrue(mFriendPosition >= 0);

            mFriendEntries.set(mFriendPosition, userData);

            mCreateTaskAdapter.notifyItemChanged(1 + mScheduleEntries.size() + 1 + mFriendPosition);

            mFriendPosition = null;
        }
    }

    @Override
    public void onFriendDeleted() {
        Assert.assertTrue(mFriendPosition != null);
        Assert.assertTrue(mFriendPosition >= 0);
        Assert.assertTrue(mData != null);

        mFriendEntries.remove(mFriendPosition.intValue());

        mCreateTaskAdapter.notifyItemRemoved(1 + mScheduleEntries.size() + 1 + mFriendPosition);

        mFriendPosition = null;
    }

    @Override
    public void onFriendCancel() {
        if (mFriendPosition != null) {
            Assert.assertTrue(mFriendPosition >= 0);

            mFriendPosition = null;
        }
    }

    public static class ScheduleHint implements Parcelable {
        @NonNull
        final Date mDate;

        @Nullable
        final TimePair mTimePair;

        public ScheduleHint(@NonNull Date date) { // root group list
            mDate = date;
            mTimePair = null;
        }

        public ScheduleHint(@NonNull Date date, @NonNull HourMinute hourMinute) { // group list for group
            mDate = date;
            mTimePair = new TimePair(hourMinute);
        }

        public ScheduleHint(@NonNull Date date, @Nullable TimePair timePair) { // join instances, parcelable
            mDate = date;
            mTimePair = timePair;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mDate, 0);

            if (mTimePair == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeParcelable(mTimePair, 0);
            }
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator<ScheduleHint>() {
            @Override
            public ScheduleHint createFromParcel(Parcel source) {
                Date date = source.readParcelable(Date.class.getClassLoader());
                Assert.assertTrue(date != null);

                boolean hasTimePair = (source.readInt() == 1);
                TimePair timePair = (hasTimePair ? source.readParcelable(HourMinute.class.getClassLoader()) : null);

                return new ScheduleHint(date, timePair);
            }

            @Override
            public ScheduleHint[] newArray(int size) {
                return new ScheduleHint[size];
            }
        };
    }

    protected class CreateTaskAdapter extends RecyclerView.Adapter<CreateTaskAdapter.Holder> {
        private static final int TYPE_SCHEDULE = 0;
        private static final int TYPE_NOTE = 1;

        private final TextWatcher mNameListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mNote = s.toString();
            }
        };

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_SCHEDULE: {
                    View scheduleRow = getLayoutInflater().inflate(R.layout.row_schedule, parent, false);

                    TextInputLayout scheduleLayout = (TextInputLayout) scheduleRow.findViewById(R.id.schedule_layout);
                    Assert.assertTrue(scheduleLayout != null);

                    EditText scheduleTime = (EditText) scheduleRow.findViewById(R.id.schedule_text);
                    Assert.assertTrue(scheduleTime != null);

                    return new ScheduleHolder(scheduleRow, scheduleLayout, scheduleTime);
                }
                case TYPE_NOTE: {
                    View noteRow = getLayoutInflater().inflate(R.layout.row_note, parent, false);

                    EditText noteText = (EditText) noteRow.findViewById(R.id.note_text);
                    Assert.assertTrue(noteText != null);

                    return new NoteHolder(noteRow, noteText);
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            if (position == 0) {
                ScheduleHolder scheduleHolder = (ScheduleHolder) holder;

                scheduleHolder.mScheduleLayout.setHint(getString(R.string.parentTask));
                scheduleHolder.mScheduleLayout.setError(null);

                if (mParent != null)
                    scheduleHolder.mScheduleText.setText(mParent.Name);
                else
                    scheduleHolder.mScheduleText.setText(null);

                scheduleHolder.mScheduleText.setEnabled(true);

                scheduleHolder.mScheduleText.setOnClickListener(v -> {
                    ParentPickerFragment parentPickerFragment = ParentPickerFragment.newInstance(mParent != null);
                    parentPickerFragment.show(getSupportFragmentManager(), PARENT_PICKER_FRAGMENT_TAG);
                    parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);
                });
            } else if (position < 1 + mScheduleEntries.size()) {
                ScheduleHolder scheduleHolder = (ScheduleHolder) holder;

                ScheduleEntry scheduleEntry = mScheduleEntries.get(position - 1);
                Assert.assertTrue(scheduleEntry != null);

                scheduleHolder.mScheduleLayout.setHint(null);
                scheduleHolder.mScheduleLayout.setError(scheduleEntry.mError);

                scheduleHolder.mScheduleText.setText(scheduleEntry.getText(mData.CustomTimeDatas, CreateTaskActivity.this));

                scheduleHolder.mScheduleText.setEnabled(true);

                scheduleHolder.mScheduleText.setOnClickListener(v -> scheduleHolder.onTextClick());
            } else if (position == 1 + mScheduleEntries.size()) {
                ScheduleHolder scheduleHolder = (ScheduleHolder) holder;

                scheduleHolder.mScheduleLayout.setHint(getString(R.string.addReminder));
                scheduleHolder.mScheduleLayout.setError(null);

                scheduleHolder.mScheduleText.setText(null);

                scheduleHolder.mScheduleText.setEnabled(true);

                scheduleHolder.mScheduleText.setOnClickListener(v -> {
                    Assert.assertTrue(mCreateTaskAdapter != null);
                    Assert.assertTrue(mHourMinutePickerPosition == null);

                    ScheduleDialogFragment scheduleDialogFragment = ScheduleDialogFragment.newInstance(firstScheduleEntry().getScheduleDialogData(Date.today(), mScheduleHint), false);
                    scheduleDialogFragment.initialize(mData.CustomTimeDatas, mScheduleDialogListener);
                    scheduleDialogFragment.show(getSupportFragmentManager(), SCHEDULE_DIALOG_TAG);
                });
            } else if (position < 1 + mScheduleEntries.size() + 1 + mFriendEntries.size()) {
                ScheduleHolder scheduleHolder = (ScheduleHolder) holder;

                int friendPosition = position - 1 - mScheduleEntries.size() - 1;

                UserData friendEntry = mFriendEntries.get(position - 1 - mScheduleEntries.size() - 1);
                Assert.assertTrue(friendEntry != null);

                scheduleHolder.mScheduleLayout.setHint(null);
                scheduleHolder.mScheduleLayout.setError(null);

                scheduleHolder.mScheduleText.setText(friendEntry.displayName + " (" + friendEntry.email + ")");

                scheduleHolder.mScheduleText.setEnabled(true);

                scheduleHolder.mScheduleText.setOnClickListener(v -> {
                    Assert.assertTrue(mCreateTaskAdapter != null);
                    Assert.assertTrue(mFriendPosition == null);

                    mFriendPosition = friendPosition;

                    ArrayList<String> chosenEmails = Stream.of(mFriendEntries)
                            .map(userData -> userData.email)
                            .collect(Collectors.toCollection(ArrayList::new));

                    FriendPickerFragment friendPickerFragment = FriendPickerFragment.newInstance(true, chosenEmails);
                    friendPickerFragment.show(getSupportFragmentManager(), FRIEND_PICKER_DIALOG_TAG);
                });
            } else if (position == 1 + mScheduleEntries.size() + 1 + mFriendEntries.size()) {
                ScheduleHolder scheduleHolder = (ScheduleHolder) holder;

                scheduleHolder.mScheduleLayout.setHint(getString(R.string.addFriend));
                scheduleHolder.mScheduleLayout.setError(null);

                scheduleHolder.mScheduleText.setText(null);

                scheduleHolder.mScheduleText.setEnabled(MainActivity.getUserData() != null);

                scheduleHolder.mScheduleText.setOnClickListener(v -> {
                    Assert.assertTrue(mCreateTaskAdapter != null);
                    Assert.assertTrue(mFriendPosition == null);

                    ArrayList<String> chosenEmails = Stream.of(mFriendEntries)
                            .map(friendEntry -> friendEntry.email)
                            .collect(Collectors.toCollection(ArrayList::new));

                    FriendPickerFragment friendPickerFragment = FriendPickerFragment.newInstance(false, chosenEmails);
                    friendPickerFragment.show(getSupportFragmentManager(), FRIEND_PICKER_DIALOG_TAG);
                });
            } else {
                Assert.assertTrue(position == 1 + mScheduleEntries.size() + 1 + mFriendEntries.size() + 1);

                NoteHolder noteHolder = (NoteHolder) holder;

                noteHolder.mNoteText.setText(mNote);

                noteHolder.mNoteText.removeTextChangedListener(mNameListener);
                noteHolder.mNoteText.addTextChangedListener(mNameListener);

                noteHolder.mNoteText.setOnFocusChangeListener((v, hasFocus) -> mNoteHasFocus = hasFocus);
            }
        }

        @Override
        public int getItemCount() {
            return (1 + mScheduleEntries.size() + 1 + mFriendEntries.size() + 1 + 1);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_SCHEDULE;
            } else if (position < 1 + mScheduleEntries.size()) {
                return TYPE_SCHEDULE;
            } else if (position == 1 + mScheduleEntries.size()) {
                return TYPE_SCHEDULE;
            } else if (position < 1 + mScheduleEntries.size() + 1 + mFriendEntries.size()) {
                return TYPE_SCHEDULE;
            } else if (position == 1 + mScheduleEntries.size() + 1 + mFriendEntries.size()) {
                return TYPE_SCHEDULE;
            } else {
                Assert.assertTrue(position == 1 + mScheduleEntries.size() + 1 + mFriendEntries.size() + 1);

                return TYPE_NOTE;
            }
        }

        void addScheduleEntry(ScheduleEntry scheduleEntry) {
            Assert.assertTrue(scheduleEntry != null);

            int position = 1 + mScheduleEntries.size();

            mScheduleEntries.add(scheduleEntry);
            notifyItemInserted(position);
        }

        void addFriendEntry(UserData friendEntry) {
            Assert.assertTrue(friendEntry != null);

            int position = 1 + mScheduleEntries.size() + 1 + mFriendEntries.size();

            mFriendEntries.add(friendEntry);
            notifyItemInserted(position);
        }

        abstract class Holder extends RecyclerView.ViewHolder {
            Holder(@NonNull View view) {
                super(view);
            }
        }

        class ScheduleHolder extends Holder {
            final TextInputLayout mScheduleLayout;
            final EditText mScheduleText;

            ScheduleHolder(@NonNull View scheduleRow, @NonNull TextInputLayout scheduleLayout, @NonNull EditText scheduleText) {
                super(scheduleRow);

                mScheduleLayout = scheduleLayout;
                mScheduleText = scheduleText;
            }

            void onTextClick() {
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mHourMinutePickerPosition == null);

                mHourMinutePickerPosition = getAdapterPosition();

                ScheduleEntry scheduleEntry = mScheduleEntries.get(mHourMinutePickerPosition - 1);
                Assert.assertTrue(scheduleEntry != null);

                ScheduleDialogFragment scheduleDialogFragment = ScheduleDialogFragment.newInstance(scheduleEntry.getScheduleDialogData(Date.today(), mScheduleHint), true);
                scheduleDialogFragment.initialize(mData.CustomTimeDatas, mScheduleDialogListener);
                scheduleDialogFragment.show(getSupportFragmentManager(), SCHEDULE_DIALOG_TAG);
            }
        }

        class NoteHolder extends Holder {
            final EditText mNoteText;

            NoteHolder(@NonNull View scheduleRow, @NonNull EditText noteText) {
                super(scheduleRow);

                mNoteText = noteText;
            }
        }
    }
}
