package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public interface MergedSchedule {
    boolean isVisible(@NonNull MergedTask task, @NonNull ExactTimeStamp now);
}
