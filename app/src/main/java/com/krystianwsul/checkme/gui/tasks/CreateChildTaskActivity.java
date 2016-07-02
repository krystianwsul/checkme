package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateChildTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateChildTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateChildTaskLoader.Data> {
    private static final String PARENT_FRAGMENT_TAG = "parentFragment";

    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private static final String DISCARD_TAG = "discard";

    private boolean mFirstLoad;

    private EditText mCreateChildTaskName;
    private TextView mCreateChildTaskParent;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mChildTaskId = null;

    private CreateChildTaskLoader.Data mData;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateChildTaskActivity.this::finish;

    private final ParentFragment.Listener mParentFragmentListener = taskData -> {
        Assert.assertTrue(taskData != null);

        mCreateChildTaskParent.setText(taskData.Name);
    };

    public static Intent getCreateIntent(Context context, int parentTaskId) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_KEY, parentTaskId);
        return intent;
    }

    public static Intent getJoinIntent(Context context, int parentTaskId, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_KEY, parentTaskId);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    public static Intent getEditIntent(Context context, int childTaskId) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(CHILD_TASK_ID_KEY, childTaskId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_child_task, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Assert.assertTrue(mCreateChildTaskName != null);

        menu.findItem(R.id.action_create_child_task_save).setVisible((mParentTaskId != null) || (mData != null));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_child_task_save:
                String name = mCreateChildTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                if (mParentTaskId != null) {
                    Assert.assertTrue(mChildTaskId == null);
                    Assert.assertTrue(mData.ChildTaskData == null);

                    if (mTaskIds != null)
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createJoinChildTask(mData.DataId, mParentTaskId, name, mTaskIds);
                    else
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createChildTask(mData.DataId, mParentTaskId, name);
                } else {
                    Assert.assertTrue(mChildTaskId != null);
                    Assert.assertTrue(mData.ChildTaskData != null);
                    Assert.assertTrue(mTaskIds == null);

                    DomainFactory.getDomainFactory(CreateChildTaskActivity.this).updateChildTask(mData.DataId, mChildTaskId, name);
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
        setContentView(R.layout.activity_create_child_task);

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_child_task_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Assert.assertTrue(actionBar != null);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        mFirstLoad = (savedInstanceState == null);

        mCreateChildTaskName = (EditText) findViewById(R.id.create_child_task_name);
        Assert.assertTrue(mCreateChildTaskName != null);

        mCreateChildTaskParent = (TextView) findViewById(R.id.create_child_task_parent);
        Assert.assertTrue(mCreateChildTaskParent != null);

        Intent intent = getIntent();
        if (intent.hasExtra(PARENT_TASK_ID_KEY)) {
            mParentTaskId = intent.getIntExtra(PARENT_TASK_ID_KEY, -1);

            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
        } else {
            Assert.assertTrue(intent.hasExtra(CHILD_TASK_ID_KEY));
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));

            mChildTaskId = intent.getIntExtra(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(mChildTaskId != -1);
        }

        if (mFirstLoad)
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        MyCrashlytics.log("CreateChildTaskActivity.onResume");

        super.onResume();
    }

    @Override
    public Loader<CreateChildTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new CreateChildTaskLoader(this, mChildTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateChildTaskLoader.Data> loader, final CreateChildTaskLoader.Data data) {
        mData = data;

        mCreateChildTaskName.setVisibility(View.VISIBLE);

        if (mFirstLoad && (data.ChildTaskData != null))
            mCreateChildTaskName.setText(data.ChildTaskData.Name);

        CreateChildTaskLoader.TaskData parentTaskData;
        if (mParentTaskId != null) {
            Assert.assertTrue(mChildTaskId == null);
            Assert.assertTrue(mData.ChildTaskData == null);

            parentTaskData = findTaskData(mParentTaskId);
        } else {
            Assert.assertTrue(mChildTaskId != null);
            Assert.assertTrue(mData.ChildTaskData != null);

            parentTaskData = findTaskData(mData.ChildTaskData.ParentTaskId);
        }

        Assert.assertTrue(parentTaskData != null);
        mCreateChildTaskParent.setText(parentTaskData.Name);

        mCreateChildTaskParent.setVisibility(View.VISIBLE);

        mCreateChildTaskParent.setOnClickListener(v -> {
            ParentFragment parentFragment = ParentFragment.newInstance();
            parentFragment.show(getSupportFragmentManager(), PARENT_FRAGMENT_TAG);
            parentFragment.initialize(mData.TaskDatas, mParentFragmentListener);
        });

        ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentByTag(PARENT_FRAGMENT_TAG);
        if (parentFragment != null)
            parentFragment.initialize(mData.TaskDatas, mParentFragmentListener);

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateChildTaskLoader.Data> loader) {
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
        if (mChildTaskId != null) {
            Assert.assertTrue(mParentTaskId == null);
            Assert.assertTrue(mTaskIds == null);

            if (mData.ChildTaskData == null)
                return false;

            if (!mCreateChildTaskName.getText().toString().equals(mData.ChildTaskData.Name))
                return true;

            return false;
        } else {
            Assert.assertTrue(mParentTaskId != null);

            if (!TextUtils.isEmpty(mCreateChildTaskName.getText()))
                return true;

            return false;
        }
    }

    private CreateChildTaskLoader.TaskData findTaskData(int taskId) {
        Assert.assertTrue(mData != null);

        List<CreateChildTaskLoader.TaskData> taskDatas = findTaskDataHelper(mData.TaskDatas, taskId)
                .collect(Collectors.toList());

        Assert.assertTrue(taskDatas.size() == 1);
        return taskDatas.get(0);
    }

    private Stream<CreateChildTaskLoader.TaskData> findTaskDataHelper(Map<Integer, CreateChildTaskLoader.TaskData> taskDatas, int taskId) {
        Assert.assertTrue(taskDatas != null);

        if (taskDatas.containsKey(taskId)) {
            List<CreateChildTaskLoader.TaskData> ret = new ArrayList<>();
            ret.add(taskDatas.get(taskId));
            return Stream.of(ret);
        }

        return Stream.of(taskDatas.values())
                .map(taskData -> findTaskDataHelper(taskData.TaskDatas, taskId))
                .flatMap(stream -> stream);
    }
}