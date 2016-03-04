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

            Data newData = loadInBackground();
            if (mData.equals(newData))
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final ArrayList<CustomTimeData> Entries;

        public Data(ArrayList<CustomTimeData> entries) {
            Assert.assertTrue(entries != null);
            Entries = entries;
        }

        @Override
        public int hashCode() {
            return Entries.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            return Entries.equals(data.Entries);
        }
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;

        public CustomTimeData(int id, String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Id = id;
            Name = name;
        }

        @Override
        public int hashCode() {
            return (Id + Name.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData data = (CustomTimeData) object;

            return (Id == data.Id && Name.equals(data.Name));
        }
    }
}
