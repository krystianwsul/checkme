package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;

public interface DailyScheduleBridge extends ScheduleBridge {
    @Nullable
    CustomTimeKey getCustomTimeKey();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
