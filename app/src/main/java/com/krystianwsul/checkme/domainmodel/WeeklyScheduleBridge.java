package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

public interface WeeklyScheduleBridge extends ScheduleBridge {
    int getDayOfWeek();

    @Nullable
    Integer getCustomTimeId();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
