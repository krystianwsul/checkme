package com.krystianwsul.checkme.domainmodel;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.loaders.DomainLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ObserverHolder {
    private static ObserverHolder sInstance;

    private final ArrayList<WeakReference<DomainLoader.Observer>> mObservers = new ArrayList<>();

    public static synchronized ObserverHolder getObserverHolder() {
        if (sInstance == null)
            sInstance = new ObserverHolder();
        return sInstance;
    }

    public synchronized void addDomainObserver(DomainLoader.Observer observer) {
        Assert.assertTrue(observer != null);
        mObservers.add(new WeakReference<>(observer));
    }

    public synchronized void clear() {
        mObservers.clear();
    }

    public synchronized void notifyDomainObservers(ArrayList<Integer> dataIds) {
        Assert.assertTrue(dataIds != null);

        ArrayList<WeakReference<DomainLoader.Observer>> remove = new ArrayList<>();

        for (WeakReference<DomainLoader.Observer> reference : mObservers) {
            Assert.assertTrue(reference != null);

            DomainLoader.Observer observer = reference.get();
            if (observer == null)
                remove.add(reference);
            else
                observer.onDomainChanged(dataIds);
        }

        Stream.of(remove)
                .forEach(mObservers::remove);
    }
}
