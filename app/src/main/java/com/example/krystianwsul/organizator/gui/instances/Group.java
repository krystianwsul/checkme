package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

class Group {
    private final DomainFactory mDomainFactory;

    private final TimeStamp mTimeStamp;

    private final ArrayList<Instance> mInstances = new ArrayList<>();

    public Group(DomainFactory domainFactory, TimeStamp timeStamp) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(timeStamp != null);

        mDomainFactory = domainFactory;
        mTimeStamp = timeStamp;
    }

    public void addInstance(Instance instance) {
        Assert.assertTrue(instance != null);
        mInstances.add(instance);
    }

    public String getNameText(Context context) {
        Assert.assertTrue(!mInstances.isEmpty());
        if (singleInstance()) {
            return getSingleSinstance().getDisplayText(context);
        } else {
            Date date = mTimeStamp.getDate();
            HourMinute hourMinute = mTimeStamp.getHourMinute();
            Time time = mDomainFactory.getCustomTime(date.getDayOfWeek(), hourMinute);
            if (time == null)
                time = new NormalTime(hourMinute);
            DateTime dateTime = new DateTime(date, time);
            return dateTime.getDisplayText(context);
        }
    }

    public String getDetailsText() {
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

    public boolean singleInstance() {
        Assert.assertTrue(!mInstances.isEmpty());
        return (mInstances.size() == 1);
    }

    public Instance getSingleSinstance() {
        Assert.assertTrue(mInstances.size() == 1);
        return mInstances.get(0);
    }
}