package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.List;
import java.util.TreeMap;

public class CreateTaskLoader extends DomainLoader<CreateTaskLoader.Data> {
    private final Integer mTaskId;
    private final List<Integer> mExcludedTaskIds;

    public CreateTaskLoader(Context context, Integer taskId, List<Integer> excludedTaskIds) {
        super(context);

        Assert.assertTrue(excludedTaskIds != null);

        mTaskId = taskId;
        mExcludedTaskIds = excludedTaskIds;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mTaskId, getContext(), mExcludedTaskIds);
    }

    public static class Data extends DomainLoader.Data {
        public final TaskData TaskData;
        public final TreeMap<Integer, TaskTreeData> TaskTreeDatas;

        public Data(TaskData taskData, TreeMap<Integer, TaskTreeData> taskTreeDatas) {
            Assert.assertTrue(taskTreeDatas != null);

            TaskData = taskData;
            TaskTreeDatas = taskTreeDatas;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (TaskData != null)
                hash += TaskData.hashCode();
            hash += TaskTreeDatas.hashCode();
            return hash;
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

            if ((TaskData == null) != (data.TaskData == null))
                return false;

            if ((TaskData != null) && !TaskData.equals(data.TaskData))
                return false;

            if (!TaskTreeDatas.equals(data.TaskTreeDatas))
                return false;

            return true;
        }
    }

    public static class TaskData {
        public final String Name;
        public final Integer ParentTaskId;
        public final com.krystianwsul.checkme.utils.ScheduleType ScheduleType;

        public TaskData(String name, Integer parentTaskId, com.krystianwsul.checkme.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue((parentTaskId == null) || (scheduleType == null));

            Name = name;
            ParentTaskId = parentTaskId;
            ScheduleType = scheduleType;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            if (ParentTaskId != null) {
                Assert.assertTrue(ScheduleType == null);

                hash += ParentTaskId;
            } else {
                Assert.assertTrue(ScheduleType != null);

                hash += ScheduleType.hashCode();
            }
            return hash;
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

            if (!Name.equals(taskData.Name))
                return false;

            if ((ParentTaskId == null) != (taskData.ParentTaskId == null))
                return false;

            if ((ParentTaskId != null) && !ParentTaskId.equals(taskData.ParentTaskId))
                return false;

            if ((ScheduleType == null) != (taskData.ScheduleType == null))
                return false;

            if ((ScheduleType != null) && !ScheduleType.equals(taskData.ScheduleType))
                return false;

            return true;
        }
    }

    public static class TaskTreeData {
        public final String Name;
        public final TreeMap<Integer, TaskTreeData> TaskDatas;
        public final int TaskId;
        public final String ScheduleText;

        public TaskTreeData(String name, TreeMap<Integer, TaskTreeData> taskDatas, int taskId, String scheduleText) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(taskDatas != null);

            Name = name;
            TaskDatas = taskDatas;
            TaskId = taskId;
            ScheduleText = scheduleText;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            hash += TaskDatas.hashCode();
            hash += TaskId;
            if (!TextUtils.isEmpty(ScheduleText))
                hash += ScheduleText.hashCode();
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskTreeData))
                return false;

            TaskTreeData taskTreeData = (TaskTreeData) object;

            if (!Name.equals(taskTreeData.Name))
                return false;

            if (!TaskDatas.equals(taskTreeData.TaskDatas))
                return false;

            if (TaskId != taskTreeData.TaskId)
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(taskTreeData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(taskTreeData.ScheduleText))
                return false;

            return true;
        }
    }
}
