package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupListLoader extends DomainLoader<GroupListLoader.Data> {
    private final TimeStamp mTimeStamp;
    private final InstanceKey mInstanceKey;
    private final ArrayList<InstanceKey> mInstanceKeys;

    public GroupListLoader(Context context) {
        super(context);

        mTimeStamp = null;
        mInstanceKey = null;
        mInstanceKeys = null;
    }

    public GroupListLoader(Context context, TimeStamp timeStamp, InstanceKey instanceKey, ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue((timeStamp != null ? 1 : 0) + (instanceKey != null ? 1 : 0) + (instanceKeys != null ? 1 : 0) == 1);

        mTimeStamp = timeStamp;
        mInstanceKey = instanceKey;
        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mTimeStamp);
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mInstanceKey);
        } else if (mInstanceKeys != null) {
            Assert.assertTrue(!mInstanceKeys.isEmpty());

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mInstanceKeys);
        } else {
            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext());
        }
    }

    public static class Data extends DomainLoader.Data {
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;
        public final ArrayList<CustomTimeData> CustomTimeDatas;

        public Data(HashMap<InstanceKey, InstanceData> instanceDatas, ArrayList<CustomTimeData> customTimeDatas) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(customTimeDatas != null);

            InstanceDatas = instanceDatas;
            CustomTimeDatas = customTimeDatas;
        }

        @Override
        public int hashCode() {
            return (InstanceDatas.hashCode() + CustomTimeDatas.hashCode());
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

            return (InstanceDatas.equals(data.InstanceDatas) && CustomTimeDatas.equals(data.CustomTimeDatas));
        }
    }

    public static class InstanceData {
        public ExactTimeStamp Done;
        public final InstanceKey InstanceKey;
        public final boolean HasChildren;
        public final String DisplayText;
        public final String Name;
        public final TimeStamp InstanceTimeStamp;

        public InstanceData(ExactTimeStamp done, boolean hasChildren, InstanceKey instanceKey, String displayText, String name, TimeStamp instanceTimeStamp) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceTimeStamp != null);

            Done = done;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
            DisplayText = displayText;
            Name = name;
            InstanceTimeStamp = instanceTimeStamp;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += (HasChildren ? 1 : 0);
            hashCode += InstanceKey.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += Name.hashCode();
            hashCode += InstanceTimeStamp.hashCode();
            return hashCode;
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

            return ((((Done == null) && (instanceData.Done == null)) || ((Done != null) && (instanceData.Done != null) && Done.equals(instanceData.Done))) && InstanceKey.equals(instanceData.InstanceKey) && (HasChildren == instanceData.HasChildren) && DisplayText.equals(instanceData.DisplayText) && Name.equals(instanceData.Name) && InstanceTimeStamp.equals(instanceData.InstanceTimeStamp));
        }
    }

    public static class CustomTimeData {
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (Name.hashCode() + HourMinutes.hashCode());
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

            return (Name.equals(customTimeData.Name) && HourMinutes.equals(customTimeData.HourMinutes));
        }
    }
}
