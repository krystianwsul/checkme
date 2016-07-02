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

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateChildTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateChildTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateChildTaskLoader.Data> {
    private static final String TASK_ID_KEY = "taskId";
    private static final String TASK_IDS_KEY = "taskIds";

    private static final String PARENT_TASK_ID_HINT_KEY = "parentTaskIdHint";

    private static final String DISCARD_TAG = "discard";

    private Bundle mSavedInstanceState;
    private EditText mCreateChildTaskName;

    private Integer mTaskId = null;
    private ArrayList<Integer> mTaskIds;

    private Integer mParentTaskIdHint = null;

    private CreateChildTaskLoader.Data mData;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = CreateChildTaskActivity.this::finish;

    public static Intent getCreateIntent(Context context, int parentTaskIdHint) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, int parentTaskIdHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getEditIntent(Context context, int taskId) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
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

        menu.findItem(R.id.action_create_child_task_save).setVisible((mParentTaskIdHint != null) || (mData != null));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_child_task_save:
                String name = mCreateChildTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_child_task_frame);
                Assert.assertTrue(parentFragment != null);

                int parentTaskId = parentFragment.getParentTaskId();

                if (mParentTaskIdHint != null) {
                    Assert.assertTrue(mTaskId == null);
                    Assert.assertTrue(mData.ChildTaskData == null);

                    if (mTaskIds != null)
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createJoinChildTask(mData.DataId, parentTaskId, name, mTaskIds);
                    else
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createChildTask(mData.DataId, parentTaskId, name);
                } else {
                    Assert.assertTrue(mTaskId != null);
                    Assert.assertTrue(mData.ChildTaskData != null);
                    Assert.assertTrue(mTaskIds == null);

                    DomainFactory.getDomainFactory(CreateChildTaskActivity.this).updateChildTask(mData.DataId, mTaskId, name, parentTaskId);
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

        mSavedInstanceState = savedInstanceState;

        mCreateChildTaskName = (EditText) findViewById(R.id.create_child_task_name);
        Assert.assertTrue(mCreateChildTaskName != null);

        Intent intent = getIntent();
        if (intent.hasExtra(PARENT_TASK_ID_HINT_KEY)) {
            mParentTaskIdHint = intent.getIntExtra(PARENT_TASK_ID_HINT_KEY, -1);
            Assert.assertTrue(mParentTaskIdHint != -1);

            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
        } else {
            Assert.assertTrue(intent.hasExtra(TASK_ID_KEY));
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));

            mTaskId = intent.getIntExtra(TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        }

        if (mSavedInstanceState != null)
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
        return new CreateChildTaskLoader(this, mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateChildTaskLoader.Data> loader, final CreateChildTaskLoader.Data data) {
        mData = data;

        mCreateChildTaskName.setVisibility(View.VISIBLE);

        if (mSavedInstanceState == null) {
            if (data.ChildTaskData != null)
                mCreateChildTaskName.setText(data.ChildTaskData.Name);
        }

        ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_child_task_frame);
        if (parentFragment == null) {
            if (mParentTaskIdHint != null) {
                Assert.assertTrue(mTaskId == null);

                if (mTaskIds != null)
                    parentFragment = ParentFragment.getJoinInstance(mParentTaskIdHint, mTaskIds);
                else
                    parentFragment = ParentFragment.getCreateInstance(mParentTaskIdHint);
            } else {
                Assert.assertTrue(mTaskId != null);
                Assert.assertTrue(mTaskIds == null);

                parentFragment = ParentFragment.getEditInstance(mTaskId);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.create_child_task_frame, parentFragment)
                    .commitAllowingStateLoss();
        }

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
        if (mData == null)
            return false;

        if (mTaskId != null) {
            Assert.assertTrue(mParentTaskIdHint == null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mData.ChildTaskData != null);

            if (!mCreateChildTaskName.getText().toString().equals(mData.ChildTaskData.Name))
                return true;

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_child_task_frame);
            Assert.assertTrue(parentFragment != null);

            if (parentFragment.dataChanged())
                return true;
        } else {
            Assert.assertTrue(mParentTaskIdHint != null);

            if (!TextUtils.isEmpty(mCreateChildTaskName.getText()))
                return true;

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_child_task_frame);
            Assert.assertTrue(parentFragment != null);

            if (parentFragment.dataChanged())
                return true;
        }

        return false;
    }
}