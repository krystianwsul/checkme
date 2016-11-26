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
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.Map;
import java.util.TreeMap;

public class EditInstanceLoader extends DomainLoader<EditInstanceLoader.Data> {
    @NonNull
    private final InstanceKey mInstanceKey;

    public EditInstanceLoader(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        super(context);

        mInstanceKey = instanceKey;
    }

    @Override
    String getName() {
        return "EditInstanceLoader, instanceKey: " + mInstanceKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getEditInstanceData(mInstanceKey);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final InstanceKey InstanceKey;

        @NonNull
        public final Date InstanceDate;

        @NonNull
        public final TimePair InstanceTimePair;

        @NonNull
        public final String Name;

        @NonNull
        public final Map<CustomTimeKey, CustomTimeData> CustomTimeDatas;

        public final boolean mDone;

        public Data(@NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair, @NonNull String name, @NonNull Map<CustomTimeKey, CustomTimeData> customTimeDatas, boolean done) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            InstanceDate = instanceDate;
            InstanceTimePair = instanceTimePair;
            Name = name;
            CustomTimeDatas = customTimeDatas;
            mDone = done;
        }

        @Override
        public int hashCode() {
            return (InstanceKey.hashCode() + InstanceDate.hashCode() + InstanceTimePair.hashCode() + Name.hashCode() + CustomTimeDatas.hashCode() + (mDone ? 1 : 0));
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

            if (!InstanceKey.equals(data.InstanceKey))
                return false;

            if (!InstanceDate.equals(data.InstanceDate))
                return false;

            if (!InstanceTimePair.equals(data.InstanceTimePair))
                return false;

            if (!Name.equals(data.Name))
                return false;

            if (!CustomTimeDatas.equals(data.CustomTimeDatas))
                return false;

            if (mDone != data.mDone)
                return false;

            return true;
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
