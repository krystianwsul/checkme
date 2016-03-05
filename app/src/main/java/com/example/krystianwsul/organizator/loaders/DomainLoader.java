package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public abstract class DomainLoader<D extends DomainLoader.Data> extends AsyncTaskLoader<D> {
    protected D mData;
    protected Observer mObserver;

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
            //DomainFactory.getDomainFactory(getContext()).removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    public class Observer {
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            if (mData != null) {
                D newData = loadInBackground();
                if (mData.equals(newData))
                    return;
            }

            onContentChanged();
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
