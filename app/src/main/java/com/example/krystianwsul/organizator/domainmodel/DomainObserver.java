package com.example.krystianwsul.organizator.domainmodel;

import junit.framework.Assert;

public class DomainObserver {
    private final DomainLoader mDomainLoader;

    public DomainObserver(DomainLoader domainLoader) {
        Assert.assertTrue(domainLoader != null);

        mDomainLoader = domainLoader;
    }

    public void onDomainChanged(DomainFactory domainFactory) {
        if (domainFactory != mDomainLoader.mDomainFactory)
            mDomainLoader.onContentChanged();
    }
}
