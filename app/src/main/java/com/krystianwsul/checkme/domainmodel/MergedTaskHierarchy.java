package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public interface MergedTaskHierarchy {
    boolean current(@NonNull ExactTimeStamp exactTimeStamp);

    @NonNull
    TaskKey getParentTaskKey();

    @NonNull
    TaskKey getChildTaskKey();

    @NonNull
    MergedTask getParentTask();

    @NonNull
    MergedTask getChildTask();

    boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp);
}
