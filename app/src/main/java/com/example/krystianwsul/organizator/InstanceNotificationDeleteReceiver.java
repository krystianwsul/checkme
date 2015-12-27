package com.example.krystianwsul.organizator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

public class InstanceNotificationDeleteReceiver extends BroadcastReceiver {
    public InstanceNotificationDeleteReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getBundleExtra(TickReceiver.INSTANCE_KEY);
        Assert.assertTrue(bundle != null);

        Instance instance = InstanceData.getInstance(bundle);
        Assert.assertTrue(instance != null);

        instance.setNotified();
        instance.setNotificationShown(false);
    }
}
