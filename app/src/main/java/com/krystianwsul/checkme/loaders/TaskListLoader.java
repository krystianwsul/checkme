package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.List;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    private final Integer mTaskId;

    public TaskListLoader(Context context, Integer taskId) {
        super(context);
        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext(), mTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final List<TaskData> TaskDatas;

        public Data(List<TaskData> taskDatas) {
            Assert.assertTrue(taskDatas != null);
            TaskDatas = taskDatas;
        }

        @Override
        public int hashCode() {
            return TaskDatas.hashCode();
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

            return TaskDatas.equals(data.TaskDatas);
        }
    }

    public static class TaskData {
        public final int TaskId;
        public final String Name;
        public final String ScheduleText;
        public final List<TaskData> Children;
        public final boolean IsRootTask;

        public TaskData(int taskId, String name, String scheduleText, List<TaskData> children, boolean isRootTask) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(children != null);

            TaskId = taskId;
            Name = name;
            ScheduleText = scheduleText;
            Children = children;
            IsRootTask = isRootTask;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += TaskId;
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
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

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(taskData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(taskData.ScheduleText))
                return false;

            if (!Children.equals(taskData.Children))
                return false;

            if (IsRootTask != taskData.IsRootTask)
                return false;

            return true;
        }
    }
}
