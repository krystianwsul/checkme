package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class TickService extends IntentService {
    public static final int MAX_NOTIFICATIONS = 4;

    private static final String SILENT_KEY = "silent";
    private static final String REGISTERING_KEY = "registering";
    private static final String TASK_KEYS_KEY = "taskKeys";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void startServiceRegister(@NonNull Context context) {
        context.startService(getIntent(context, true, true, new ArrayList<>()));
    }

    public static void startServiceTimeChange(@NonNull Context context) {
        context.startService(getIntent(context, true, false, new ArrayList<>()));
    }

    public static void startServiceDebug(@NonNull Context context) {
        context.startService(getIntent(context, false, false, new ArrayList<>()));
    }

    public static Intent getIntent(@NonNull Context context, boolean silent, boolean registering, @NonNull ArrayList<TaskKey> taskKeys) {
        Assert.assertTrue(!registering || silent);

        Intent intent = new Intent(context, TickService.class);
        intent.putExtra(SILENT_KEY, silent);
        intent.putExtra(REGISTERING_KEY, registering);
        intent.putExtra(TASK_KEYS_KEY, taskKeys);
        return intent;
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(SILENT_KEY));
        Assert.assertTrue(intent.hasExtra(REGISTERING_KEY));
        Assert.assertTrue(intent.hasExtra(TASK_KEYS_KEY));

        boolean silent = intent.getBooleanExtra(SILENT_KEY, false);
        boolean registering = intent.getBooleanExtra(REGISTERING_KEY, false);

        List<TaskKey> taskKeys = intent.getParcelableArrayListExtra(TASK_KEYS_KEY);
        Assert.assertTrue(taskKeys != null);

        DomainFactory.getDomainFactory(this).updateNotificationsTick(this, silent, registering, taskKeys);
    }
}
