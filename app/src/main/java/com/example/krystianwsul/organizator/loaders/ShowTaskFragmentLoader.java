package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskFragmentLoader extends DomainLoader<ShowTaskFragmentLoader.Data> {
    private final int mTaskId;

    public ShowTaskFragmentLoader(Context context, int taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowTaskFragmentData(mTaskId, getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final int TaskId;
        public final ArrayList<ChildTaskData> ChildTaskDatas;

        public Data(int taskId, ArrayList<ChildTaskData> childTaskDatas) {
            Assert.assertTrue(childTaskDatas != null);

            TaskId = taskId;
            ChildTaskDatas = childTaskDatas;
        }

        @Override
        public int hashCode() {
            return TaskId + ChildTaskDatas.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            return ((TaskId == data.TaskId) && ChildTaskDatas.equals(data.ChildTaskDatas));
        }
    }

    public static class ChildTaskData {
        public final int TaskId;
        public final String Name;
        public final boolean HasChildTasks;

        public ChildTaskData(int taskId, String name, boolean hasChildTasks) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            TaskId = taskId;
            Name = name;
            HasChildTasks = hasChildTasks;
        }

        @Override
        public int hashCode() {
            return (TaskId + Name.hashCode() + (HasChildTasks ? 1 : 0));
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof ChildTaskData))
                return false;

            ChildTaskData childTaskData = (ChildTaskData) object;

            return ((TaskId == childTaskData.TaskId) && Name.equals(childTaskData.Name) && (HasChildTasks == childTaskData.HasChildTasks));
        }
    }
}
