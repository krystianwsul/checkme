package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.TreeMap;

public class CreateChildTaskLoader extends DomainLoader<CreateChildTaskLoader.Data> {
    private final Integer mChildTaskId;

    public CreateChildTaskLoader(Context context, Integer childTaskId) {
        super(context);

        mChildTaskId = childTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mChildTaskId, getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final TreeMap<Integer, TaskData> TaskDatas;
        public final ChildTaskData ChildTaskData;

        public Data(TreeMap<Integer, TaskData> taskDatas, ChildTaskData childTaskData) {
            Assert.assertTrue(taskDatas != null);

            TaskDatas = taskDatas;
            ChildTaskData = childTaskData;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += TaskDatas.hashCode();
            if (ChildTaskData != null)
                hash += ChildTaskData.hashCode();
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

            if (!TaskDatas.equals(data.TaskDatas))
                return false;

            if ((ChildTaskData == null) != (data.ChildTaskData == null))
                return false;

            if ((ChildTaskData != null) && !ChildTaskData.equals(data.ChildTaskData))
                return false;

            return true;
        }
    }

    public static class TaskData {
        public final String Name;
        public final TreeMap<Integer, TaskData> TaskDatas;
        public final int TaskId;
        public final String ScheduleText;

        public TaskData(String name, TreeMap<Integer, TaskData> taskDatas, int taskId, String scheduleText) {
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

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            if (!Name.equals(taskData.Name))
                return false;

            if (!TaskDatas.equals(taskData.TaskDatas))
                return false;

            if (TaskId != taskData.TaskId)
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(taskData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(taskData.ScheduleText))
                return false;

            return true;
        }
    }

    public static class ChildTaskData {
        public final String Name;
        public final int ParentTaskId;

        public ChildTaskData(String name, int parentTaskId) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ParentTaskId = parentTaskId;
        }

        @Override
        public int hashCode() {
            return Name.hashCode() + ParentTaskId;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof ChildTaskData))
                return false;

            ChildTaskData childTaskData = (ChildTaskData) object;

            if (!Name.equals(childTaskData.Name))
                return false;

            if (ParentTaskId != childTaskData.ParentTaskId)
                return false;

            return true;
        }
    }
}
