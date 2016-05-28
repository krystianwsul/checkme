package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class ShowTaskLoader extends DomainLoader<ShowTaskLoader.Data> {
    private final int mTaskId;

    public ShowTaskLoader(Context context, int taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowTaskData(mTaskId, getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final boolean IsRootTask;
        public final String Name;
        public final String ScheduleText;
        public final int TaskId;

        public Data(boolean isRootTask, String name, String scheduleText, int taskId) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            IsRootTask = isRootTask;
            Name = name;
            ScheduleText = scheduleText;
            TaskId = taskId;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += (IsRootTask ? 1 : 0);
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
            hashCode += TaskId;return hashCode;
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

            return ((IsRootTask == data.IsRootTask) && Name.equals(data.Name) && ((TextUtils.isEmpty(ScheduleText) && TextUtils.isEmpty(data.ScheduleText)) || ((!TextUtils.isEmpty(ScheduleText) && !TextUtils.isEmpty(data.ScheduleText)) && ScheduleText.equals(data.ScheduleText))) && (TaskId == data.TaskId));
        }
    }
}
