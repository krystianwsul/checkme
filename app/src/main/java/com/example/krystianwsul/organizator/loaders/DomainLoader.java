package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public abstract class DomainLoader<D, O extends DomainFactory.Observer> extends AsyncTaskLoader<D> {
    protected D mData;
    protected O mObserver;

    public DomainLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(D data) {
        Assert.assertTrue(data != null);

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
            mObserver = newObserver();
            DomainFactory.getDomainFactory(getContext()).addDomainObserver(mObserver);
        }

        if (takeContentChanged() || mData == null)
            forceLoad();
    }

    protected abstract O newObserver();

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
            //DomainFactory.getDomainFactory(getContext()).removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    public abstract static class Data {
        private static int sDataId = 1;

        public final int DataId;

        private static int getNextId() {
            return sDataId++;
        }

        public Data() {
            DataId = getNextId();
        }
    }
}
