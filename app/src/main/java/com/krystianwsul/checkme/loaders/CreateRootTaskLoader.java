package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.TreeMap;

public class CreateRootTaskLoader extends DomainLoader<CreateRootTaskLoader.Data> {
    private final Integer mRootTaskId;

    public CreateRootTaskLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateRootTaskData(getContext(), mRootTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final TreeMap<Integer, CreateChildTaskLoader.TaskData> TaskDatas;
        public final RootTaskData RootTaskData;

        public Data(TreeMap<Integer, CreateChildTaskLoader.TaskData> taskDatas, RootTaskData rootTaskData) {
            Assert.assertTrue(taskDatas != null);

            TaskDatas = taskDatas;
            RootTaskData = rootTaskData;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += TaskDatas.hashCode();
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

            if (!TaskDatas.equals(data.TaskDatas))
                return false;

            if ((RootTaskData == null) != (data.RootTaskData == null))
                return false;

            if ((RootTaskData != null) && !RootTaskData.equals(data.RootTaskData))
                return false;

            return true;
        }
    }

    public static class RootTaskData {
        public final String Name;
        public final com.krystianwsul.checkme.utils.ScheduleType ScheduleType;

        public RootTaskData(String name, com.krystianwsul.checkme.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleType = scheduleType;
        }

        @Override
        public int hashCode() {
            return (Name.hashCode() + ScheduleType.hashCode());
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

            if (!Name.equals(rootTaskData.Name))
                return false;

            if (!ScheduleType.equals(rootTaskData.ScheduleType))
                return false;

            return true;
        }
    }
}
