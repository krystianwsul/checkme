package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

public class DomainLoader extends AsyncTaskLoader<DomainFactory> {
    private DomainFactory mDomainFactory;
    private Observer mObserver;

    public DomainLoader(Context context) {
        super(context);
    }

    @Override
    public DomainFactory loadInBackground() {
        return DomainFactory.getDomainFactory(getContext());
    }

    @Override
    public void deliverResult(DomainFactory domainFactory) {
        if (isReset())
            return;

        mDomainFactory = domainFactory;

        if (isStarted())
            super.deliverResult(domainFactory);
    }

    @Override
    protected void onStartLoading() {
        if (mDomainFactory != null)
            deliverResult(mDomainFactory);

        if (mObserver == null) {
            mObserver = new Observer();
            DomainFactory.addDomainObserver(mObserver);
        }

        if (takeContentChanged() || mDomainFactory == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mDomainFactory != null)
            mDomainFactory = null;

        if (mObserver != null) {
            DomainFactory.removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    private class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(DomainFactory domainFactory, int dataId) {
            if (mDomainFactory == null || domainFactory != mDomainFactory)
                onContentChanged();
        }
    }
}
