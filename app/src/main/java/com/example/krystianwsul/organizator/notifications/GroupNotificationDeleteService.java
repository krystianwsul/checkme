package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public class GroupNotificationDeleteService extends IntentService {
    private final static String INSTANCES_KEY = "instanceKeys";

    public GroupNotificationDeleteService() {
        super("GroupNotificationDeleteService");
    }

    public static Intent getIntent(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        Intent intent = new Intent(context, GroupNotificationDeleteService.class);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(instanceKeys != null);

        DomainFactory.getDomainFactory(this).setInstanceKeysNotified(0, instanceKeys);
    }
}
