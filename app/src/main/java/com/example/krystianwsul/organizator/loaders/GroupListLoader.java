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
    public GroupListLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext());
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
            Assert.assertTrue(!TextUtils.isEmpty(displayText));
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
            return (Done.hashCode() + InstanceKey.hashCode() + (HasChildren ? 1 : 0) + DisplayText.hashCode() + Name.hashCode() + InstanceTimeStamp.hashCode());
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
