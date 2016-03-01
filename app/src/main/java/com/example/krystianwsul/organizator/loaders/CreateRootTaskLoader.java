package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateRootTaskLoader extends DomainLoader<CreateRootTaskLoader.Data, CreateRootTaskLoader.Observer> {
    private final int mRootTaskId;

    public CreateRootTaskLoader(Context context, int rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateRootTaskData(mRootTaskId);
    }

    @Override
    protected CreateRootTaskLoader.Observer newObserver() {
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
        public final String Name;
        public final com.example.krystianwsul.organizator.utils.ScheduleType ScheduleType;

        public Data(String name, com.example.krystianwsul.organizator.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleType = scheduleType;
        }
    }
}
