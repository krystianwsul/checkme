package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import com.google.firebase.auth.FirebaseUser;

import junit.framework.Assert;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("WeakerAccess")
public class User implements Serializable {
    public String email;
    public String displayName;

    @SuppressWarnings("unused")
    public User() {

    }

    public User(@NonNull FirebaseUser firebaseUser) {
        email = firebaseUser.getEmail();
        Assert.assertTrue(!TextUtils.isEmpty(email));

        displayName = firebaseUser.getDisplayName();
        Assert.assertTrue(!TextUtils.isEmpty(displayName));
    }

    @NonNull
    public static String getKey(@NonNull String email) {
        Assert.assertTrue(!TextUtils.isEmpty(email));

        try {
            byte[] encoded = email.trim().toLowerCase().getBytes("UTF-8");
            return Base64.encodeToString(encoded, Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
