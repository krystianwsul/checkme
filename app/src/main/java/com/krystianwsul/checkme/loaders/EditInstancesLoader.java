package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class EditInstancesLoader extends DomainLoader<EditInstancesLoader.Data> {
    @NonNull
    private final ArrayList<InstanceKey> mInstanceKeys;

    public EditInstancesLoader(@NonNull Context context, @NonNull ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue(instanceKeys.size() > 1);

        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getEditInstancesData(mInstanceKeys);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;

        @NonNull
        public final Map<CustomTimeKey, CustomTimeData> CustomTimeDatas;

        public Data(@NonNull HashMap<InstanceKey, InstanceData> instanceDatas, @NonNull Map<CustomTimeKey, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(instanceDatas.size() > 1);

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
        @NonNull
        public final CustomTimeKey mCustomTimeKey;

        @NonNull
        public final String Name;

        @NonNull
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(@NonNull CustomTimeKey customTimeKey, @NonNull String name, @NonNull TreeMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes.size() == 7);

            mCustomTimeKey = customTimeKey;
            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (mCustomTimeKey.hashCode() + Name.hashCode() + HourMinutes.hashCode());
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

            return (mCustomTimeKey.equals(customTimeData.mCustomTimeKey) && Name.equals(customTimeData.Name) && HourMinutes.equals(customTimeData.HourMinutes));
        }
    }
}
