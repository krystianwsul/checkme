package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.Nullable;

public interface DailyScheduleBridge extends ScheduleBridge {
    @Nullable
    Integer getCustomTimeId();

    @Nullable
    Integer getHour();

    @Nullable
    Integer getMinute();
}
