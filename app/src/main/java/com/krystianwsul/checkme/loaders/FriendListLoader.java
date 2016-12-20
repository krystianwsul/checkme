package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.Set;

public class FriendListLoader extends DomainLoader<FriendListLoader.Data> {
    public FriendListLoader(@NonNull Context context) {
        super(context, FirebaseLevel.FRIEND);
    }

    @Override
    String getName() {
        return "UserListLoader";
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getFriendListData();
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final Set<UserListData> mUserListDatas;

        public Data(@NonNull Set<UserListData> userListDatas) {
            mUserListDatas = userListDatas;
        }

        @Override
        public int hashCode() {
            return mUserListDatas.hashCode();
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
