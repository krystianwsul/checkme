package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class TickService extends IntentService {
    public static final int MAX_NOTIFICATIONS = 4;

    private static final String SILENT_KEY = "silent";
    private static final String TASK_KEYS_KEY = "taskKeys";
    private static final String SOURCE_KEY = "source";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void startServiceRegister(@NonNull Context context, @NonNull String source) {
        context.startService(getIntent(context, true, new ArrayList<>(), source));
    }

    public static void startServiceTimeChange(@NonNull Context context, @NonNull String source) {
        context.startService(getIntent(context, true, new ArrayList<>(), source));
    }

    public static void startServiceDebug(@NonNull Context context, @NonNull String source) {
        context.startService(getIntent(context, false, new ArrayList<>(), source));
    }

    public static Intent getIntent(@NonNull Context context, boolean silent, @NonNull ArrayList<TaskKey> taskKeys, @NonNull String source) {
        Assert.assertTrue(!TextUtils.isEmpty(source));

        Intent intent = new Intent(context, TickService.class);
        intent.putExtra(SILENT_KEY, silent);
        intent.putExtra(TASK_KEYS_KEY, taskKeys);
        intent.putExtra(SOURCE_KEY, source);
        return intent;
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(SILENT_KEY));
        Assert.assertTrue(intent.hasExtra(TASK_KEYS_KEY));
        Assert.assertTrue(intent.hasExtra(SOURCE_KEY));

        boolean silent = intent.getBooleanExtra(SILENT_KEY, false);

        List<TaskKey> taskKeys = intent.getParcelableArrayListExtra(TASK_KEYS_KEY);
        Assert.assertTrue(taskKeys != null);

        String source = intent.getStringExtra(SOURCE_KEY);
        Assert.assertTrue(!TextUtils.isEmpty(source));

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);

        domainFactory.updateNotificationsTick(this, silent, taskKeys);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            UserData userData = new UserData(firebaseUser);

            domainFactory.setUserData(this, userData);

            domainFactory.setFirebaseTickListener(this, new DomainFactory.TickData(silent, taskKeys, source));
        }
    }
}
