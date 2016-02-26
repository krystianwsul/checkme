package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListLoader extends AsyncTaskLoader<TaskListLoader.Data> {
    private Data mData;

    private Observer mObserver;

    public TaskListLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());
        Assert.assertTrue(domainFactory != null);

        Data data = domainFactory.getTaskListData(getContext());
        Assert.assertTrue(data != null);

        return data;
    }

    @Override
    public void deliverResult(Data data) {
        if (isReset())
            return;

        mData = data;

        if (isStarted())
            super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null)
            deliverResult(mData);

        if (mObserver == null) {
            mObserver = new Observer();
            DomainFactory.addDomainObserver(mObserver);
        }

        if (takeContentChanged() || mData == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mData != null)
            mData = null;

        if (mObserver != null) {
            DomainFactory.removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    private class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(DomainFactory domainFactory, int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
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
