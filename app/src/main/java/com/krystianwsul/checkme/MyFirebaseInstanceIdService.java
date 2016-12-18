package com.krystianwsul.checkme;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.notifications.InstanceDoneService;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        Log.e("asdf", "onTokenRefresh " + getToken());

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null)
            return;

        UserData userData = new UserData(firebaseUser);

        InstanceDoneService.needsFirebase(this, domainFactory -> domainFactory.updateUserData(this, userData));
    }

    @Nullable
    public static String getToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }
}
