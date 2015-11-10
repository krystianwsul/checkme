package com.example.krystianwsul.organizatortest.domainmodel.groups;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class Group {
    private final Date mDate;
    private final HourMinute mHourMinute;

    private final ArrayList<Instance> mInstances = new ArrayList<>();

    public Group(Date date, HourMinute hourMinute) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMinute != null);

        mDate = date;
        mHourMinute = hourMinute;
    }

    public void addInstance(Instance instance) {
        Assert.assertTrue(instance != null);
        mInstances.add(instance);
    }

    public String getName(Context context) {
        Assert.assertTrue(!mInstances.isEmpty());
        if (mInstances.size() == 1) {
            return mInstances.get(0).getName();
        } else {
            return mDate.getDisplayText(context) + ", " + mHourMinute.toString();
        }
    }

    public String getScheduleText(Context context) {
        Assert.assertTrue(!mInstances.isEmpty());
        if (mInstances.size() == 1) {
            return mInstances.get(0).getScheduleText(context);
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (Instance instance : mInstances)
                names.add(instance.getName());
            return TextUtils.join("; ", names);
        }
    }

    public Date getDate() {
        return mDate;
    }

    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    public ArrayList<Instance> getInstances() {
        return mInstances;
    }
}
