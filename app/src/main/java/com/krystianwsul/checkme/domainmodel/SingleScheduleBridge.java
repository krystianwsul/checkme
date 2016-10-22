package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

public interface SingleScheduleBridge extends ScheduleBridge {
    int getYear();

    int getMonth();

    int getDay();

    @Nullable
    Integer getCustomTimeId();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
