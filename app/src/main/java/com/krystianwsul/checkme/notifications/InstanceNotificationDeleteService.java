package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;

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

        if (instanceKey.getType().equals(TaskKey.Type.REMOTE)) {
            GroupNotificationDeleteService.needsFirebase(this, domainFactory -> setInstanceNotified(domainFactory, instanceKey)); // todo shouldn't actually need the remote data
        } else {
            setInstanceNotified(DomainFactory.getDomainFactory(this), instanceKey);
        }
    }

    private void setInstanceNotified(@NonNull DomainFactory domainFactory, @NonNull InstanceKey instanceKey) {
        domainFactory.setInstanceNotified(this, 0, instanceKey);
    }
}
