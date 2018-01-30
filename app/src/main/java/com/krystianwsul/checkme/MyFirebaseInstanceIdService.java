package com.krystianwsul.checkme;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.krystianwsul.checkme.domainmodel.UserInfo;
import com.krystianwsul.checkme.notifications.InstanceDoneService;
import com.krystianwsul.checkme.persistencemodel.SaveService;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        Log.e("asdf", "onTokenRefresh " + getToken());

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null)
            return;

        UserInfo userInfo = new UserInfo(firebaseUser);

        InstanceDoneService.Companion.throttleFirebase(this, true, domainFactory -> domainFactory.updateUserInfo(this, SaveService.Source.SERVICE, userInfo));
    }

    @Nullable
    public static String getToken() {
        return FirebaseInstanceId.getInstance().getToken();
    }
}
