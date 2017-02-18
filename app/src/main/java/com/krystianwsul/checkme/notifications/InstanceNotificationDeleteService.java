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

    @NonNull
    public static Intent getIntent(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        if (instanceKey.getType() == TaskKey.Type.REMOTE && instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey != null)
            Assert.assertTrue(instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey.getType() == TaskKey.Type.REMOTE);

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

        DomainFactory.getDomainFactory(this)
                .setInstanceNotified(this, 0, instanceKey);
    }
}
