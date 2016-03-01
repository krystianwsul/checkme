package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;

public class EditInstanceLoader extends DomainLoader<EditInstanceLoader.Data, EditInstanceLoader.Observer> {
    private final InstanceKey mInstanceKey;

    public EditInstanceLoader(Context context, InstanceKey instanceKey) {
        super(context);

        Assert.assertTrue(instanceKey != null);

        mInstanceKey = instanceKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getEditInstanceData(mInstanceKey);
    }

    @Override
    protected EditInstanceLoader.Observer newObserver() {
        return new Observer();
    }

    public class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final InstanceKey InstanceKey;
        public final Date InstanceDate;
        public final TimePair InstanceTimePair;
        public final String Name;
        public final HashMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(InstanceKey instanceKey, Date instanceDate, TimePair instanceTimePair, String name, HashMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(instanceDate != null);
            Assert.assertTrue(instanceTimePair != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(customTimeDatas != null);

            InstanceKey = instanceKey;
            InstanceDate = instanceDate;
            InstanceTimePair = instanceTimePair;
            Name = name;
            CustomTimeDatas = customTimeDatas;
        }
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }
    }
}
