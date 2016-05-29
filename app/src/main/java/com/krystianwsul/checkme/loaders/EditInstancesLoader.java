package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class EditInstancesLoader extends DomainLoader<EditInstancesLoader.Data> {
    private final ArrayList<InstanceKey> mInstanceKeys;

    public EditInstancesLoader(Context context, ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(instanceKeys.size() > 1);

        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getEditInstancesData(mInstanceKeys);
    }

    public static class Data extends DomainLoader.Data {
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;
        public final TreeMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(HashMap<InstanceKey, InstanceData> instanceDatas, TreeMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(instanceDatas.size() > 1);
            Assert.assertTrue(customTimeDatas != null);

            InstanceDatas = instanceDatas;
            CustomTimeDatas = customTimeDatas;
        }

        @Override
        public int hashCode() {
            return (InstanceDatas.hashCode() + CustomTimeDatas.hashCode());
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            if (!InstanceDatas.equals(data.InstanceDatas))
                return false;

            if (!CustomTimeDatas.equals(data.CustomTimeDatas))
                return false;

            return true;
        }
    }

    public static class InstanceData extends DomainLoader.Data {
        public final Date InstanceDate;
        public final String Name;

        public InstanceData(Date instanceDate, String name) {
            Assert.assertTrue(instanceDate != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceDate = instanceDate;
            Name = name;
        }

        @Override
        public int hashCode() {
            return (InstanceDate.hashCode() + Name.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof InstanceData))
                return false;

            InstanceData instanceData = (InstanceData) object;

            return (InstanceDate.equals(instanceData.InstanceDate) && Name.equals(instanceData.Name));
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
