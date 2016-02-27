package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class InstanceNotificationDeleteService extends IntentService {
    private final static String INSTANCE_KEY = "instanceKey";

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceNotificationDeleteService.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        return intent;
    }

    public InstanceNotificationDeleteService() {
        super("InstanceNotificationDeleteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        DomainFactory.getDomainFactory(this).setInstanceNotifiedNotShown(0, instanceKey);
    }
}
