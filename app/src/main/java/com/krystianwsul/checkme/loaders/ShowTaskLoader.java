package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

public class ShowTaskLoader extends DomainLoader<ShowTaskLoader.Data> {
    private final TaskKey mTaskKey;

    public ShowTaskLoader(@NonNull Context context, @NonNull TaskKey taskKey) {
        super(context, false);

        mTaskKey = taskKey;
    }

    @Override
    String getName() {
        return "ShowTaskLoader, taskKey: " + mTaskKey;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getShowTaskData(mTaskKey, getContext());
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final String Name;

        @Nullable
        public final String ScheduleText;

        @NonNull
        public final TaskKey mTaskKey;

        public Data(@NonNull String name, @Nullable String scheduleText, @NonNull TaskKey taskKey) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleText = scheduleText;
            mTaskKey = taskKey;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
            hashCode += mTaskKey.hashCode();
            return hashCode;
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

            if (!Name.equals(data.Name))
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(data.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(data.ScheduleText))
                return false;

            if (!mTaskKey.equals(data.mTaskKey))
                return false;

            return true;
        }
    }
}
