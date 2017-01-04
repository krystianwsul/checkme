package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EditInstancesLoader extends DomainLoader<EditInstancesLoader.Data> {
    @NonNull
    private final ArrayList<InstanceKey> mInstanceKeys;

    public EditInstancesLoader(@NonNull Context context, @NonNull ArrayList<InstanceKey> instanceKeys) {
        super(context, needsFirebase(instanceKeys));

        Assert.assertTrue(instanceKeys.size() > 1);

        mInstanceKeys = instanceKeys;
    }

    @NonNull
    private static FirebaseLevel needsFirebase(@NonNull List<InstanceKey> instanceKeys) {
        return (Stream.of(instanceKeys)
                .map(InstanceKey::getType)
                .anyMatch(type -> type == TaskKey.Type.REMOTE) ? FirebaseLevel.NEED : FirebaseLevel.NOTHING);
    }

    @Override
    String getName() {
        return "EditInstanceLoader, instanceKeys: " + mInstanceKeys;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getEditInstancesData(mInstanceKeys);
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
        @NonNull
        public final DateTime mInstanceDateTime;

        @NonNull
        public final String Name;

        public InstanceData(@NonNull DateTime instanceDateTime, @NonNull String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            mInstanceDateTime = instanceDateTime;
            Name = name;
        }

        @Override
        public int hashCode() {
            return (mInstanceDateTime.hashCode() + Name.hashCode());
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

            return (mInstanceDateTime.equals(instanceData.mInstanceDateTime) && Name.equals(instanceData.Name));
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
