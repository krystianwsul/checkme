package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;
import com.krystianwsul.checkme.MyFirebaseInstanceIdService;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {
    @NonNull
    private final String mEmail;

    @NonNull
    public final String mName;

    @Nullable
    public final String mToken;

    public UserInfo(@NonNull FirebaseUser firebaseUser) {
        //noinspection ConstantConditions
        mEmail = firebaseUser.getEmail();
        Assert.assertTrue(!TextUtils.isEmpty(mEmail));

        //noinspection ConstantConditions
        mName = firebaseUser.getDisplayName();
        Assert.assertTrue(!TextUtils.isEmpty(mName));

        mToken = MyFirebaseInstanceIdService.getToken();
    }

    @NonNull
    public String getKey() {
        return UserData.getKey(mEmail);
    }

    @NonNull
    public Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();

        values.put("email", mEmail);
        values.put("name", mName);
        values.put("token", mToken);

        return values;
    }

    @Override
    public int hashCode() {
        int hash = mEmail.hashCode();
        hash += mName.hashCode();
        if (!TextUtils.isEmpty(mToken))
            hash += mToken.hashCode();
        return hash;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (!(obj instanceof UserInfo))
            return false;

        UserInfo userInfo = (UserInfo) obj;

        if (!mEmail.equals(userInfo.mEmail))
            return false;

        if (!mName.equals(userInfo.mName))
            return false;

        if (Utils.stringEquals(mToken, userInfo.mToken))
            return false;

        return true;
    }

    @NonNull
    @Exclude
    public UserJson toUserJson() {
        return new UserJson(mEmail, mName, mToken);
    }
}
