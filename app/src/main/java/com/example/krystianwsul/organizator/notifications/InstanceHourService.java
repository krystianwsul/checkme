package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class InstanceHourService extends IntentService {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String NOTIFICATION_ID_KEY = "notificationId";

    public static Intent getIntent(Context context, InstanceKey instanceKey, int notificationId) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceHourService.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        intent.putExtra(NOTIFICATION_ID_KEY, notificationId);
        return intent;
    }

    public InstanceHourService() {
        super("InstanceHourService");
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

        DomainFactory.getDomainFactory(this).setInstanceAddHour(0, instanceKey);

        TickService.startService(this);
    }
}
