package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

public class InstanceNotificationDeleteService extends IntentService {
    private final static String INSTANCE_KEY = "instance";

    public static Intent getIntent(Context context, Instance instance) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instance != null);

        Intent intent = new Intent(context, InstanceNotificationDeleteService.class);
        intent.putExtra(INSTANCE_KEY, InstanceData.getBundle(instance));
        return intent;
    }

    public InstanceNotificationDeleteService() {
        super("InstanceNotificationDeleteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getBundleExtra(INSTANCE_KEY);
        Assert.assertTrue(bundle != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        Instance instance = InstanceData.getInstance(domainFactory, bundle);
        Assert.assertTrue(instance != null);

        domainFactory.setInstanceNotifiedNotShown(instance);

        domainFactory.save();
    }
}
