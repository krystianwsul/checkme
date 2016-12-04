package com.krystianwsul.checkme;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.krystianwsul.checkme.notifications.InstanceDoneService;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String REFRESH_KEY = "refresh";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Assert.assertTrue(remoteMessage != null);

        Log.e("asdf", "remoteMessage: " + remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        Assert.assertTrue(data != null);

        if (data.containsKey(REFRESH_KEY)) {
            String refresh = data.get(REFRESH_KEY);
            Assert.assertTrue(!TextUtils.isEmpty(refresh));
            Assert.assertTrue(data.get(REFRESH_KEY).equals("true"));

            InstanceDoneService.needsFirebase(this, domainFactory -> domainFactory.updateNotificationsTick(this, false, false, new ArrayList<>()));
        } else {
            MyCrashlytics.logException(new UnknownMessageException(data));
        }
    }

    private static class UnknownMessageException extends Exception {
        UnknownMessageException(@NonNull Map<String, String> data) {
            super(getMessage(data));
        }

        @NonNull
        private static String getMessage(@NonNull Map<String, String> data) {
            return Stream.of(data.entrySet())
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
        }
    }
}
