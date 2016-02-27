package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateRootTaskLoader extends AsyncTaskLoader<CreateRootTaskLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final int mRootTaskId;

    public CreateRootTaskLoader(Context context, int rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        Data data = DomainFactory.getDomainFactory(getContext()).getCreateRootTaskData(mRootTaskId);
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
        public final String Name;
        public final com.example.krystianwsul.organizator.utils.ScheduleType ScheduleType;

        public Data(String name, com.example.krystianwsul.organizator.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleType = scheduleType;
        }
    }
}
