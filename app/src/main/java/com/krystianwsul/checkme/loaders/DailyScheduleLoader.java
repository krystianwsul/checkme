package com.krystianwsul.checkme.loaders;

import android.content.Context;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;

public class DailyScheduleLoader extends DomainLoader<DailyScheduleLoader.Data> {
    private final Integer mRootTaskId; // possibly null

    public DailyScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getDailyScheduleData(mRootTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final List<DailyScheduleData> ScheduleDatas;
        public final Map<Integer, SingleScheduleLoader.CustomTimeData> CustomTimeDatas;

        public Data(List<DailyScheduleData> scheduleDatas, Map<Integer, SingleScheduleLoader.CustomTimeData> customTimeDatas) {
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

    public static class DailyScheduleData implements SingleScheduleLoader.ScheduleData {
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
    }
}
