package com.krystianwsul.checkme.notifications;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.UserInfo;

import junit.framework.Assert;

public class TickService extends WakefulIntentService {
    public static final int MAX_NOTIFICATIONS = 3;
    public static final String GROUP_KEY = "group";

    private static final String SILENT_KEY = "silent";
    private static final String SOURCE_KEY = "source";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";
    public static final String TICK_LOG = "tickLog";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void startServiceRegister(@NonNull Context context, @NonNull String source) {
        WakefulIntentService.sendWakefulWork(context, getIntent(context, true, source));
    }

    public static void startServiceTimeChange(@NonNull Context context, @NonNull String source) {
        WakefulIntentService.sendWakefulWork(context, getIntent(context, true, source));
    }

    public static void startServiceDebug(@NonNull Context context, @NonNull String source) {
        WakefulIntentService.sendWakefulWork(context, getIntent(context, false, source));
    }

    public static Intent getIntent(@NonNull Context context, boolean silent, @NonNull String source) {
        Assert.assertTrue(!TextUtils.isEmpty(source));

        Intent intent = new Intent(context, TickService.class);
        intent.putExtra(SILENT_KEY, silent);
        intent.putExtra(SOURCE_KEY, source);
        return intent;
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        Assert.assertTrue(intent.hasExtra(SILENT_KEY));
        Assert.assertTrue(intent.hasExtra(SOURCE_KEY));

        boolean silent = intent.getBooleanExtra(SILENT_KEY, false);

        String source = intent.getStringExtra(SOURCE_KEY);
        Assert.assertTrue(!TextUtils.isEmpty(source));

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);

        domainFactory.updateNotificationsTick(this, silent, source);

        if (!domainFactory.isConnected()) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                domainFactory.setUserInfo(this, new UserInfo(firebaseUser));

                domainFactory.setFirebaseTickListener(this, new DomainFactory.TickData(silent, source, this));
            }
        }
    }
}
