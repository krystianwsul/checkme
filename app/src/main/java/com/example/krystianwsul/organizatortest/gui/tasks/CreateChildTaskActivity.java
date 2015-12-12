package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

public class CreateChildTaskActivity extends AppCompatActivity {
    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    public static Intent getCreateIntent(Context context, Task parentTask) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_KEY, parentTask.getId());
        return intent;
    }

    public static Intent getEditIntent(Context context, ChildTask childTask) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(CHILD_TASK_ID_KEY, childTask.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_child_task);

        Intent intent = getIntent();
        Task parentTask = null;
        ChildTask childTask = null;
        if (intent.hasExtra(PARENT_TASK_ID_KEY)) {
            int parentTaskId = intent.getIntExtra(PARENT_TASK_ID_KEY, -1);
            Assert.assertTrue(parentTaskId != -1);

            parentTask = TaskFactory.getInstance().getTask(parentTaskId);
            Assert.assertTrue(parentTask != null);
        } else {
            Assert.assertTrue(intent.hasExtra(CHILD_TASK_ID_KEY));

            int childTaskId = intent.getIntExtra(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(childTaskId != -1);

            childTask = (ChildTask) TaskFactory.getInstance().getTask(childTaskId);
            Assert.assertTrue(childTask != null);
        }

        final Task finalParentTask = parentTask;
        final ChildTask finalChildTask = childTask;

        final EditText createChildTaskName = (EditText) findViewById(R.id.create_child_task_name);
        if (savedInstanceState == null && finalChildTask != null)
            createChildTaskName.setText(finalChildTask.getName());

        Button createChildTaskSave = (Button) findViewById(R.id.create_child_task_save);
        createChildTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = createChildTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.task_name_toast));
                    messageDialogFragment.show(getSupportFragmentManager(), "empty_name");
                    return;
                }

                if (finalParentTask != null) {
                    TaskFactory.getInstance().createChildTask(finalParentTask, name);
                } else {
                    finalChildTask.setName(name);
                }

                finish();
            }
        });
    }
}