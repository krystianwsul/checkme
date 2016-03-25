package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    private final Integer mTaskId;

    public TaskListLoader(Context context) {
        super(context);
        mTaskId = null;
    }

    public TaskListLoader(Context context, int taskId) {
        super(context);
        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final ArrayList<TaskData> taskDatas;

        public Data(ArrayList<TaskData> taskDatas) {
            Assert.assertTrue(taskDatas != null);
            this.taskDatas = taskDatas;
        }

        @Override
        public int hashCode() {
            return taskDatas.hashCode();
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

            return taskDatas.equals(data.taskDatas);
        }
    }

    public static class TaskData {
        public final int TaskId;
        public final String Name;
        public final String ScheduleText;
        public final boolean HasChildTasks;

        public TaskData(int taskId, String name, String scheduleText, boolean hasChildTasks) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            TaskId = taskId;
            Name = name;
            ScheduleText = scheduleText;
            HasChildTasks = hasChildTasks;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += TaskId;
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
            hashCode += (HasChildTasks ? 1 : 0);
            return hashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            return ((TaskId == taskData.TaskId) && Name.equals(taskData.Name) && ((TextUtils.isEmpty(ScheduleText) && TextUtils.isEmpty(taskData.ScheduleText)) || ((!TextUtils.isEmpty(ScheduleText) && !TextUtils.isEmpty(taskData.ScheduleText)) && ScheduleText.equals(taskData.ScheduleText))) && (HasChildTasks == taskData.HasChildTasks));
        }
    }
}
