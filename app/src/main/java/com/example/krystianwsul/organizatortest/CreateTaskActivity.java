package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

public class CreateTaskActivity extends AppCompatActivity {

    private static final String INTENT_KEY = "parentTaskId";

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        return intent;
    }

    public static Intent getIntent(Context context, Task task) {
        Intent intent = new Intent(context, CreateTaskActivity.class);
        intent.putExtra(INTENT_KEY, task.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        Integer parentTaskId = null;
        if (getIntent().hasExtra(INTENT_KEY)) {
            parentTaskId = getIntent().getIntExtra(INTENT_KEY, -1);
            Assert.assertTrue(parentTaskId != -1);

            Task parentTask = TaskFactory.getInstance().getTask(parentTaskId);
            Assert.assertTrue(parentTask != null);
        }

        Spinner createTaskSpinner = (Spinner) findViewById(R.id.create_task_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        createTaskSpinner.setAdapter(adapter);

        createTaskSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        //single
                        break;
                    case 1:
                        //daily
                        break;
                    case 2:
                        //weekly
                        break;
                    default:
                        throw new IllegalArgumentException("invalid spinner selection: " + position);
                }
            }
        });
    }
}
