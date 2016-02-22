package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateChildTaskLoader extends AsyncTaskLoader<CreateChildTaskLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final int mChildTaskId;

    public CreateChildTaskLoader(Context context, int childTaskId) {
        super(context);

        mChildTaskId = childTaskId;
    }

    @Override
    public Data loadInBackground() {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());
        Assert.assertTrue(domainFactory != null);

        Data data = domainFactory.getCreateChildTaskData(mChildTaskId);
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

        public Data(String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
        }
    }
}
