package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;

public class UserJson {
    @Nullable
    private String email;

    @Nullable
    private String name;

    @Nullable
    private String token;

    @Nullable
    private Map<String, String> tokens;

    @SuppressWarnings("unused")
    public UserJson() {

    }

    public UserJson(@NonNull String email, @NonNull String name, @Nullable String token, @NonNull Map<String, String> tokens) {
        Assert.assertTrue(!TextUtils.isEmpty(email));
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.email = email;
        this.name = name;
        this.token = token;
        this.tokens = tokens;
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

    @NonNull
    public Map<String, String> getTokens() {
        if (tokens == null)
            return new HashMap<>();
        else
            return tokens;
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        this.name = name;
    }

    public void setToken(@Nullable String token, @NonNull String uuid) {
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        this.token = token;

        if (tokens == null)
            tokens = new HashMap<>();

        tokens.put(uuid, token);
    }
}
