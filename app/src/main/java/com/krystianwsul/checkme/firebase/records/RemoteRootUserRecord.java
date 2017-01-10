package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.json.UserWrapper;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.Map;

public class RemoteRootUserRecord extends RemoteRecord {
    private static final String USER_DATA = "userData";
    private static final String FRIEND_OF = "friendOf";

    @NonNull
    private final UserWrapper mUserWrapper;

    public RemoteRootUserRecord(boolean create, @NonNull UserWrapper userWrapper) {
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

    public void setName(@NonNull String name) {
        if (getName().equals(name))
            return;

        getUserJson().setName(name);
        addValue(getKey() + "/" + USER_DATA + "/name", name);
    }

    public void setToken(@Nullable String token, @NonNull String uuid) {
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        Map<String, String> tokens = getUserJson().getTokens();

        if (Utils.stringEquals(tokens.get(uuid), token))
            return;

        getUserJson().addToken(token, uuid);
        addValue(getKey() + "/" + USER_DATA + "/tokens/" + uuid, token);
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
