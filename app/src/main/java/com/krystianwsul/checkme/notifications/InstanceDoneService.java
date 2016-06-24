package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;

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

        DomainFactory.getDomainFactory(this).setInstanceNotificationDone(0, instanceKey);
    }
}
