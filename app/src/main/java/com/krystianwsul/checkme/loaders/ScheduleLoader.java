package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScheduleLoader extends DomainLoader<ScheduleLoader.Data> {
    private final Integer mTaskId; // possibly null

    public ScheduleLoader(Context context, Integer taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getScheduleData(mTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final List<ScheduleData> ScheduleDatas;
        public final Map<Integer, CustomTimeData> CustomTimeDatas;

        public Data(List<ScheduleData> scheduleDatas, Map<Integer, CustomTimeData> customTimeDatas) {
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

    public interface ScheduleData {
        ScheduleType getScheduleType();
    }

    public static class SingleScheduleData implements ScheduleData {
        public final Date Date;
        public final TimePair TimePair;

        public SingleScheduleData(Date date, TimePair timePair) {
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

            if (!(object instanceof SingleScheduleData))
                return false;

            SingleScheduleData singleScheduleData = (SingleScheduleData) object;

            return (Date.equals(singleScheduleData.Date) && TimePair.equals(singleScheduleData.TimePair));
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.SINGLE;
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

    public static class DailyScheduleData implements ScheduleData {
        public final TimePair TimePair;

        public DailyScheduleData(TimePair timePair) {
            Assert.assertTrue(timePair != null);
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

            if (!(object instanceof DailyScheduleData))
                return false;

            DailyScheduleData dailyScheduleData = (DailyScheduleData) object;

            return TimePair.equals(dailyScheduleData.TimePair);
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.DAILY;
        }
    }

    public static class WeeklyScheduleData implements ScheduleData {
        public final DayOfWeek DayOfWeek;
        public final TimePair TimePair;

        public WeeklyScheduleData(DayOfWeek dayOfWeek, TimePair timePair) {
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

            if (!(object instanceof WeeklyScheduleData))
                return false;

            WeeklyScheduleData weeklyScheduleData = (WeeklyScheduleData) object;

            return (DayOfWeek.equals(weeklyScheduleData.DayOfWeek) && TimePair.equals(weeklyScheduleData.TimePair));
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.WEEKLY;
        }
    }
}
