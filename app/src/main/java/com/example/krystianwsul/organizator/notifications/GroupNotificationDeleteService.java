package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

import java.util.ArrayList;

public class GroupNotificationDeleteService extends IntentService {
    private final static String INSTANCES_KEY = "instances";

    public GroupNotificationDeleteService() {
        super("GroupNotificationDeleteService");
    }

    public static Intent getIntent(Context context, ArrayList<Bundle> bundles) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        Intent intent = new Intent(context, GroupNotificationDeleteService.class);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(bundles != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Bundle bundle : bundles) {
            Instance instance = InstanceData.getInstance(domainFactory, bundle);
            Assert.assertTrue(instance != null);

            instances.add(instance);
        }

        domainFactory.setInstancesNotified(instances);

        domainFactory.save();
    }
}
