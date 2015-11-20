package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.arrayadapters.InstanceAdapter;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowGroupActivity extends AppCompatActivity {
    private static final String INTENT_KEY = "instanceIds";

    public static Intent getIntent(Group group, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);

        ArrayList<Integer> instanceIds = new ArrayList<>();
        for (Instance instance : group.getInstances())
            instanceIds.add(instance.getId());

        intent.putIntegerArrayListExtra(ShowGroupActivity.INTENT_KEY, instanceIds);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        ArrayList<Integer> instanceIds = intent.getIntegerArrayListExtra(INTENT_KEY);
        Assert.assertTrue(instanceIds != null);
        Assert.assertTrue(instanceIds.size() > 1);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Integer instanceId : instanceIds) {
            Instance instance = InstanceFactory.getInstance().getInstance(instanceId);
            Assert.assertTrue(instance != null);
            instances.add(instance);
        }

        TextView showGroupName = (TextView) findViewById(R.id.show_group_name);
        showGroupName.setText(getDisplayText(instances.get(0)));

        RecyclerView showGroupList = (RecyclerView) findViewById(R.id.show_group_list);
        showGroupList.setLayoutManager(new LinearLayoutManager(this));
        showGroupList.setAdapter(new InstanceAdapter(this, instances));
    }

    private String getDisplayText(Instance instance) {
        Assert.assertTrue(instance != null);

        Time time = getTime(instance.getDateTime());
        Assert.assertTrue(time != null);
        return new DateTime(instance.getDateTime().getDate(), time).getDisplayText(this);
    }

    private Time getTime(DateTime dateTime) {
        Assert.assertTrue(dateTime != null);

        DayOfWeek dayOfWeek = dateTime.getDate().getDayOfWeek();
        HourMinute hourMinute = dateTime.getTime().getHourMinute(dayOfWeek);
        Time time = CustomTimeFactory.getInstance().getCustomTime(dayOfWeek, hourMinute);
        if (time == null)
            time = new NormalTime(hourMinute);
        return time;
    }
}
