package com.example.krystianwsul.organizatortest.domainmodel.groups;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class Group {
    private final TimeStamp mTimeStamp;

    private final ArrayList<Instance> mInstances = new ArrayList<>();

    public Group(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;
    }

    public void addInstance(Instance instance) {
        Assert.assertTrue(instance != null);
        mInstances.add(instance);
    }

    public String getNameText(Context context) {
        Assert.assertTrue(!mInstances.isEmpty());
        if (singleInstance()) {
            return getSingleSinstance().getScheduleText(context);
        } else {
            Date date = mTimeStamp.getDate();
            HourMinute hourMinute = mTimeStamp.getHourMinute();
            Time time = CustomTimeFactory.getInstance().getCustomTime(date.getDayOfWeek(), hourMinute);
            if (time == null)
                time = new NormalTime(hourMinute);
            DateTime dateTime = new DateTime(date, time);
            return dateTime.getDisplayText(context);
        }
    }

    public String getDetailsText(Context context) {
        Assert.assertTrue(!mInstances.isEmpty());
        if (singleInstance()) {
            return getSingleSinstance().getName();
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (Instance instance : mInstances)
                names.add(instance.getName());
            return TextUtils.join(", ", names);
        }
    }

    public TimeStamp getTimeStamp() {
        return mTimeStamp;
    }

    public ArrayList<Instance> getInstances() {
        return mInstances;
    }

    public boolean singleInstance() {
        Assert.assertTrue(!mInstances.isEmpty());
        return (mInstances.size() == 1);
    }

    public Instance getSingleSinstance() {
        Assert.assertTrue(mInstances.size() == 1);
        return mInstances.get(0);
    }
}