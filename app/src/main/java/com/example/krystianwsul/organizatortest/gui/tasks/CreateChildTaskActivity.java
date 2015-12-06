package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

public class CreateChildTaskActivity extends AppCompatActivity {
    private static String PARENT_TASK_ID_KEY = "parentTaskId";
    public static String NEW_PARENT_TASK_ID_KEY = "newParentTaskId";

    public static Intent getIntent(Context context, Task parentTask) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_KEY, parentTask.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_child_task);

        int parentTaskId = getIntent().getIntExtra(PARENT_TASK_ID_KEY, -1);
        Assert.assertTrue(parentTaskId != -1);

        final Task parentTask = TaskFactory.getInstance().getTask(parentTaskId);
        Assert.assertTrue(parentTask != null);

        final EditText createChildTaskName = (EditText) findViewById(R.id.create_child_task_name);

        Button createChildTaskSave = (Button) findViewById(R.id.create_child_task_save);
        createChildTaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = createChildTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(v.getContext(), R.string.task_name_toast, Toast.LENGTH_SHORT).show();
                    createChildTaskName.requestFocus();
                    return;
                }

                Pair<ChildTask, Task> newPair = TaskFactory.getInstance().addChildTask(parentTask, name);

                Intent result = new Intent();
                result.putExtra(NEW_PARENT_TASK_ID_KEY, newPair.second.getId());
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }
}