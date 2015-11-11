package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstance extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        Instance instance = getInstance();
        Assert.assertTrue(instance != null);

        TextView showInstanceName = (TextView) findViewById(R.id.show_instance_name);
        showInstanceName.setText(instance.getName());

        TextView showInstanceDetails = (TextView) findViewById(R.id.show_instance_details);
        showInstanceDetails.setText(instance.getScheduleText(this));

        ListView showInstanceList = (ListView) findViewById(R.id.show_instance_list);
        if (instance.getChildInstances().isEmpty())
            showInstanceList.setVisibility(View.GONE);
        else
            showInstanceList.setAdapter(new InstanceAdapter(this, new ArrayList(instance.getChildInstances())));

        showInstanceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Instance childInstance = (Instance) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowInstance.class);
                intent.putExtra(childInstance.getIntentKey(), childInstance.getIntentValue());
                startActivity(intent);
            }
        });
    }

    private Instance getInstance() {
        Intent intent = getIntent();

        if (intent.hasExtra(SingleInstance.INTENT_KEY)) {
            int taskId = intent.getIntExtra(SingleInstance.INTENT_KEY, -1);
            Assert.assertTrue(taskId != -1);
            SingleInstance singleInstance = SingleInstance.getSingleInstance(taskId);
            Assert.assertTrue(singleInstance != null);
            return singleInstance;
        } else if (intent.hasExtra(DailyInstance.INTENT_KEY)) {
            int dailyInstanceId = intent.getIntExtra(DailyInstance.INTENT_KEY, -1);
            Assert.assertTrue(dailyInstanceId != -1);
            DailyInstance dailyInstance = DailyInstance.getDailyInstance(dailyInstanceId);
            Assert.assertTrue(dailyInstance != null);
            return dailyInstance;
        } else if (intent.hasExtra(WeeklyInstance.INTENT_KEY)) {
            int weeklyInstanceId = intent.getIntExtra(WeeklyInstance.INTENT_KEY, -1);
            Assert.assertTrue(weeklyInstanceId != -1);
            WeeklyInstance weeklyInstance = WeeklyInstance.getWeeklyInstance(weeklyInstanceId);
            Assert.assertTrue(weeklyInstance != null);
            return weeklyInstance;
        } else {
            throw new IllegalArgumentException("instance key not found");
        }
    }
}
