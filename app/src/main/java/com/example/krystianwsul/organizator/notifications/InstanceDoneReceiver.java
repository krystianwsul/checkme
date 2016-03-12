package com.example.krystianwsul.organizator.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class InstanceDoneReceiver extends BroadcastReceiver {
    private static final String INSTANCE_KEY = "instanceKey";

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceDoneReceiver.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        return intent;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));

        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        DomainFactory.getDomainFactory(context).setInstanceDone(0, instanceKey, true);

        TickService.startService(context);
    }
}
