package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateRootTaskLoader extends DomainLoader<CreateRootTaskLoader.Data> {
    private final int mRootTaskId;

    public CreateRootTaskLoader(Context context, int rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateRootTaskData(mRootTaskId);
    }

    public static class Data extends DomainLoader.Data {
        public final String Name;
        public final com.krystianwsul.checkme.utils.ScheduleType ScheduleType;

        public Data(String name, com.krystianwsul.checkme.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleType = scheduleType;
        }

        @Override
        public int hashCode() {
            return (Name.hashCode() + ScheduleType.hashCode());
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

            return (Name.equals(data.Name) && ScheduleType.equals(data.ScheduleType));
        }
    }
}
