package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.Map;
import java.util.Set;

public class UserListLoader extends DomainLoader<UserListLoader.Data> {
    @Nullable
    private final String mProjectId;

    public UserListLoader(@NonNull Context context, @Nullable String projectId) {
        super(context, FirebaseLevel.FRIEND);

        mProjectId = projectId;
    }

    @Override
    String getName() {
        return "UserListLoader";
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getUserListData(mProjectId);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final Set<UserListData> mUserListDatas;

        @NonNull
        public final Map<String, UserListData> mFriendDatas;

        public Data(@NonNull Set<UserListData> userListDatas, @NonNull Map<String, UserListData> friendDatas) {
            mUserListDatas = userListDatas;
            mFriendDatas = friendDatas;
        }

        @Override
        public int hashCode() {
            int hash = mUserListDatas.hashCode();
            hash += mFriendDatas.hashCode();
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

            if (!mUserListDatas.equals(data.mUserListDatas))
                return false;

            if (!mFriendDatas.equals(data.mFriendDatas))
                return false;

            return true;
        }
    }

    public static class UserListData {
        @NonNull
        public final String mName;

        @NonNull
        public final String mEmail;

        @NonNull
        public final String mId;

        public UserListData(@NonNull String name, @NonNull String email, @NonNull String key) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(email));
            Assert.assertTrue(!TextUtils.isEmpty(key));

            mName = name;
            mEmail = email;
            mId = key;
        }

        @Override
        public int hashCode() {
            return (mName.hashCode() + mEmail.hashCode() + mId.hashCode());
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (!(obj instanceof UserListData))
                return false;

            UserListData userListData = (UserListData) obj;

            if (!mName.equals(userListData.mName))
                return false;

            if (!mEmail.equals(userListData.mEmail))
                return false;

            if (!mId.equals(userListData.mId))
                return false;

            return true;
        }
    }
}
