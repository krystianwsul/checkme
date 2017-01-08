package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;

public class RemoteProjectUserRecord extends RemoteRecord {
    private static final String USERS = "users";

    @NonNull
    private final RemoteProjectRecord mRemoteProjectRecord;

    @NonNull
    private final UserJson mUserJson;

    RemoteProjectUserRecord(boolean create, @NonNull RemoteProjectRecord remoteProjectRecord, @NonNull UserJson userJson) {
        super(create);

        mRemoteProjectRecord = remoteProjectRecord;
        mUserJson = userJson;
    }

    @NonNull
    @Override
    protected UserJson getCreateObject() {
        return mUserJson;
    }

    @NonNull
    public String getId() {
        return UserData.getKey(mUserJson.getEmail());
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteProjectRecord.getKey() + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + USERS + "/" + getId();
    }

    @NonNull
    public String getName() {
        return mUserJson.getName();
    }

    @NonNull
    public String getEmail() {
        return mUserJson.getEmail();
    }

    @Nullable
    public String getToken() {
        return mUserJson.getToken();
    }

    public void setName(@NonNull String name) {
        if (getName().equals(name))
            return;

        mUserJson.setName(name);
        addValue(getKey() + "/name", name);
    }

    public void setToken(@Nullable String token) {
        if (TextUtils.isEmpty(getToken()) && TextUtils.isEmpty(token))
            return;

        if (!TextUtils.isEmpty(getToken()) && getToken().equals(token))
            return;

        mUserJson.setToken(token);
        addValue(getKey() + "/token", token);
    }
}
