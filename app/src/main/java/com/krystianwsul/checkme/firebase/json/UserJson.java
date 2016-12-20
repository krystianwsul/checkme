package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

public class UserJson {
    @Nullable
    private String email;

    @Nullable
    private String name;

    @Nullable
    private String token;

    @SuppressWarnings("unused")
    public UserJson() {

    }

    public UserJson(@NonNull String email, @NonNull String name, @Nullable String token) {
        Assert.assertTrue(!TextUtils.isEmpty(email));
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.email = email;
        this.name = name;
        this.token = token;
    }

    @NonNull
    public String getEmail() {
        Assert.assertTrue(!TextUtils.isEmpty(email));

        return email;
    }

    @NonNull
    public String getName() {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        return name;
    }

    @Nullable
    public String getToken() {
        return token;
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }
}
