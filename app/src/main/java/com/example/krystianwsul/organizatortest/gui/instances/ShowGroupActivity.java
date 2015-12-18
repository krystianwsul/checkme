package com.example.krystianwsul.organizatortest.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

public class ShowGroupActivity extends AppCompatActivity {
    private RecyclerView mShowGroupList;
    private TimeStamp mTimeStamp;
    private TextView mShowGroupName;

    private static final String TIME_KEY = "time";

    public static Intent getIntent(Group group, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);
        intent.putExtra(TIME_KEY, group.getTimeStamp().getLong());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TIME_KEY));
        long time = intent.getLongExtra(TIME_KEY, -1);
        Assert.assertTrue(time != -1);
        mTimeStamp = new TimeStamp(time);

        mShowGroupName = (TextView) findViewById(R.id.show_group_name);

        mShowGroupList = (RecyclerView) findViewById(R.id.show_group_list);
        mShowGroupList.setLayoutManager(new LinearLayoutManager(this));

    }

    private ArrayList<Instance> getInstances(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);

        HashSet<Instance> allInstances = new HashSet<>();
        allInstances.addAll(InstanceFactory.getInstance().getExistingInstances());

        Collection<Task> tasks = TaskFactory.getInstance().getTasks();

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        for (Task task : tasks)
            allInstances.addAll(task.getInstances(timeStamp, endTimeStamp));

        ArrayList<Instance> rootInstances = new ArrayList<>();
        for (Instance instance : allInstances)
            if (instance.isRootInstance())
                rootInstances.add(instance);

        ArrayList<Instance> currentInstances = new ArrayList<>();
        for (Instance instance : rootInstances)
            if (instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                currentInstances.add(instance);

        return currentInstances;
    }

    @Override
    protected void onStart() {
        super.onStart();

        ArrayList<Instance> instances = getInstances(mTimeStamp);
        Assert.assertTrue(!instances.isEmpty());
        if (instances.size() == 1)
            finish();

        mShowGroupName.setText(getDisplayText(instances.get(0)));

        mShowGroupList.setAdapter(new InstanceAdapter(this, instances));
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