package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.TreeMap;

public class EditInstanceLoader extends DomainLoader<EditInstanceLoader.Data> {
    @NonNull
    private final InstanceKey mInstanceKey;

    public EditInstanceLoader(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        super(context);

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
        public final TreeMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(InstanceKey instanceKey, Date instanceDate, TimePair instanceTimePair, String name, TreeMap<Integer, CustomTimeData> customTimeDatas) {
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
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, TreeMap<DayOfWeek, HourMinute> hourMinutes) {
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
