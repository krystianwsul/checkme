package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.records.RemoteUserRecord;

class RemoteUser {
    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteUserRecord mRemoteUserRecord;

    RemoteUser(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteUserRecord remoteUserRecord) {
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

    @NonNull
    public String getToken() {
        return mRemoteUserRecord.getToken();
    }
}
