package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;

public class MainLoader extends DomainLoader<MainLoader.Data> {
    public MainLoader(@NonNull Context context) {
        super(context, FirebaseLevel.WANT);
    }

    @Override
    String getName() {
        return "MainLoader";
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getMainData(getContext());
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
