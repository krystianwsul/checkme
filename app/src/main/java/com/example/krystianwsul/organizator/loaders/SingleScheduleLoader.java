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

            Data newData = loadInBackground();
            if (mData.equals(newData))
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

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (ScheduleData != null)
                hashCode += ScheduleData.hashCode();
            hashCode += CustomTimeDatas.hashCode();
            return hashCode;
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

            return (((ScheduleData == null) == (data.ScheduleData == null)) && ((ScheduleData == null) || ScheduleData.equals(data.ScheduleData)) && CustomTimeDatas.equals(data.CustomTimeDatas));
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

        @Override
        public int hashCode() {
            return TimePair.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof ScheduleData))
                return false;

            ScheduleData scheduleData = (ScheduleData) object;

            return (Date.equals(scheduleData.Date) && TimePair.equals(scheduleData.TimePair));
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

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData customTimeData = (CustomTimeData) object;

            return (Id == customTimeData.Id && Name.equals(customTimeData.Name) && HourMinutes.equals(customTimeData.HourMinutes));
        }
    }
}
