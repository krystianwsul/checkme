package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.json.UserWrapper;

import junit.framework.Assert;

import java.util.Map;

public class RemoteRootUserRecord extends RemoteRecord {
    private static final String USER_DATA = "userData";
    private static final String FRIEND_OF = "friendOf";

    @NonNull
    private final UserWrapper mUserWrapper;

    RemoteRootUserRecord(boolean create, @NonNull UserWrapper userWrapper) {
        super(create);

        mUserWrapper = userWrapper;
    }

    @NonNull
    public UserJson getUserJson() {
        return mUserWrapper.getUserData();
    }

    @NonNull
    @Override
    protected UserWrapper getCreateObject() {
        return mUserWrapper;
    }

    @NonNull
    public String getId() {
        return UserData.getKey(getUserJson().getEmail());
    }

    @NonNull
    @Override
    protected String getKey() {
        return getId();
    }

    @NonNull
    public String getName() {
        return getUserJson().getName();
    }

    @NonNull
    public String getEmail() {
        return getUserJson().getEmail();
    }

    @Nullable
    private String getToken() {
        return getUserJson().getToken();
    }

    public void setName(@NonNull String name) {
        if (getName().equals(name))
            return;

        getUserJson().setName(name);
        addValue(getKey() + "/" + USER_DATA + "/name", name);
    }

    public void setToken(@Nullable String token) {
        if (TextUtils.isEmpty(getToken()) && TextUtils.isEmpty(token))
            return;

        if (!TextUtils.isEmpty(getToken()) && getToken().equals(token))
            return;

        getUserJson().setToken(token);
        addValue(getKey() + "/" + USER_DATA + "/token", token);
    }

    public void removeFriendOf(@NonNull String friendId) {
        Assert.assertTrue(!TextUtils.isEmpty(friendId));

        Map<String, Boolean> friendOf = mUserWrapper.getFriendOf();
        Assert.assertTrue(friendOf != null);
        Assert.assertTrue(friendOf.containsKey(friendId));

        friendOf.remove(friendId);

        addValue(getKey() + "/" + FRIEND_OF + "/" + friendId, null);
    }
}
