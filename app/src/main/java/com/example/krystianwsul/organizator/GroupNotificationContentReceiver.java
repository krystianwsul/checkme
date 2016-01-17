package com.example.krystianwsul.organizator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;
import com.example.krystianwsul.organizator.gui.instances.ShowNotificationGroupActivity;

import junit.framework.Assert;

import java.util.ArrayList;

public class GroupNotificationContentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(TickReceiver.INSTANCES_KEY);
        Assert.assertTrue(bundles != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(context);
        Assert.assertTrue(domainFactory != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Bundle bundle : bundles) {
            Instance instance = InstanceData.getInstance(domainFactory, bundle);
            Assert.assertTrue(instance != null);

            instance.setNotified();
            instances.add(instance);
        }

        context.startActivity(ShowNotificationGroupActivity.getIntent(context, instances));
    }
}
