package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.NotificationWrapper;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

public class InstanceHourService extends IntentService {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String NOTIFICATION_ID_KEY = "notificationId";

    public static Intent getIntent(Context context, InstanceKey instanceKey, int notificationId) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, InstanceHourService.class);
        intent.putExtra(INSTANCE_KEY, (Parcelable) instanceKey);
        intent.putExtra(NOTIFICATION_ID_KEY, notificationId);
        return intent;
    }

    public InstanceHourService() {
        super("InstanceHourService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        Assert.assertTrue(intent.hasExtra(NOTIFICATION_ID_KEY));

        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(instanceKey != null);

        int notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1);
        Assert.assertTrue(notificationId != -1);

        NotificationWrapper notificationWrapper = NotificationWrapper.getInstance();
        notificationWrapper.cleanGroup(this, notificationId);

        if (instanceKey.getType().equals(TaskKey.Type.REMOTE)) {
            InstanceDoneService.needsFirebase(this, domainFactory -> setInstanceAddHour(domainFactory, instanceKey));
        } else {
            setInstanceAddHour(DomainFactory.getDomainFactory(this), instanceKey);
        }
    }

    private void setInstanceAddHour(@NonNull DomainFactory domainFactory, @NonNull InstanceKey instanceKey) {
        domainFactory.setInstanceAddHourService(this, 0, instanceKey);
    }
}
