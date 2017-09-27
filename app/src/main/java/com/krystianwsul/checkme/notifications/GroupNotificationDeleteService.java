package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class GroupNotificationDeleteService extends IntentService {
    private final static String INSTANCES_KEY = "instanceKeys";

    public static Intent getIntent(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        Assert.assertTrue(Stream.of(instanceKeys)
                .filter(instanceKey -> instanceKey.getType() == TaskKey.Type.REMOTE)
                .map(instanceKey -> instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey)
                .filter(customTimeKey -> customTimeKey != null)
                .allMatch(customTimeKey -> customTimeKey.getType() == TaskKey.Type.REMOTE));

        Intent intent = new Intent(context, GroupNotificationDeleteService.class);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys);
        return intent;
    }

    public GroupNotificationDeleteService() {
        super("GroupNotificationDeleteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        DomainFactory.getDomainFactory(this)
                .setInstancesNotified(this, 0, SaveService.Source.SERVICE, instanceKeys);
    }
}
