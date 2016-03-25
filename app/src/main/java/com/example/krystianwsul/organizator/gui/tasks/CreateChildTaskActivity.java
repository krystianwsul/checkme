package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.CreateChildTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateChildTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<CreateChildTaskLoader.Data> {
    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private boolean mFirstLoad;

    private EditText mCreateChildTaskName;
    private Button mCreateChildTaskSave;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mChildTaskId = null;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_child_task);

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
                mCreateChildTaskSave.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });

        mCreateChildTaskSave = (Button) findViewById(R.id.create_child_task_save);
        Assert.assertTrue(mCreateChildTaskSave != null);

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

    private void updateGui(final CreateChildTaskLoader.Data data)
    {
        if (mFirstLoad && data != null)
            mCreateChildTaskName.setText(data.Name);

        mCreateChildTaskSave.setEnabled(!TextUtils.isEmpty(mCreateChildTaskName.getText().toString().trim()));
        mCreateChildTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mCreateChildTaskName.getText().toString().trim();

                if (mParentTaskId != null) {
                    Assert.assertTrue(mChildTaskId == null);
                    Assert.assertTrue(data == null);

                    if (mTaskIds != null)
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createJoinChildTask(mParentTaskId, name, mTaskIds);
                    else
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createChildTask(mParentTaskId, name);
                } else {
                    Assert.assertTrue(mChildTaskId != null);
                    Assert.assertTrue(data != null);
                    Assert.assertTrue(mTaskIds == null);

                    DomainFactory.getDomainFactory(CreateChildTaskActivity.this).updateChildTask(data.DataId, mChildTaskId, name);
                }

                finish();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<CreateChildTaskLoader.Data> loader) {
    }
}