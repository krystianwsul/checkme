package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeLoader extends DomainLoader<ShowCustomTimeLoader.Data> {
    private final int mCustomTimeId;

    public ShowCustomTimeLoader(Context context, int customTimeId) {
        super(context);

        mCustomTimeId = customTimeId;
    }

    @Override
    String getName() {
        return "ShowCustomTimeLoader, customTimeId: " + mCustomTimeId;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getShowCustomTimeData(mCustomTimeId);
    }

    public static class Data extends DomainLoader.Data {
        public final int Id;
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public Data(int id, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(!hourMinutes.isEmpty());

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (Id + Name.hashCode() + HourMinutes.hashCode());
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

            return (Id == data.Id && Name.equals(data.Name) && HourMinutes.equals(data.HourMinutes));
        }
    }
}
