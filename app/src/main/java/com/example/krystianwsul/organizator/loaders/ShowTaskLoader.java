package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskLoader extends AsyncTaskLoader<ShowTaskLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final int mTaskId;

    public ShowTaskLoader(Context context, int taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        Data data = DomainFactory.getDomainFactory(getContext()).getShowTaskData(mTaskId, getContext());
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
            DomainFactory.getDomainFactory(getContext()).addDomainObserver(mObserver);
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
            DomainFactory.getDomainFactory(getContext()).removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    private class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
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
