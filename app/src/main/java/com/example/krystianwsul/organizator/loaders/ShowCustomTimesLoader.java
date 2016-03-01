package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowCustomTimesLoader extends DomainLoader<ShowCustomTimesLoader.Data, ShowCustomTimesLoader.Observer> {
    public ShowCustomTimesLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        Data data = DomainFactory.getDomainFactory(getContext()).getShowCustomTimesData();
        Assert.assertTrue(data != null);

        return data;
    }

    @Override
    protected ShowCustomTimesLoader.Observer newObserver() {
        return new Observer();
    }

    public class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
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
