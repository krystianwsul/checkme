package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowCustomTimeLoader extends DomainLoader<ShowCustomTimeLoader.Data, ShowCustomTimeLoader.Observer> {
    private final int mCustomTimeId;

    public ShowCustomTimeLoader(Context context, int customTimeId) {
        super(context);

        mCustomTimeId = customTimeId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowCustomTimeData(mCustomTimeId);
    }

    @Override
    protected ShowCustomTimeLoader.Observer newObserver() {
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
    }
}
