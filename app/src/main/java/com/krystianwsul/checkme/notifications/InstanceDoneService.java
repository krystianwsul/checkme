package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.NotificationWrapper;
import com.krystianwsul.checkme.domainmodel.UserInfo;
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

        NotificationWrapper notificationWrapper = NotificationWrapper.Companion.getInstance();
        notificationWrapper.cleanGroup(notificationId); // todo uodpornić na podwójne kliknięcie

        throttleFirebase(this, instanceKey.getType().equals(TaskKey.Type.REMOTE), domainFactory -> setInstanceNotificationDone(domainFactory, instanceKey));
    }

    private void setInstanceNotificationDone(@NonNull DomainFactory domainFactory, @NonNull InstanceKey instanceKey) {
        domainFactory.setInstanceNotificationDone(this, 0, instanceKey);
    }

    public static void throttleFirebase(@NonNull Context context, boolean needsFirebase, @NonNull DomainFactory.FirebaseListener firebaseListener) {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(context.getApplicationContext());

        if (domainFactory.isConnected()) {
            if (domainFactory.isSaved()) {
                queueFirebase(domainFactory, context, firebaseListener);
            } else {
                firebaseListener.onFirebaseResult(domainFactory);
            }
        } else {
            if (needsFirebase) {
                queueFirebase(domainFactory, context, firebaseListener);
            } else {
                firebaseListener.onFirebaseResult(domainFactory);
            }
        }
    }

    private static void queueFirebase(@NonNull DomainFactory domainFactory, @NonNull Context context, @NonNull DomainFactory.FirebaseListener firebaseListener) {
        Assert.assertTrue(!domainFactory.isConnected() || domainFactory.isSaved());

        if (!domainFactory.isConnected()) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null)
                throw new NeedsFirebaseException();

            domainFactory.setUserInfo(context.getApplicationContext(), new UserInfo(firebaseUser));
        }

        domainFactory.addFirebaseListener(firebaseListener);
    }

    private static class NeedsFirebaseException extends RuntimeException {
        NeedsFirebaseException() {
            super();
        }
    }
}
