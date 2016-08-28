package com.krystianwsul.checkme.loaders;

import android.content.Context;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

public class DailyScheduleLoader extends DomainLoader<SingleScheduleLoader.Data> {
    private final Integer mRootTaskId; // possibly null

    public DailyScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public SingleScheduleLoader.Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getDailyScheduleData(mRootTaskId);
    }
}
