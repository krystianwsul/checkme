package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {
    private static final String DISCARD_TAG = "discard";

    static final String TASK_ID_KEY = "taskId";
    static final String TASK_IDS_KEY = "taskIds";

    static final String PARENT_TASK_ID_HINT_KEY = "parentTaskIdHint";
    static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String PARENT_ID = "parentId";
    private static final String PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment";

    private static final String POSITION_KEY = "position";
    private static final String SCHEDULE_TYPE_CHANGED_KEY = "scheduleTypeChanged";

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

    private TextInputLayout mFragmentParentLayout;
    private TextView mCreateChildTaskParent;

    private CreateTaskLoader.TaskTreeData mParent;

    private final ParentPickerFragment.Listener mParentFragmentListener = taskData -> {
        Assert.assertTrue(taskData != null);

        clearSchedule();

        mParent = taskData;
        mCreateChildTaskParent.setText(taskData.Name);

        updateError();
    };

    private Spinner mCreateRootTaskSpinner;

    private boolean mScheduleTypeChanged = false;

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
        Assert.assertTrue(!hasValueParent() || !hasValueSchedule());

        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mToolbarEditText != null);

                updateError();

                String name = mToolbarEditText.getText().toString().trim();
                if (TextUtils.isEmpty(name))
                    break;

                switch (mCreateTaskSpinner.getSelectedItemPosition()) {
                    case 0: // schedule
                        Assert.assertTrue(!hasValueParent());
                        Assert.assertTrue(hasValueSchedule());

                        if (mTaskId != null) {
                            Assert.assertTrue(mData.TaskData != null);
                            Assert.assertTrue(mTaskIds == null);

                            ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                            if (scheduleFragment != null) {
                                if (scheduleFragment.updateRootTask(mTaskId, name))
                                    finish();
                            } else {
                                DomainFactory.getDomainFactory(this).updateRootTask(mData.DataId, mTaskId, name);
                                finish();
                            }
                        } else if (mTaskIds != null) {
                            Assert.assertTrue(mData.TaskData == null);

                            ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                            if (scheduleFragment != null) {
                                if (scheduleFragment.createRootJoinTask(name, mTaskIds))
                                    finish();
                            } else {
                                DomainFactory.getDomainFactory(this).createJoinRootTask(mData.DataId, name, mTaskIds);
                                finish();
                            }
                        } else {
                            Assert.assertTrue(mData.TaskData == null);

                            ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                            if (scheduleFragment != null) {
                                if (scheduleFragment.createRootTask(name))
                                    finish();
                            } else {
                                DomainFactory.getDomainFactory(this).createRootTask(mData.DataId, name);
                                finish();
                            }
                        }
                        break;
                    case 1: // parent task
                        Assert.assertTrue(hasValueParent());
                        Assert.assertTrue(!hasValueSchedule());

                        if (mTaskId != null) {
                            Assert.assertTrue(mData.TaskData != null);
                            Assert.assertTrue(mTaskIds == null);

                            if (!isValidParent()) {
                                updateErrorParent();
                            } else {
                                DomainFactory.getDomainFactory(this).updateChildTask(mData.DataId, mTaskId, name, mParent.TaskId);

                                finish();
                            }
                        } else if (mTaskIds != null) {
                            Assert.assertTrue(mData.TaskData == null);

                            Assert.assertTrue(!TextUtils.isEmpty(name));
                            Assert.assertTrue(mTaskIds.size() > 1);

                            if (!isValidParent()) {
                                updateErrorParent();
                            } else {
                                DomainFactory.getDomainFactory(this).createJoinChildTask(mData.DataId, mParent.TaskId, name, mTaskIds);

                                finish();
                            }
                        } else {
                            Assert.assertTrue(mData.TaskData == null);

                            if (!isValidParent()) {
                                updateErrorParent();
                            } else {
                                DomainFactory.getDomainFactory(this).createChildTask(mData.DataId, mParent.TaskId, name);

                                finish();
                            }
                        }
                        break;
                    case 2: // no reminder
                        Assert.assertTrue(!hasValueParent());
                        Assert.assertTrue(!hasValueSchedule());

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

        mFragmentParentLayout = (TextInputLayout) findViewById(R.id.fragment_parent_layout);
        Assert.assertTrue(mFragmentParentLayout != null);

        mCreateChildTaskParent = (TextView) findViewById(R.id.create_child_task_parent);
        Assert.assertTrue(mCreateChildTaskParent != null);

        mCreateRootTaskSpinner = (Spinner) findViewById(R.id.schedule_picker_spinner);
        Assert.assertTrue(mCreateRootTaskSpinner != null);

        ArrayAdapter<CharSequence> scheduleAdapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        scheduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateRootTaskSpinner.setAdapter(scheduleAdapter);

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null && mParent != null) {
            outState.putInt(PARENT_ID, mParent.TaskId);
        }

        if (mData != null) {
            outState.putInt(POSITION_KEY, mCreateRootTaskSpinner.getSelectedItemPosition());
            outState.putBoolean(SCHEDULE_TYPE_CHANGED_KEY, mScheduleTypeChanged);
        }
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
                switch (i) {
                    case 0: // schedule
                        clearParent();

                        break;
                    case 1: // parent
                        clearSchedule();

                        break;
                    case 2: // no reminder
                        clearParent();
                        clearSchedule();

                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(PARENT_ID)) {
            int parentId = mSavedInstanceState.getInt(PARENT_ID);
            mParent = findTaskData(parentId);
            Assert.assertTrue(mParent != null);
        } else {
            if (mData.TaskData != null && mData.TaskData.ParentTaskId != null) {
                Assert.assertTrue(mParentTaskIdHint == null);
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mTaskId != null);

                mParent = findTaskData(mData.TaskData.ParentTaskId);
            } else if (mParentTaskIdHint != null) {
                Assert.assertTrue(mTaskId == null);

                mParent = findTaskData(mParentTaskIdHint);
            }
        }

        if (mParent != null)
            mCreateChildTaskParent.setText(mParent.Name);

        mFragmentParentLayout.setVisibility(View.VISIBLE);
        mFragmentParentLayout.setHintAnimationEnabled(true);

        mCreateChildTaskParent.setOnClickListener(v -> {
            ParentPickerFragment parentPickerFragment = ParentPickerFragment.newInstance();
            parentPickerFragment.show(getSupportFragmentManager(), PARENT_PICKER_FRAGMENT_TAG);
            parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);
        });

        ParentPickerFragment parentPickerFragment = (ParentPickerFragment) getSupportFragmentManager().findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG);
        if (parentPickerFragment != null)
            parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);

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
        } else if (mData.TaskData != null) {
            if (mData.TaskData.ScheduleType != null) {
                Assert.assertTrue(mTaskId != null);

                ScheduleType scheduleType = mData.TaskData.ScheduleType;

                Fragment fragment;
                if (scheduleType == ScheduleType.SINGLE) {
                    fragment = SingleScheduleFragment.newInstance(mTaskId);
                    spinnerPosition = 0;
                } else if (scheduleType == ScheduleType.DAILY) {
                    count++;

                    fragment = DailyScheduleFragment.newInstance(mTaskId);
                    spinnerPosition = 1;
                } else if (scheduleType == ScheduleType.WEEKLY) {
                    count++;

                    fragment = WeeklyScheduleFragment.newInstance(mTaskId);
                    spinnerPosition = 2;
                } else {
                    throw new IndexOutOfBoundsException("unknown schedule type");
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.schedule_picker_frame, fragment)
                        .commitAllowingStateLoss();
            } else {
                spinnerPosition = 3;
                count++;
            }
        } else {
            if (mParentTaskIdHint != null) {
                spinnerPosition = 3;
                count++;
            } else {
                spinnerPosition = 0;
                loadFragment(0);
            }
        }
        final int finalCount = count;

        mCreateRootTaskSpinner.setSelection(spinnerPosition);

        mCreateRootTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 4);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                mScheduleTypeChanged = true;

                if (position < 3) {
                    clearParent();

                    loadFragment(position);
                } else {
                    Assert.assertTrue(position == 3);

                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                    Assert.assertTrue(fragment != null);

                    getSupportFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commitAllowingStateLoss();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        invalidateOptionsMenu();

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

    private void updateError() {
        updateNameError();

        updateErrorParent();
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

            if (mData.TaskData.ParentTaskId != null) {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 1)
                    return true;

                if (mParent.TaskId != mData.TaskData.ParentTaskId)
                    return true;

                return false;
            } else if (mData.TaskData.ScheduleType != null) {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 0)
                    return true;

                if (mScheduleTypeChanged)
                    return true;

                ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                if (scheduleFragment == null)
                    return false;

                if (scheduleFragment.dataChanged())
                    return true;

                return false;
            } else {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 2)
                    return true;

                return false;
            }
        } else {
            if (!TextUtils.isEmpty(mToolbarEditText.getText()))
                return true;

            if (mCreateTaskSpinner.getSelectedItemPosition() == 2)
                return true;

            if (mParentTaskIdHint != null) {
                Assert.assertTrue(mScheduleHint == null);

                if (mCreateTaskSpinner.getSelectedItemPosition() != 1)
                    return true;

                if (mParent == null || mParent.TaskId != mParentTaskIdHint)
                    return true;

                return false;
            } else {
                if (mCreateTaskSpinner.getSelectedItemPosition() != 0)
                    return true;

                ScheduleFragment scheduleFragment = (ScheduleFragment) getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                if (scheduleFragment == null)
                    return true;

                if (!(scheduleFragment instanceof SingleScheduleFragment))
                    return true;

                if (scheduleFragment.dataChanged())
                    return true;

                return false;
            }
        }
    }

    private CreateTaskLoader.TaskTreeData findTaskData(int taskId) {
        Assert.assertTrue(mData != null);

        List<CreateTaskLoader.TaskTreeData> taskTreeDatas = findTaskDataHelper(mData.TaskTreeDatas, taskId)
                .collect(Collectors.toList());

        Assert.assertTrue(taskTreeDatas.size() == 1);
        return taskTreeDatas.get(0);
    }

    private Stream<CreateTaskLoader.TaskTreeData> findTaskDataHelper(Map<Integer, CreateTaskLoader.TaskTreeData> taskDatas, int taskId) {
        Assert.assertTrue(taskDatas != null);

        if (taskDatas.containsKey(taskId)) {
            List<CreateTaskLoader.TaskTreeData> ret = new ArrayList<>();
            ret.add(taskDatas.get(taskId));
            return Stream.of(ret);
        }

        return Stream.of(taskDatas.values())
                .map(taskData -> findTaskDataHelper(taskData.TaskDatas, taskId))
                .flatMap(stream -> stream);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isValidParent() {
        if (mData == null)
            return false;

        if (mParent == null)
            return false;

        return true;
    }

    public void updateErrorParent() {
        Assert.assertTrue(mFragmentParentLayout != null);

        if (isValidParent()) {
            mFragmentParentLayout.setError(null);
        } else {
            mFragmentParentLayout.setError(getString(R.string.error_parent));
        }
    }

    public void clearParent() {
        mParent = null;
        mCreateChildTaskParent.setText(null);

        mFragmentParentLayout.setError(null);
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.schedule_picker_frame, fragment)
                .commitAllowingStateLoss();
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
                } else {
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

    public void clearSchedule() {
        mCreateRootTaskSpinner.setSelection(3);
    }

    private boolean hasValueParent() {
        Assert.assertTrue((mParent == null) == TextUtils.isEmpty(mCreateChildTaskParent.getText()));

        return (mParent != null);
    }

    private boolean hasValueSchedule() {
        Assert.assertTrue((mCreateRootTaskSpinner.getSelectedItemPosition() == 3) == (getSupportFragmentManager().findFragmentById(R.id.schedule_picker_frame) == null));

        return (mCreateRootTaskSpinner.getSelectedItemPosition() != 3);
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
