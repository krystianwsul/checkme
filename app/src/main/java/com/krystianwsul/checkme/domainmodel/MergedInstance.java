package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimePair;

import java.util.List;

public interface MergedInstance {
    @NonNull
    InstanceKey getInstanceKey();

    @NonNull
    DateTime getScheduleDateTime();

    @NonNull
    TaskKey getTaskKey();

    @Nullable
    ExactTimeStamp getDone();

    boolean exists();

    @NonNull
    DateTime getInstanceDateTime();

    @NonNull
    List<MergedInstance> getChildInstances(@NonNull ExactTimeStamp now);

    @NonNull
    String getName();

    boolean isRootInstance(@NonNull ExactTimeStamp now);

    @NonNull
    Task getTask();

    @NonNull
    TimePair getInstanceTimePair();

    @NonNull
    Date getInstanceDate();

    @Nullable
    String getDisplayText(@NonNull Context context, @NonNull ExactTimeStamp now);

    void setInstanceDateTime(@NonNull Date date, @NonNull TimePair timePair, @NonNull ExactTimeStamp now);

    void createInstanceHierarchy(@NonNull ExactTimeStamp now);

    void setNotificationShown(boolean notificationShown, @NonNull ExactTimeStamp now);

    void setDone(boolean done, @NonNull ExactTimeStamp now);

    void setNotified(@NonNull ExactTimeStamp now);

    boolean isVisible(@NonNull ExactTimeStamp now);

    boolean getNotified();

    int getNotificationId();

    @Nullable
    MergedInstance getParentInstance(@NonNull ExactTimeStamp now);

    @NonNull
    TimePair getScheduleTimePair();

    void setRelevant();
}
