package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data, TaskListLoader.Observer> {
    public TaskListLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext());
    }

    @Override
    protected TaskListLoader.Observer newObserver() {
        return new Observer();
    }

    public class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final ArrayList<RootTaskData> RootTaskDatas;

        public Data(ArrayList<RootTaskData> rootTaskDatas) {
            Assert.assertTrue(rootTaskDatas != null);
            RootTaskDatas = rootTaskDatas;
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
    }
}
