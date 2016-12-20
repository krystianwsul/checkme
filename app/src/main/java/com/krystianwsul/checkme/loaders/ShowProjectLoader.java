package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

public class ShowProjectLoader extends DomainLoader<ShowProjectLoader.Data> {
    @Nullable
    private final String mProjectId;

    public ShowProjectLoader(@NonNull Context context, @Nullable String projectId) {
        super(context, FirebaseLevel.NEED);

        mProjectId = projectId;
    }

    @Override
    String getName() {
        return "ShowProjectLoader, projectId: " + mProjectId;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getShowProjectData(mProjectId);
    }

    public static class Data extends DomainLoader.Data {
        @Nullable
        public final String mName;

        public Data(@Nullable String name) {
            mName = name;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (!TextUtils.isEmpty(mName))
                hash += mName.hashCode();
            return hash;
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

            if (TextUtils.isEmpty(mName) != TextUtils.isEmpty(data.mName))
                return false;

            if (!TextUtils.isEmpty(mName) && !mName.equals(data.mName))
                return false;

            return true;
        }
    }
}
