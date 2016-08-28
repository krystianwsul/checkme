package com.krystianwsul.checkme.loaders;

import android.content.Context;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

public class WeeklyScheduleLoader extends DomainLoader<SingleScheduleLoader.Data> {
    private final Integer mRootTaskId; // possibly null

    public WeeklyScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public SingleScheduleLoader.Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getWeeklyScheduleData(mRootTaskId);
    }
}
