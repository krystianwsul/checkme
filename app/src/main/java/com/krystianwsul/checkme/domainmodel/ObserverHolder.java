package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.loaders.DomainLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ObserverHolder {
    @Nullable
    private static ObserverHolder sInstance;

    private final ArrayList<WeakReference<DomainLoader.Observer>> mObservers = new ArrayList<>();

    @NonNull
    public static synchronized ObserverHolder getObserverHolder() {
        if (sInstance == null)
            sInstance = new ObserverHolder();
        return sInstance;
    }

    public synchronized void addDomainObserver(@NonNull DomainLoader.Observer observer) {
        mObservers.add(new WeakReference<>(observer));
    }

    synchronized void clear() {
        mObservers.clear();
    }

    synchronized void notifyDomainObservers(@NonNull List<Integer> dataIds) {
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
