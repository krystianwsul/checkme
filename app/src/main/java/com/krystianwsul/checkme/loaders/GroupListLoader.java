package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class GroupListLoader extends DomainLoader<GroupListLoader.Data> {
    @Nullable
    private final Integer mPosition;

    @Nullable
    private final MainActivity.TimeRange mTimeRange;

    @Nullable
    private final TimeStamp mTimeStamp;

    @Nullable
    private final InstanceKey mInstanceKey;

    @Nullable
    private final ArrayList<InstanceKey> mInstanceKeys;

    public GroupListLoader(@NonNull Context context, @Nullable TimeStamp timeStamp, @Nullable InstanceKey instanceKey, @Nullable ArrayList<InstanceKey> instanceKeys, @Nullable Integer position, @Nullable MainActivity.TimeRange timeRange) {
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
    String getName() {
        return "GroupListLoader, position: " + mPosition + ", timeRange: " + mTimeRange + ", timeStamp: " + mTimeStamp + ", instanceKey: " + mInstanceKey + ", instanceKeys: " + mInstanceKeys;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        if (mPosition != null) {
            Assert.assertTrue(mTimeRange != null);

            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return domainFactory.getGroupListData(getContext(), ExactTimeStamp.getNow(), mPosition, mTimeRange);
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return domainFactory.getGroupListData(mTimeStamp);
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            return domainFactory.getGroupListData(mInstanceKey);
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());

            return domainFactory.getGroupListData(getContext(), mInstanceKeys);
        }
    }

    public static class Data extends DomainLoader.Data {
        @Nullable
        public final DataWrapper mDataWrapper;

        public Data(@Nullable DataWrapper dataWrapper) {
            mDataWrapper = dataWrapper;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (mDataWrapper != null)
                hashCode += mDataWrapper.hashCode();
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

            if ((mDataWrapper == null) != (data.mDataWrapper == null))
                return false;

            if ((mDataWrapper != null) && !mDataWrapper.equals(data.mDataWrapper))
                return false;

            return true;
        }
    }

    public static class DataWrapper implements InstanceDataParent {
        public HashMap<InstanceKey, InstanceData> InstanceDatas;

        @NonNull
        public final List<CustomTimeData> CustomTimeDatas;

        @Nullable
        public final Boolean TaskEditable;

        @Nullable
        public final List<TaskData> TaskDatas;

        @Nullable
        public final String mNote;

        public DataWrapper(@NonNull List<CustomTimeData> customTimeDatas, @Nullable Boolean taskEditable, @Nullable List<TaskData> taskDatas, @Nullable String note) {
            CustomTimeDatas = customTimeDatas;
            TaskEditable = taskEditable;
            TaskDatas = taskDatas;
            mNote = note;
        }

        public void setInstanceDatas(@NonNull HashMap<InstanceKey, InstanceData> instanceDatas) {
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
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof DataWrapper))
                return false;

            DataWrapper dataWrapper = (DataWrapper) object;

            if (!InstanceDatas.equals(dataWrapper.InstanceDatas))
                return false;

            if (!CustomTimeDatas.equals(dataWrapper.CustomTimeDatas))
                return false;

            if ((TaskEditable == null) != (dataWrapper.TaskEditable == null))
                return false;

            if ((TaskEditable != null) && !TaskEditable.equals(dataWrapper.TaskEditable))
                return false;

            if ((TaskDatas == null) != (dataWrapper.TaskDatas == null))
                return false;

            if ((TaskDatas != null) && !TaskDatas.equals(dataWrapper.TaskDatas))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(dataWrapper.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(dataWrapper.mNote))
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
        @Nullable
        public ExactTimeStamp Done;

        @NonNull
        public final InstanceKey InstanceKey;

        @Nullable
        public final String DisplayText;

        public HashMap<InstanceKey, InstanceData> Children;

        @NonNull
        public final String Name;

        @NonNull
        public final TimeStamp InstanceTimeStamp;

        public boolean TaskCurrent;
        public final boolean IsRootInstance;

        @Nullable
        public Boolean IsRootTask;

        public boolean Exists;

        @NonNull
        public final TimePair InstanceTimePair;

        @Nullable
        public final String mNote;

        @NonNull
        public final InstanceDataParent mInstanceDataParent;

        @NonNull
        public final ExactTimeStamp mTaskStartExactTimeStamp;

        public InstanceData(@Nullable ExactTimeStamp done, @NonNull InstanceKey instanceKey, @Nullable String displayText, @NonNull String name, @NonNull TimeStamp instanceTimeStamp, boolean taskCurrent, boolean isRootInstance, @Nullable Boolean isRootTask, boolean exists, @NonNull InstanceDataParent instanceDataParent, @NonNull TimePair instanceTimePair, @Nullable String note, @NonNull ExactTimeStamp taskStartExactTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

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
            mInstanceDataParent = instanceDataParent;
            mNote = note;
            mTaskStartExactTimeStamp = taskStartExactTimeStamp;
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
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            hashCode += mTaskStartExactTimeStamp.hashCode();
            return hashCode;
        }

        public void setChildren(@NonNull HashMap<InstanceKey, InstanceData> children) {
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

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(instanceData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(instanceData.mNote))
                return false;

            if (!mTaskStartExactTimeStamp.equals(instanceData.mTaskStartExactTimeStamp))
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
        @NonNull
        public final TaskKey mTaskKey;

        @NonNull
        public final String Name;

        @NonNull
        public final List<TaskData> Children;

        @NonNull
        public final ExactTimeStamp mStartExactTimeStamp;

        public TaskData(@NonNull TaskKey taskKey, @NonNull String name, @NonNull List<TaskData> children, @NonNull ExactTimeStamp startExactTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            mTaskKey = taskKey;
            Name = name;
            Children = children;
            mStartExactTimeStamp = startExactTimeStamp;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += mTaskKey.hashCode();
            hashCode += Name.hashCode();
            hashCode += Children.hashCode();
            hashCode += mStartExactTimeStamp.hashCode();
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

            if (!mTaskKey.equals(taskData.mTaskKey))
                return false;

            if (!Name.equals(taskData.Name))
                return false;

            if (!Children.equals(taskData.Children))
                return false;

            if (!mStartExactTimeStamp.equals(taskData.mStartExactTimeStamp))
                return false;

            return true;
        }
    }
}
