package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

public class InstanceNotificationDeleteService extends IntentService {
    private final static String INSTANCE_KEY = "instanceKey";

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceNotificationDeleteService.class);
        intent.putExtra(INSTANCE_KEY, (Parcelable) instanceKey);
        return intent;
    }

    public InstanceNotificationDeleteService() {
        super("InstanceNotificationDeleteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        TickService.doAfterFirebase(this, domainFactory -> domainFactory.setInstanceNotified(this, 0, instanceKey));
    }
}
