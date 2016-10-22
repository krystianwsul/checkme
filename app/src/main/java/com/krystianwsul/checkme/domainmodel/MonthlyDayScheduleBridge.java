package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

public interface MonthlyDayScheduleBridge extends ScheduleBridge {
    int getDayOfMonth();

    boolean getBeginningOfMonth();

    @Nullable
    Integer getCustomTimeId();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
