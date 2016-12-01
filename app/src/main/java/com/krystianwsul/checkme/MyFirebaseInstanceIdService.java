package com.krystianwsul.checkme;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.UserData;

import junit.framework.Assert;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        Log.e("asdf", "onTokenRefresh " + getToken());

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Assert.assertTrue(firebaseUser != null);

        UserData userData = new UserData(firebaseUser);

        DatabaseWrapper.setUserData(userData);
    }

    @NonNull
    public static String getToken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Assert.assertTrue(!TextUtils.isEmpty(token));

        return token;
    }
}
