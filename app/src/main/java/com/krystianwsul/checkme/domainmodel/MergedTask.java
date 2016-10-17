package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import java.util.List;

public interface MergedTask {
    boolean current(@NonNull ExactTimeStamp exactTimeStamp);

    @NonNull
    ExactTimeStamp getStartExactTimeStamp();

    @NonNull
    String getName();

    @Nullable
    String getScheduleText(@NonNull Context context, @NonNull ExactTimeStamp exactTimeStamp);

    @Nullable
    String getNote();

    @NonNull
    TaskKey getTaskKey();

    @NonNull
    List<MergedTask> getChildTasks(@NonNull ExactTimeStamp exactTimeStamp);

    boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp);

    boolean isVisible(@NonNull ExactTimeStamp now);

    @NonNull
    MergedTask getRootTask(@NonNull ExactTimeStamp exactTimeStamp);

    @NonNull
    List<? extends MergedSchedule> getCurrentSchedules(@NonNull ExactTimeStamp exactTimeStamp);

    boolean isRootTask(@NonNull ExactTimeStamp exactTimeStamp);

    void setEndExactTimeStamp(@NonNull ExactTimeStamp now);

    void createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note);
}
