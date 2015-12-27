package com.example.krystianwsul.organizator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

import java.util.ArrayList;

public class GroupNotificationDeleteReceiver extends BroadcastReceiver {
    public GroupNotificationDeleteReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(TickReceiver.INSTANCES_KEY);
        Assert.assertTrue(bundles != null);

        for (Bundle bundle : bundles) {
            Instance instance = InstanceData.getInstance(bundle);
            Assert.assertTrue(instance != null);

            instance.setNotified();
        }
    }
}
