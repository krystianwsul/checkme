package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
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

        TextView instanceName = (TextView) findViewById(R.id.show_instance_name);
        instanceName.setText(instance.getName());

        TextView instanceDatetime = (TextView) findViewById(R.id.show_instance_datetime);
        instanceDatetime.setText(instance.getScheduleText(this));

        ListView showInstanceList = (ListView) findViewById(R.id.show_instance_list);
        if (instance.getChildInstances().isEmpty())
            showInstanceList.setVisibility(View.GONE);
        else
            showInstanceList.setAdapter(new InstanceAdapter(this, new ArrayList(instance.getChildInstances())));

        showInstanceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Instance instance = (Instance) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowInstance.class);
                intent.putExtra(instance.getIntentKey(), instance.getIntentValue());
                startActivity(intent);
            }
        });
    }

    private Instance getInstance() {
        Bundle extras = getIntent().getExtras();

        Integer singleInstanceId = extras.getInt("singleInstanceId");
        if (singleInstanceId != null)
            return SingleInstance.getSingleInstance(singleInstanceId);

        Integer dailyInstanceId = extras.getInt("dailyInstanceId");
        if (dailyInstanceId != null)
            return DailyInstance.getDailyInstance(dailyInstanceId);

        throw new IllegalArgumentException("singleInstanceId == " + singleInstanceId + " and dailyInstanceId == " + dailyInstanceId + " don't match an instance.");
    }
}
