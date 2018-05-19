package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.Map;

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
    public UserJson getCreateObject() {
        return mUserJson;
    }

    @NonNull
    public String getId() {
        return UserData.Companion.getKey(mUserJson.getEmail());
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

    public void setName(@NonNull String name) {
        if (getName().equals(name))
            return;

        mUserJson.setName(name);
        addValue(getKey() + "/name", name);
    }

    public void setToken(@Nullable String token, @NonNull String uuid) {
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        Map<String, String> tokens = mUserJson.getTokens();

        if (Utils.INSTANCE.stringEquals(tokens.get(uuid), token))
            return;

        mUserJson.addToken(token, uuid);
        addValue(getKey() + "/tokens/" + uuid, token);
    }
}
