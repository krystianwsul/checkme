package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.records.RemoteUserRecord;

import junit.framework.Assert;

public class RemoteUser {
    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteUserRecord mRemoteUserRecord;

    RemoteUser(@NonNull RemoteProject remoteProject, @NonNull RemoteUserRecord remoteUserRecord) {
        mRemoteProject = remoteProject;
        mRemoteUserRecord = remoteUserRecord;
    }

    @NonNull
    public String getId() {
        return mRemoteUserRecord.getId();
    }

    public void delete() {
        mRemoteProject.deleteUser(this);

        mRemoteUserRecord.delete();
    }

    @NonNull
    public String getName() {
        return mRemoteUserRecord.getName();
    }

    @NonNull
    public String getEmail() {
        return mRemoteUserRecord.getEmail();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteUserRecord.setName(name);
    }

    void setToken(@NonNull String token) {
        Assert.assertTrue(!TextUtils.isEmpty(token));

        mRemoteUserRecord.setToken(token);
    }
}
