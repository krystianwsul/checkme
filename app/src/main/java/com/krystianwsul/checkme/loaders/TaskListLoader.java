package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.utils.TaskKey;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    @Nullable
    private final TaskKey mTaskKey;

    public TaskListLoader(@NonNull Context context, @Nullable TaskKey taskKey) {
        super(context, needsFirebase(taskKey));

        mTaskKey = taskKey;
    }

    @NonNull
    private static FirebaseLevel needsFirebase(@Nullable TaskKey taskKey) {
        if (taskKey != null && taskKey.getType() == TaskKey.Type.REMOTE) {
            return FirebaseLevel.NEED;
        } else {
            return FirebaseLevel.WANT;
        }
    }

    @Override
    String getName() {
        return "TaskListLoader, taskKey: " + mTaskKey;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getTaskListData(getContext(), mTaskKey);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final TaskListFragment.TaskData mTaskData;

        public Data(@NonNull TaskListFragment.TaskData taskData) {
            mTaskData = taskData;
        }

        @Override
        public int hashCode() {
            return mTaskData.hashCode();
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            if (!mTaskData.equals(data.mTaskData))
                return false;

            return true;
        }
    }

}
