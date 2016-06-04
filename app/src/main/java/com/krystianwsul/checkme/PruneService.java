package com.krystianwsul.checkme;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class PruneService extends IntentService {
    public static void startService(Context context) {
        Assert.assertTrue(context != null);
        context.startService(new Intent(context, PruneService.class));
    }

    public PruneService() {
        super("PruneService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DomainFactory.getDomainFactory(this).updateTaskOldestVisible();
    }
}
