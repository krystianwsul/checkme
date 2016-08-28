package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.List;

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
        public final TimePair TimePair;

        public ScheduleData(TimePair timePair) {
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

            if (!(object instanceof ScheduleData))
                return false;

            ScheduleData scheduleData = (ScheduleData) object;

            return TimePair.equals(scheduleData.TimePair);
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

            CustomTimeData customTimeData = (CustomTimeData) object;

            return (Id == customTimeData.Id && Name.equals(customTimeData.Name));
        }
    }
}
