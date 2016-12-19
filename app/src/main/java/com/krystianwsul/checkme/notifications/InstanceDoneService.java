package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

public class InstanceDoneService extends IntentService {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String NOTIFICATION_ID_KEY = "notificationId";

    public static Intent getIntent(Context context, InstanceKey instanceKey, int notificationId) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceDoneService.class);
        intent.putExtra(INSTANCE_KEY, (Parcelable) instanceKey);
        intent.putExtra(NOTIFICATION_ID_KEY, notificationId);
        return intent;
    }

    public InstanceDoneService() {
        super("InstanceDoneService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        Assert.assertTrue(intent.hasExtra(NOTIFICATION_ID_KEY));

        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        int notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1);
        Assert.assertTrue(notificationId != -1);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        hideGroup(notificationManager);

        if (instanceKey.getType().equals(TaskKey.Type.REMOTE)) {
            needsFirebase(this, domainFactory -> setInstanceNotificationDone(domainFactory, instanceKey));
        } else {
            setInstanceNotificationDone(DomainFactory.getDomainFactory(this), instanceKey);
        }
    }

    static void hideGroup(@NonNull NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;

        StatusBarNotification[] statusBarNotifications = notificationManager.getActiveNotifications();
        if (statusBarNotifications.length > 1)
            return;

        Assert.assertTrue(statusBarNotifications.length == 1);
        Assert.assertTrue(statusBarNotifications[0].getId() == 0);

        notificationManager.cancel(0);
    }

    private void setInstanceNotificationDone(@NonNull DomainFactory domainFactory, @NonNull InstanceKey instanceKey) {
        domainFactory.setInstanceNotificationDone(this, 0, instanceKey);
    }

    public static void needsFirebase(@NonNull Context context, @NonNull DomainFactory.FirebaseListener firebaseListener) {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(context.getApplicationContext());

        if (domainFactory.isConnected()) {
            firebaseListener.onFirebaseResult(domainFactory);
        } else {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                UserData userData = new UserData(firebaseUser);

                domainFactory.setUserData(context.getApplicationContext(), userData);
                domainFactory.addFirebaseListener(firebaseListener);
            } else {
                throw new NeedsFirebaseException();
            }
        }
    }

    public static class NeedsFirebaseException extends RuntimeException {
        public NeedsFirebaseException() {
            super();
        }
    }
}
