package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    public TaskListLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final ArrayList<RootTaskData> RootTaskDatas;

        public Data(ArrayList<RootTaskData> rootTaskDatas) {
            Assert.assertTrue(rootTaskDatas != null);
            RootTaskDatas = rootTaskDatas;
        }

        @Override
        public int hashCode() {
            return RootTaskDatas.hashCode();
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

            return RootTaskDatas.equals(data.RootTaskDatas);
        }
    }

    public static class RootTaskData {
        public final int TaskId;
        public final String Name;
        public final String ScheduleText;
        public final boolean HasChildTasks;

        public RootTaskData(int taskId, String name, String scheduleText, boolean hasChildTasks) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(scheduleText));

            TaskId = taskId;
            Name = name;
            ScheduleText = scheduleText;
            HasChildTasks = hasChildTasks;
        }

        @Override
        public int hashCode() {
            return (TaskId + Name.hashCode() + ScheduleText.hashCode() + (HasChildTasks ? 1 : 0));
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof RootTaskData))
                return false;

            RootTaskData rootTaskData = (RootTaskData) object;

            return ((TaskId == rootTaskData.TaskId) && Name.equals(rootTaskData.Name) && ScheduleText.equals(rootTaskData.ScheduleText) && (HasChildTasks == rootTaskData.HasChildTasks));
        }
    }
}
