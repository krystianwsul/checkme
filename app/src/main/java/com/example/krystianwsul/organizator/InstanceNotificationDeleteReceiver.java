package com.example.krystianwsul.organizator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

public class InstanceNotificationDeleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getBundleExtra(TickService.INSTANCE_KEY);
        Assert.assertTrue(bundle != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(context);
        Assert.assertTrue(domainFactory != null);

        Instance instance = InstanceData.getInstance(domainFactory, bundle);
        Assert.assertTrue(instance != null);

        instance.setNotified();
        instance.setNotificationShown(false);

        domainFactory.getPersistenceManager().save();
    }
}
