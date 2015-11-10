package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.InstanceGroup;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;

import java.util.ArrayList;

public class ShowGroup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        Group group = getGroup();

        TextView instanceName = (TextView) findViewById(R.id.show_instance_name);
        instanceName.setText(group.getName());

        TextView instanceDatetime = (TextView) findViewById(R.id.show_instance_datetime);
        instanceDatetime.setText(group.getScheduleText(this));

        ListView showInstanceList = (ListView) findViewById(R.id.show_instance_list);
        if (group.getChildGroups().isEmpty())
            showInstanceList.setVisibility(View.GONE);
        else
            showInstanceList.setAdapter(new GroupAdapter(this, new ArrayList(group.getChildGroups())));

        showInstanceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = (Group) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowGroup.class);
                intent.putExtra(group.getIntentKey(), group.getIntentValue());
                startActivity(intent);
            }
        });
    }

    private Group getGroup() {
        Intent intent = getIntent();

        if (intent.hasExtra("singleInstanceId"))
            return new InstanceGroup(SingleInstance.getSingleInstance(intent.getIntExtra("singleInstanceId", -1)));

        if (intent.hasExtra("dailyInstanceId"))
            return new InstanceGroup(DailyInstance.getDailyInstance(intent.getIntExtra("dailyInstanceId", -1)));

        if (intent.hasExtra("weeklyInstanceId"))
            return new InstanceGroup(WeeklyInstance.getWeeklyInstance(intent.getIntExtra("weeklyInstanceId", -1)));

        throw new IllegalArgumentException("no instance id found.");
    }
}
