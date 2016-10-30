package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

public interface MonthlyWeekScheduleBridge extends ScheduleBridge {
    int getDayOfMonth();

    int getDayOfWeek();

    boolean getBeginningOfMonth();

    @Nullable
    CustomTimeKey getCustomTimeKey();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
