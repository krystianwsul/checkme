package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.ObserverHolder;

import java.util.ArrayList;

public abstract class DomainLoader<D extends DomainLoader.Data> extends AsyncTaskLoader<D> {
    private D mData;
    private Observer mObserver;

    DomainLoader(Context context) {
        super(context);
    }

    abstract String getName();


    @Override
    public final D loadInBackground() {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());

        return loadDomain(domainFactory);
    }

    protected abstract D loadDomain(@NonNull DomainFactory domainFactory);

    // main thread
    @Override
    public void deliverResult(D data) {
        if (data == null)
            return;

        if (isReset())
            return;

        if ((mData == null) || !mData.equals(data)) {
            mData = data;

            if (isStarted())
                super.deliverResult(data);
        }
    }

    // main thread
    @Override
    protected void onStartLoading() {
        if (mData != null)
            deliverResult(mData);

        if (mObserver == null) {
            mObserver = new Observer();
            ObserverHolder.getObserverHolder().addDomainObserver(mObserver);
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
        public void onDomainChanged(ArrayList<Integer> dataIds) {
            if (mData != null && dataIds.contains(mData.DataId))
                return;

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
