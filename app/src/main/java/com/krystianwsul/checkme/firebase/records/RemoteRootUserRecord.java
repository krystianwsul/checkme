package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.json.UserWrapper;

public class RemoteRootUserRecord extends RemoteRecord {
    private static final String USERS = "users";
    private static final String USER_DATA = "userData";

    @NonNull
    private final UserWrapper mUserWrapper;

    RemoteRootUserRecord(boolean create, @NonNull UserWrapper userWrapper) {
        super(create);

        mUserWrapper = userWrapper;
    }

    @NonNull
    private UserJson getUserJson() {
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
    public String getToken() {
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
}
