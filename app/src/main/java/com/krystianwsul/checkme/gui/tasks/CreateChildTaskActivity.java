package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.CreateChildTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateChildTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateChildTaskLoader.Data> {
    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private boolean mFirstLoad;

    private EditText mCreateChildTaskName;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mChildTaskId = null;

    private CreateChildTaskLoader.Data mData;

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

        boolean save = !TextUtils.isEmpty(mCreateChildTaskName.getText().toString().trim());
        menu.findItem(R.id.action_create_child_task_save).setVisible(save);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_child_task_save:
                String name = mCreateChildTaskName.getText().toString().trim();

                if (mParentTaskId != null) {
                    Assert.assertTrue(mChildTaskId == null);
                    Assert.assertTrue(mData == null);

                    if (mTaskIds != null)
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createJoinChildTask(mParentTaskId, name, mTaskIds);
                    else
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createChildTask(mParentTaskId, name);
                } else {
                    Assert.assertTrue(mChildTaskId != null);
                    Assert.assertTrue(mData != null);
                    Assert.assertTrue(mTaskIds == null);

                    DomainFactory.getDomainFactory(CreateChildTaskActivity.this).updateChildTask(mData.DataId, mChildTaskId, name);
                }

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

        mFirstLoad = (savedInstanceState == null);

        mCreateChildTaskName = (EditText) findViewById(R.id.create_child_task_name);
        Assert.assertTrue(mCreateChildTaskName != null);

        mCreateChildTaskName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                invalidateOptionsMenu();
            }
        });

        Intent intent = getIntent();
        if (intent.hasExtra(PARENT_TASK_ID_KEY)) {
            mParentTaskId = intent.getIntExtra(PARENT_TASK_ID_KEY, -1);

            if (intent.hasExtra(TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }

            updateGui(null);
        } else {
            Assert.assertTrue(intent.hasExtra(CHILD_TASK_ID_KEY));
            Assert.assertTrue(!intent.hasExtra(TASK_IDS_KEY));

            mChildTaskId = intent.getIntExtra(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(mChildTaskId != -1);

            getSupportLoaderManager().initLoader(0, null, this);
        }

        if (mFirstLoad)
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public Loader<CreateChildTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        return new CreateChildTaskLoader(this, mChildTaskId);
    }

    @Override
    public void onLoadFinished(Loader<CreateChildTaskLoader.Data> loader, final CreateChildTaskLoader.Data data) {
        updateGui(data);
    }

    private void updateGui(final CreateChildTaskLoader.Data data) {
        mCreateChildTaskName.setVisibility(View.VISIBLE);

        if (mFirstLoad && data != null)
            mCreateChildTaskName.setText(data.Name);

        mData = data;

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateChildTaskLoader.Data> loader) {
    }
}