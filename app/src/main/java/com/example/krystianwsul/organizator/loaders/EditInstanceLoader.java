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

public class EditInstanceLoader extends DomainLoader<EditInstanceLoader.Data> {
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

        @Override
        public int hashCode() {
            return (InstanceKey.hashCode() + InstanceDate.hashCode() + InstanceTimePair.hashCode() + Name.hashCode() + CustomTimeDatas.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            return (InstanceKey.equals(data.InstanceKey) && InstanceDate.equals(data.InstanceDate) && InstanceTimePair.equals(data.InstanceTimePair) && Name.equals(data.Name) && CustomTimeDatas.equals(data.CustomTimeDatas));
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

        @Override
        public int hashCode() {
            return (Id + Name.hashCode() + HourMinutes.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData customTimeData = (CustomTimeData) object;

            return (Id == customTimeData.Id && Name.equals(customTimeData.Name) && HourMinutes.equals(customTimeData.HourMinutes));
        }
    }
}
