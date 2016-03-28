package com.example.krystianwsul.organizator.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class InstanceHourService extends IntentService {
    private static final String INSTANCE_KEY = "instanceKey";

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceHourService.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        return intent;
    }

    public InstanceHourService() {
        super("InstanceHourService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));

        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        DomainFactory.getDomainFactory(this).setInstanceAddHour(0, instanceKey);

        TickService.startService(this);
    }
}
