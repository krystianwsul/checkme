package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class WeeklyScheduleLoader extends DomainLoader<WeeklyScheduleLoader.Data> {
    private final Integer mRootTaskId; // possibly null

    public WeeklyScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getWeeklyScheduleData(mRootTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final List<ScheduleData> ScheduleDatas;
        public final HashMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(List<ScheduleData> scheduleDatas, HashMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            ScheduleDatas = scheduleDatas;
            CustomTimeDatas = customTimeDatas;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (ScheduleDatas != null)
                hashCode += ScheduleDatas.hashCode();
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

            return (((ScheduleDatas == null) == (data.ScheduleDatas == null)) && ((ScheduleDatas == null) || ScheduleDatas.equals(data.ScheduleDatas)) && CustomTimeDatas.equals(data.CustomTimeDatas));
        }
    }

    public static class ScheduleData {
        public final DayOfWeek DayOfWeek;
        public final TimePair TimePair;

        public ScheduleData(DayOfWeek dayOfWeek, TimePair timePair) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePair != null);

            DayOfWeek = dayOfWeek;
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

            return (DayOfWeek.equals(scheduleData.DayOfWeek) && TimePair.equals(scheduleData.TimePair));
        }
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, TreeMap<DayOfWeek, HourMinute> hourMinutes) {
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

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData customTimeData = (CustomTimeData) object;

            if (Id != customTimeData.Id)
                return false;

            if (!Name.equals(customTimeData.Name))
                return false;

            return (HourMinutes.equals(customTimeData.HourMinutes));
        }
    }
}
