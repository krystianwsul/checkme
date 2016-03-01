package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;

public class SingleScheduleLoader extends DomainLoader<SingleScheduleLoader.Data, SingleScheduleLoader.Observer> {
    private final Integer mRootTaskId; // possibly null

    public SingleScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getSingleScheduleData(mRootTaskId);
    }

    @Override
    protected SingleScheduleLoader.Observer newObserver() {
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
        public final ScheduleData ScheduleData;
        public final HashMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(ScheduleData scheduleData, HashMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            ScheduleData = scheduleData;
            CustomTimeDatas = customTimeDatas;
        }
    }

    public static class ScheduleData {
        public final Date Date;
        public final TimePair TimePair;

        public ScheduleData(Date date, TimePair timePair) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(timePair != null);

            Date = date;
            TimePair = timePair;
        }
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }
    }
}
