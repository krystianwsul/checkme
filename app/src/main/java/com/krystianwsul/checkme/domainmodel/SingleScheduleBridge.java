package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

public interface SingleScheduleBridge extends ScheduleBridge {
    int getYear();

    int getMonth();

    int getDay();

    @Nullable
    CustomTimeKey getCustomTimeKey();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
