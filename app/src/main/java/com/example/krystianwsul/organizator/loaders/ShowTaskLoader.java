package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskLoader extends DomainLoader<ShowTaskLoader.Data, ShowTaskLoader.Observer> {
    private final int mTaskId;

    public ShowTaskLoader(Context context, int taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowTaskData(mTaskId, getContext());
    }

    @Override
    protected ShowTaskLoader.Observer newObserver() {
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
        public final boolean IsRootTask;
        public final String Name;
        public final String ScheduleText;
        public final int TaskId;
        public final ArrayList<ChildTaskData> ChildTaskDatas;

        public Data(boolean isRootTask, String name, String scheduleText, int taskId, ArrayList<ChildTaskData> childTaskDatas) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(childTaskDatas != null);

            IsRootTask = isRootTask;
            Name = name;
            ScheduleText = scheduleText;
            TaskId = taskId;
            ChildTaskDatas = childTaskDatas;
        }
    }

    public static class ChildTaskData {
        public final int TaskId;
        public final String Name;
        public final boolean HasChildTasks;

        public ChildTaskData(int taskId, String name, boolean hasChildTasks) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            TaskId = taskId;
            Name = name;
            HasChildTasks = hasChildTasks;
        }
    }
}
