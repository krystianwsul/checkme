package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.records.RemoteRootUserRecord;

import junit.framework.Assert;

public class RemoteRootUser {
    @NonNull
    private final RemoteRootUserRecord mRemoteRootUserRecord;

    RemoteRootUser(@NonNull RemoteRootUserRecord remoteRootUserRecord) {
        mRemoteRootUserRecord = remoteRootUserRecord;
    }

    @NonNull
    public String getId() {
        return mRemoteRootUserRecord.getId();
    }

    @NonNull
    public String getName() {
        return mRemoteRootUserRecord.getName();
    }

    @NonNull
    public String getEmail() {
        return mRemoteRootUserRecord.getEmail();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteRootUserRecord.setName(name);
    }

    void setToken(@Nullable String token) {
        mRemoteRootUserRecord.setToken(token);
    }

    @NonNull
    UserJson getUserJson() {
        return mRemoteRootUserRecord.getUserJson();
    }

    void removeFriend(@NonNull String friendId) {
        Assert.assertTrue(!TextUtils.isEmpty(friendId));

        mRemoteRootUserRecord.removeFriendOf(friendId);
    }
}
