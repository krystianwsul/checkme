package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

public interface WeeklyScheduleBridge extends ScheduleBridge {
    int getDayOfWeek();

    @Nullable
    CustomTimeKey getCustomTimeKey();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
