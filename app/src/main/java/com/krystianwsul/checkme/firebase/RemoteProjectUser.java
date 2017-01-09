package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.records.RemoteProjectUserRecord;

import junit.framework.Assert;

public class RemoteProjectUser {
    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteProjectUserRecord mRemoteProjectUserRecord;

    RemoteProjectUser(@NonNull RemoteProject remoteProject, @NonNull RemoteProjectUserRecord remoteProjectUserRecord) {
        mRemoteProject = remoteProject;
        mRemoteProjectUserRecord = remoteProjectUserRecord;
    }

    @NonNull
    public String getId() {
        return mRemoteProjectUserRecord.getId();
    }

    public void delete() {
        mRemoteProject.deleteUser(this);

        mRemoteProjectUserRecord.delete();
    }

    @NonNull
    public String getName() {
        return mRemoteProjectUserRecord.getName();
    }

    @NonNull
    public String getEmail() {
        return mRemoteProjectUserRecord.getEmail();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteProjectUserRecord.setName(name);
    }

    void setToken(@Nullable String token, @NonNull String uuid) {
        mRemoteProjectUserRecord.setToken(token, uuid);
    }
}
