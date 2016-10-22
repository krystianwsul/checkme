package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

public interface MonthlyWeekScheduleBridge extends ScheduleBridge {
    int getDayOfMonth();

    int getDayOfWeek();

    boolean getBeginningOfMonth();

    @Nullable
    Integer getCustomTimeId();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
