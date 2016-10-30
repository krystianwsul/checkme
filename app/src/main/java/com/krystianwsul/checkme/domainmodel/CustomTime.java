package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.Time;

import java.util.Set;
import java.util.TreeMap;

public interface CustomTime extends Time {
    @NonNull
    String getName();

    @NonNull
    TreeMap<DayOfWeek, HourMinute> getHourMinutes();

    @NonNull
    CustomTimeKey getCustomTimeKey();

    void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends);
}
