package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class ShowProjectLoader extends DomainLoader<ShowProjectLoader.Data> {
    @NonNull
    private final String mProjectId;

    public ShowProjectLoader(@NonNull Context context, @NonNull String projectId) {
        super(context, FirebaseLevel.NEED);

        Assert.assertTrue(!TextUtils.isEmpty(projectId));
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
        @NonNull
        public final String mName;

        public Data(@NonNull String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            mName = name;
        }

        @Override
        public int hashCode() {
            return mName.hashCode();
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

            if (!mName.equals(data.mName))
                return false;

            return true;
        }
    }
}
