package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class CreateTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {
    private static final String DISCARD_TAG = "discard";

    static final String TASK_ID_KEY = "taskId";
    static final String TASK_IDS_KEY = "taskIds";

    static final String PARENT_TASK_ID_HINT_KEY = "parentTaskIdHint";
    static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private Bundle mSavedInstanceState;

    private TextInputLayout mToolbarLayout;
    private EditText mToolbarEditText;

    private Spinner mCreateTaskSpinner;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateTaskActivity.this::finish;

    private Integer mTaskId;
    private ArrayList<Integer> mTaskIds;

    private CreateTaskActivity.ScheduleHint mScheduleHint;
    private Integer mParentTaskIdHint = null;
    private String mNameHint = null;

    private CreateTaskLoader.Data mData;

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
        Assert.assertTrue(mToolbarEditText != null);

        menu.findItem(R.id.action_create_task_save).setVisible(mData != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mToolbarEditText != null);

                updateError();

                String name = mToolbarEditText.getText().toString().trim();
                if (TextUtils.isEmpty(name))
                    break;

                boolean finish;

                switch (mCreateTaskSpinner.getSelectedItemPosition()) {
                    case 0: // schedule
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

                        if (finish)
                            finish();
                        break;
                    case 1: // parent task
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

                        if (finish)
                            finish();
                        break;
                    case 2: // no reminder
                        if (mTaskId != null) {
                            Assert.assertTrue(mData.TaskData != null);
                            Assert.assertTrue(mTaskIds == null);

                            DomainFactory.getDomainFactory(this).updateRootTask(mData.DataId, mTaskId, name);
                        } else if (mTaskIds != null) {
                            Assert.assertTrue(mData.TaskData == null);

                            DomainFactory.getDomainFactory(this).createJoinRootTask(mData.DataId, name, mTaskIds);
                        } else {
                            Assert.assertTrue(mData.TaskData == null);

                            DomainFactory.getDomainFactory(this).createRootTask(mData.DataId, name);
                        }

                        finish();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

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

        mCreateTaskSpinner = (Spinner) findViewById(R.id.create_task_spinner);
        Assert.assertTrue(mCreateTaskSpinner != null);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.task_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateTaskSpinner.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent.hasExtra(TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));
            Assert.assertTrue(!intent.hasExtra(PARENT_TASK_ID_HINT_KEY));
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
        List<Integer> excludedTaskIds = new ArrayList<>();

        if (mTaskId != null) {
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskIdHint == null);

            excludedTaskIds.add(mTaskId);
        } else if (mTaskIds != null) {
            excludedTaskIds.addAll(mTaskIds);
        }

        return new CreateTaskLoader(this, mTaskId, excludedTaskIds);
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
                Assert.assertTrue(mParentTaskIdHint == null);
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

                updateNameError();
            }
        });

        mCreateTaskSpinner.setVisibility(View.VISIBLE);

        if (mSavedInstanceState == null) {
            if ((mData.TaskData != null && mData.TaskData.ParentTaskId != null) || (mParentTaskIdHint != null)) {
                Assert.assertTrue(mScheduleHint == null);

                mCreateTaskSpinner.setSelection(1);
            } else if (mData.TaskData != null && mData.TaskData.ScheduleType == null) {
                Assert.assertTrue(mData.TaskData.ParentTaskId == null);
                Assert.assertTrue(mParentTaskIdHint == null);
                Assert.assertTrue(mScheduleHint == null);

                mCreateTaskSpinner.setSelection(2);
            }
        }

        mCreateTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                Assert.assertTrue(parentFragment != null);

                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                switch (i) {
                    case 0: // schedule
                        parentFragment.clear();

                        break;
                    case 1: // parent
                        schedulePickerFragment.clear();

                        break;
                    case 2: // no reminder
                        parentFragment.clear();
                        schedulePickerFragment.clear();

                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

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

    private void updateError() {
        updateNameError();

        ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
        Assert.assertTrue(parentFragment != null);
        parentFragment.updateError();

        SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
        Assert.assertTrue(schedulePickerFragment != null);
        schedulePickerFragment.updateError();
    }

    private void updateNameError() {
        if (TextUtils.isEmpty(mToolbarEditText.getText())) {
            mToolbarLayout.setError(getString(R.string.nameError));
        } else {
            mToolbarLayout.setError(null);
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

            if (!mToolbarEditText.getText().toString().equals(mData.TaskData.Name))
                return true;

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
            Assert.assertTrue(parentFragment != null);

            SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
            Assert.assertTrue(schedulePickerFragment != null);

            if (mData.TaskData.ParentTaskId != null) {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 1)
                    return true;

                if (parentFragment.dataChanged())
                    return true;
            } else if (mData.TaskData.ScheduleType != null) {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 0)
                    return true;

                if (schedulePickerFragment.dataChanged())
                    return true;
            } else {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 2)
                    return true;
            }

            return false;
        } else {
            if (!TextUtils.isEmpty(mToolbarEditText.getText()))
                return true;

            if (mCreateTaskSpinner.getSelectedItemPosition() == 2)
                return true;

            if (mParentTaskIdHint != null) {
                Assert.assertTrue(mScheduleHint == null);

                if (mCreateTaskSpinner.getSelectedItemPosition() != 1)
                    return true;

                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                Assert.assertTrue(parentFragment != null);

                return parentFragment.dataChanged();
            } else {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 0)
                    return true;

                SchedulePickerFragment schedulePickerFragmentFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragmentFragment != null);

                return schedulePickerFragmentFragment.dataChanged();
            }
        }
    }

    public static class ScheduleHint implements Parcelable {
        public final Date mDate;
        public final TimePair mTimePair;

        public ScheduleHint(Date date) {
            Assert.assertTrue(date != null);

            mDate = date;
            mTimePair = null;
        }

        public ScheduleHint(Date date, HourMinute hourMinute) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(hourMinute != null);

            mDate = date;
            mTimePair = new TimePair(hourMinute);
        }

        public ScheduleHint(Date date, TimePair timePair) {
            Assert.assertTrue(date != null);

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
}
