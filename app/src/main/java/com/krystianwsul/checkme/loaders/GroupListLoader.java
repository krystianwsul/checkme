package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class GroupListLoader extends DomainLoader<GroupListLoader.Data> {
    private final Integer mPosition;
    private final MainActivity.TimeRange mTimeRange;
    private final TimeStamp mTimeStamp;
    private final InstanceKey mInstanceKey;
    private final ArrayList<InstanceKey> mInstanceKeys;

    public GroupListLoader(Context context, TimeStamp timeStamp, InstanceKey instanceKey, ArrayList<InstanceKey> instanceKeys, Integer position, MainActivity.TimeRange timeRange) {
        super(context);

        Assert.assertTrue((position == null) == (timeRange == null));
        Assert.assertTrue((position != null ? 1 : 0) + (timeStamp != null ? 1 : 0) + (instanceKey != null ? 1 : 0) + (instanceKeys != null ? 1 : 0) == 1);

        mPosition = position;
        mTimeRange = timeRange;
        mTimeStamp = timeStamp;
        mInstanceKey = instanceKey;
        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        if (mPosition != null) {
            Assert.assertTrue(mTimeRange != null);

            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext(), mPosition, mTimeRange);
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

    public static class Data extends DomainLoader.Data implements InstanceDataParent {
        public HashMap<InstanceKey, InstanceData> InstanceDatas;
        public final List<CustomTimeData> CustomTimeDatas;
        public final Boolean TaskEditable;
        public List<TaskData> TaskDatas;

        public Data(List<CustomTimeData> customTimeDatas, Boolean taskEditable, List<TaskData> taskDatas) {
            Assert.assertTrue(customTimeDatas != null);

            CustomTimeDatas = customTimeDatas;
            TaskEditable = taskEditable;
            TaskDatas = taskDatas;
        }

        public void setInstanceDatas(HashMap<InstanceKey, InstanceData> instanceDatas) {
            Assert.assertTrue(instanceDatas != null);

            InstanceDatas = instanceDatas;
        }

        @Override
        public int hashCode() {
            int hashCode = InstanceDatas.hashCode();
            hashCode += CustomTimeDatas.hashCode();
            if (TaskEditable != null)
                hashCode += (TaskEditable ? 2 : 1);
            if (TaskDatas != null)
                hashCode += TaskDatas.hashCode();
            return hashCode;
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

            if ((TaskEditable == null) != (data.TaskEditable == null))
                return false;

            if ((TaskEditable != null) && !TaskEditable.equals(data.TaskEditable))
                return false;

            if ((TaskDatas == null) != (data.TaskDatas == null))
                return false;

            if ((TaskDatas != null) && !TaskDatas.equals(data.TaskDatas))
                return false;

            return true;
        }

        @Override
        public void remove(InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(InstanceDatas.containsKey(instanceKey));

            InstanceDatas.remove(instanceKey);
        }
    }

    public static class InstanceData implements InstanceDataParent {
        public ExactTimeStamp Done;
        public final InstanceKey InstanceKey;
        public final String DisplayText;
        public HashMap<InstanceKey, InstanceData> Children;
        public final String Name;
        public final TimeStamp InstanceTimeStamp;
        public boolean TaskCurrent;
        public final boolean IsRootInstance;
        public Boolean IsRootTask;
        public boolean Exists;
        public final TimePair InstanceTimePair;

        public final WeakReference<InstanceDataParent> InstanceDataParentReference;

        public InstanceData(ExactTimeStamp done, InstanceKey instanceKey, String displayText, String name, TimeStamp instanceTimeStamp, boolean taskCurrent, boolean isRootInstance, Boolean isRootTask, boolean exists, WeakReference<InstanceDataParent> instanceDataParentReference, TimePair instanceTimePair) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceTimeStamp != null);
            Assert.assertTrue(instanceDataParentReference != null);
            Assert.assertTrue(instanceTimePair != null);

            Done = done;
            InstanceKey = instanceKey;
            DisplayText = displayText;
            Name = name;
            InstanceTimeStamp = instanceTimeStamp;
            TaskCurrent = taskCurrent;
            IsRootInstance = isRootInstance;
            IsRootTask = isRootTask;
            Exists = exists;
            InstanceTimePair = instanceTimePair;

            InstanceDataParentReference = instanceDataParentReference;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += InstanceKey.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += Children.hashCode();
            hashCode += Name.hashCode();
            hashCode += InstanceTimeStamp.hashCode();
            hashCode += (TaskCurrent ? 1 : 0);
            hashCode += (IsRootInstance ? 1 : 0);
            if (IsRootTask != null)
                hashCode += (IsRootTask ? 2 : 1);
            hashCode += (Exists ? 1 : 0);
            hashCode += InstanceTimePair.hashCode();
            return hashCode;
        }

        public void setChildren(HashMap<InstanceKey, InstanceData> children) {
            Assert.assertTrue(children != null);

            Children = children;
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

            if (!Children.equals(instanceData.Children))
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

            if (!InstanceTimePair.equals(instanceData.InstanceTimePair))
                return false;

            return true;
        }

        @Override
        public void remove(InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(Children.containsKey(instanceKey));

            Children.remove(instanceKey);
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

    public interface InstanceDataParent {
        void remove(InstanceKey instanceKey);
    }

    public static class TaskData {
        public final int TaskId;
        public final String Name;
        public final List<TaskData> Children;
        public final boolean IsRootTask;

        public TaskData(int taskId, String name, List<TaskData> children, boolean isRootTask) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(children != null);

            TaskId = taskId;
            Name = name;
            Children = children;
            IsRootTask = isRootTask;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += TaskId;
            hashCode += Name.hashCode();
            hashCode += Children.hashCode();
            hashCode += (IsRootTask ? 1 : 0);
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            if (TaskId != taskData.TaskId)
                return false;

            if (!Name.equals(taskData.Name))
                return false;

            if (!Children.equals(taskData.Children))
                return false;

            if (IsRootTask != taskData.IsRootTask)
                return false;

            return true;
        }
    }
}
