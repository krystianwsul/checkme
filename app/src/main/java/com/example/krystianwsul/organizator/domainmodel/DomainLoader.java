package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class DomainLoader extends AsyncTaskLoader<DomainFactory> {
    private DomainFactory mDomainFactory;
    private DomainObserver mDomainObserver;

    public DomainLoader(Context context) {
        super(context);
    }

    @Override
    public DomainFactory loadInBackground() {
        return DomainFactory.getDomainFactory(getContext());
    }

    @Override
    public void deliverResult(DomainFactory domainFactory) {
        if (isReset()) {
            //release resources
            return;
        }

        mDomainFactory = domainFactory;

        if (isStarted())
            super.deliverResult(domainFactory);
    }

    @Override
    protected void onStartLoading() {
        if (mDomainFactory != null)
            deliverResult(mDomainFactory);

        if (mDomainObserver == null) {
            mDomainObserver = new DomainObserver(this);
            DomainFactory.addDomainObserver(mDomainObserver);
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

        if (mDomainFactory != null) {
            // release resources
            mDomainFactory = null;
        }

        if (mDomainObserver != null) {
            DomainFactory.removeDomainObserver(mDomainObserver);
            mDomainObserver = null;
        }
    }

    @Override
    public void onCanceled(DomainFactory domainFactory) {
        super.onCanceled(domainFactory);
        // release resources
    }
}
