package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowCustomTimesLoader extends AsyncTaskLoader<ShowCustomTimesLoader.Data> {
    private Data mData;

    private Observer mObserver;

    public ShowCustomTimesLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());
        Assert.assertTrue(domainFactory != null);

        Data data = domainFactory.getShowCustomTimesData();
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
            if (dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
        public final ArrayList<Entry> Entries;

        public Data(ArrayList<Entry> entries) {
            Assert.assertTrue(entries != null);
            Entries = entries;
        }

        public static class Entry {
            public final int Id;
            public final String Name;

            public Entry(int id, String name) {
                Assert.assertTrue(!TextUtils.isEmpty(name));

                Id = id;
                Name = name;
            }
        }
    }
}
