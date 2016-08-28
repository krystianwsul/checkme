package com.krystianwsul.checkme.loaders;

import android.content.Context;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;

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
        public final List<WeeklyScheduleData> ScheduleDatas;
        public final Map<Integer, SingleScheduleLoader.CustomTimeData> CustomTimeDatas;

        public Data(List<WeeklyScheduleData> scheduleDatas, Map<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas) {
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

    public static class WeeklyScheduleData implements SingleScheduleLoader.ScheduleData {
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
    }
}
