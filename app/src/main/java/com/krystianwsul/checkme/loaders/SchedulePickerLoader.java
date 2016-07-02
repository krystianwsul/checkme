package com.krystianwsul.checkme.loaders;

import android.content.Context;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class SchedulePickerLoader extends DomainLoader<SchedulePickerLoader.Data> {
    private final Integer mRootTaskId;

    public SchedulePickerLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getSchedulePickerData(getContext(), mRootTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final RootTaskData RootTaskData;

        public Data(RootTaskData rootTaskData) {
            RootTaskData = rootTaskData;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (RootTaskData != null)
                hash += RootTaskData.hashCode();
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            if ((RootTaskData == null) != (data.RootTaskData == null))
                return false;

            if ((RootTaskData != null) && !RootTaskData.equals(data.RootTaskData))
                return false;

            return true;
        }
    }

    public static class RootTaskData {
        public final com.krystianwsul.checkme.utils.ScheduleType ScheduleType;

        public RootTaskData(com.krystianwsul.checkme.utils.ScheduleType scheduleType) {
            Assert.assertTrue(scheduleType != null);
            ScheduleType = scheduleType;
        }

        @Override
        public int hashCode() {
            return ScheduleType.hashCode();
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof RootTaskData))
                return false;

            RootTaskData rootTaskData = (RootTaskData) object;

            if (!ScheduleType.equals(rootTaskData.ScheduleType))
                return false;

            return true;
        }
    }
}
