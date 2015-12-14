package com.example.krystianwsul.organizatortest.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class ShowGroupActivity extends AppCompatActivity {
    private RecyclerView mShowGroupList;
    private ArrayList<Instance> mInstances;

    private static final String INTENT_KEY = "instanceData";

    public static Intent getIntent(Group group, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);

        ArrayList<Instance> instances = group.getInstances();
        ArrayList<Bundle> instanceData = new ArrayList<>();
        for (Instance instance : group.getInstances())
            instanceData.add(InstanceData.getBundle(instance.getTask(), instance.getScheduleDateTime()));

        intent.putExtra(INTENT_KEY, instanceData);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        List<Bundle> instanceData = intent.getParcelableArrayListExtra(INTENT_KEY);

        mInstances = new ArrayList<>();
        for (Parcelable parcelable : instanceData) {
            Bundle bundle = (Bundle) parcelable;
            Pair<Task, DateTime> pair = InstanceData.getData(bundle);

            Instance instance = InstanceFactory.getInstance().getInstance(pair.first, pair.second);
            Assert.assertTrue(instance != null);
            mInstances.add(instance);
        }

        Assert.assertTrue(mInstances.size() > 1);

        TextView showGroupName = (TextView) findViewById(R.id.show_group_name);
        showGroupName.setText(getDisplayText(mInstances.get(0)));

        mShowGroupList = (RecyclerView) findViewById(R.id.show_group_list);
        mShowGroupList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mShowGroupList.setAdapter(new InstanceAdapter(this, mInstances));
    }

    private String getDisplayText(Instance instance) {
        Assert.assertTrue(instance != null);

        Time time = getTime(instance.getInstanceDateTime());
        Assert.assertTrue(time != null);
        return new DateTime(instance.getScheduleDate(), time).getDisplayText(this);
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
