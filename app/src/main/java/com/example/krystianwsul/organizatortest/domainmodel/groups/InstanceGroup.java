package com.example.krystianwsul.organizatortest.domainmodel.groups;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class InstanceGroup implements Group {
    private final Instance mInstance;

    public InstanceGroup(Instance instance) {
        Assert.assertTrue(instance != null);
        mInstance = instance;
    }

    public String getName() {
        return mInstance.getName();
    }

    public ArrayList<Group> getChildGroups() {
        ArrayList<Group> childGroups = new ArrayList<>();
        for (Instance childInstance : mInstance.getChildInstances())
            childGroups.add(new InstanceGroup(childInstance));
        return childGroups;
    }

    public String getScheduleText(Context context) {
        return mInstance.getScheduleText(context);
    }

    public DateTime getDateTime() {
        return mInstance.getDateTime();
    }

    public String getIntentKey() {
        return mInstance.getIntentKey();
    }

    public int getIntentValue() {
        return mInstance.getIntentValue();
    }
}
