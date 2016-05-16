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
import java.util.TreeMap;

public class GroupListLoader extends DomainLoader<GroupListLoader.Data> {
    private final Integer mDay;
    private final TimeStamp mTimeStamp;
    private final InstanceKey mInstanceKey;
    private final ArrayList<InstanceKey> mInstanceKeys;

    public GroupListLoader(Context context, TimeStamp timeStamp, InstanceKey instanceKey, ArrayList<InstanceKey> instanceKeys, Integer day) {
        super(context);

        Assert.assertTrue((day != null ? 1 : 0) + (timeStamp != null ? 1 : 0) + (instanceKey != null ? 1 : 0) + (instanceKeys != null ? 1 : 0) == 1);

        mDay = day;
        mTimeStamp = timeStamp;
        mInstanceKey = instanceKey;
        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        if (mDay != null) {
            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext(), mDay);
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext(), mTimeStamp);
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext(), mInstanceKey);
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());

            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext(), mInstanceKeys);
        }
    }

    public static class Data extends DomainLoader.Data {
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;
        public final ArrayList<CustomTimeData> CustomTimeDatas;
        public final Boolean TaskEditable;

        public Data(HashMap<InstanceKey, InstanceData> instanceDatas, ArrayList<CustomTimeData> customTimeDatas, Boolean taskEditable) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(customTimeDatas != null);

            InstanceDatas = instanceDatas;
            CustomTimeDatas = customTimeDatas;
            TaskEditable = taskEditable;
        }

        @Override
        public int hashCode() {
            int hashCode = InstanceDatas.hashCode();
            hashCode += CustomTimeDatas.hashCode();
            if (TaskEditable != null)
                hashCode += (TaskEditable ? 2 : 1);
            return hashCode;
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

            return (InstanceDatas.equals(data.InstanceDatas) && CustomTimeDatas.equals(data.CustomTimeDatas) && (((TaskEditable == null) && (data.TaskEditable == null)) || ((TaskEditable != null) && (data.TaskEditable != null) && (TaskEditable.equals(data.TaskEditable)))));
        }
    }

    public static class InstanceData {
        public ExactTimeStamp Done;
        public final InstanceKey InstanceKey;
        public final String DisplayText;
        public final String Children;
        public final String Name;
        public final TimeStamp InstanceTimeStamp;
        public boolean TaskCurrent;
        public final boolean IsRootInstance;
        public final Boolean IsRootTask;
        public final boolean Exists;

        public InstanceData(ExactTimeStamp done, InstanceKey instanceKey, String displayText, String children, String name, TimeStamp instanceTimeStamp, boolean taskCurrent, boolean isRootInstance, Boolean isRootTask, boolean exists) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceTimeStamp != null);

            Done = done;
            InstanceKey = instanceKey;
            DisplayText = displayText;
            Children = children;
            Name = name;
            InstanceTimeStamp = instanceTimeStamp;
            TaskCurrent = taskCurrent;
            IsRootInstance = isRootInstance;
            IsRootTask = isRootTask;
            Exists = exists;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += InstanceKey.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            if (!TextUtils.isEmpty(Children))
                hashCode += Children.hashCode();
            hashCode += Name.hashCode();
            hashCode += InstanceTimeStamp.hashCode();
            hashCode += (TaskCurrent ? 1 : 0);
            hashCode += (IsRootInstance ? 1 : 0);
            if (IsRootTask != null)
                hashCode += (IsRootTask ? 2 : 1);
            hashCode += (Exists ? 1 : 0);
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof InstanceData))
                return false;

            InstanceData instanceData = (InstanceData) object;

            if ((Done == null) != (instanceData.Done == null))
                return false;

            if ((Done != null) && !Done.equals(instanceData.Done))
                return false;

            if (!InstanceKey.equals(instanceData.InstanceKey))
                return false;

            if (TextUtils.isEmpty(DisplayText) != TextUtils.isEmpty(instanceData.DisplayText))
                return false;

            if (!TextUtils.isEmpty(DisplayText) && !DisplayText.equals(instanceData.DisplayText))
                return false;

            if (TextUtils.isEmpty(Children) != TextUtils.isEmpty(instanceData.Children))
                return false;

            if (!TextUtils.isEmpty(Children) && !Children.equals(instanceData.Children))
                return false;

            if (!Name.equals(instanceData.Name))
                return false;

            if (!InstanceTimeStamp.equals(instanceData.InstanceTimeStamp))
                return false;

            if (TaskCurrent != instanceData.TaskCurrent)
                return false;

            if (IsRootInstance != instanceData.IsRootInstance)
                return false;

            if ((IsRootTask == null) != (instanceData.IsRootTask == null))
                return false;

            if ((IsRootTask != null) && !IsRootTask.equals(instanceData.IsRootTask))
                return false;

            if (Exists != instanceData.Exists)
                return false;

            return true;
        }
    }

    public static class CustomTimeData {
        public final String Name;
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(String name, TreeMap<DayOfWeek, HourMinute> hourMinutes) {
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
