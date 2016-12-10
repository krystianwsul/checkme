package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowCustomTimesLoader extends DomainLoader<ShowCustomTimesLoader.Data> {
    public ShowCustomTimesLoader(Context context) {
        super(context, FirebaseLevel.NOTHING);
    }

    @Override
    String getName() {
        return "ShowCustomTimesLoader";
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getShowCustomTimesData();
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
