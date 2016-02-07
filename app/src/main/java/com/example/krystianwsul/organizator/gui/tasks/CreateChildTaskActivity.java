package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

public class CreateChildTaskActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<DomainFactory> {
    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private boolean mFirstLoad;

    private EditText mCreateChildTaskName;
    private Button mCreateChildTaskSave;

    public static Intent getCreateIntent(Context context, Task parentTask) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_KEY, parentTask.getId());
        return intent;
    }

    public static Intent getEditIntent(Context context, Task childTask) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(CHILD_TASK_ID_KEY, childTask.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_child_task);

        mFirstLoad = (savedInstanceState == null);

        mCreateChildTaskName = (EditText) findViewById(R.id.create_child_task_name);
        mCreateChildTaskSave = (Button) findViewById(R.id.create_child_task_save);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, final DomainFactory domainFactory) {
        Intent intent = getIntent();
        Task parentTask = null;
        Task childTask = null;
        if (intent.hasExtra(PARENT_TASK_ID_KEY)) {
            int parentTaskId = intent.getIntExtra(PARENT_TASK_ID_KEY, -1);
            Assert.assertTrue(parentTaskId != -1);

            parentTask = domainFactory.getTask(parentTaskId);
            Assert.assertTrue(parentTask != null);
        } else {
            Assert.assertTrue(intent.hasExtra(CHILD_TASK_ID_KEY));

            int childTaskId = intent.getIntExtra(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(childTaskId != -1);

            childTask = domainFactory.getTask(childTaskId);
            Assert.assertTrue(childTask != null);
        }

        final Task finalParentTask = parentTask;
        final Task finalChildTask = childTask;

        if (mFirstLoad && finalChildTask != null)
            mCreateChildTaskName.setText(finalChildTask.getName());

        mCreateChildTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mCreateChildTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.task_name_toast));
                    messageDialogFragment.show(getSupportFragmentManager(), "empty_name");
                    return;
                }

                if (finalParentTask != null)
                    domainFactory.createChildTask(finalParentTask, name, TimeStamp.getNow());
                else
                    domainFactory.updateChildTask(finalChildTask, name);

                domainFactory.save();

                finish();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mCreateChildTaskSave.setOnClickListener(null);
    }
}