package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.TreeMap;

public class ProjectListLoader extends DomainLoader<ProjectListLoader.Data> {
    public ProjectListLoader(Context context) {
        super(context, FirebaseLevel.NEED);
    }

    @Override
    String getName() {
        return "ProjectListLoader";
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getProjectListData();
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final TreeMap<String, ProjectData> mProjectDatas;

        public Data(@NonNull TreeMap<String, ProjectData> projectDatas) {
            mProjectDatas = projectDatas;
        }

        @Override
        public int hashCode() {
            return mProjectDatas.hashCode();
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

            return mProjectDatas.equals(data.mProjectDatas);
        }
    }

    public static class ProjectData {
        @NonNull
        public final String mName;

        public ProjectData(@NonNull String name) {
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

            if (!(object instanceof ProjectData))
                return false;

            ProjectData data = (ProjectData) object;

            if (!mName.equals(data.mName))
                return false;

            return true;
        }
    }
}
