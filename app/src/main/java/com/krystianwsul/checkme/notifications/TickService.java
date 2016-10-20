package com.krystianwsul.checkme.notifications;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.DomainLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TickService extends IntentService {
    public static final int MAX_NOTIFICATIONS = 4;

    private static final String SILENT_KEY = "silent";
    private static final String REGISTERING_KEY = "registering";
    private static final String TASK_KEYS_KEY = "taskKeys";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void register(@NonNull Context context) {
        context.startService(getIntent(context, true, true, new ArrayList<>()));
    }

    public static void startService(@NonNull Context context) {
        context.startService(getIntent(context, true, false, new ArrayList<>()));
    }

    public static void startService(@NonNull Context context, @NonNull ArrayList<TaskKey> taskKeys) {
        context.startService(getIntent(context, true, false, taskKeys));
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

        DomainFactory.getDomainFactory(this).updateNotifications(this, silent, registering, taskKeys);
    }

    public static class Data extends DomainLoader.Data {
        public final Map<InstanceKey, NotificationInstanceData> NotificationInstanceDatas;
        public final Map<InstanceKey, ShownInstanceData> ShownInstanceDatas;
        public final TimeStamp NextAlarm;

        public Data(Map<InstanceKey, NotificationInstanceData> notificationInstanceDatas, Map<InstanceKey, ShownInstanceData> shownInstanceDatas, TimeStamp nextAlarm) {
            Assert.assertTrue(notificationInstanceDatas != null);
            Assert.assertTrue(shownInstanceDatas != null);

            NotificationInstanceDatas = notificationInstanceDatas;
            ShownInstanceDatas = shownInstanceDatas;
            NextAlarm = nextAlarm;
        }
    }

    public static class NotificationInstanceData {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final int NotificationId;
        public final String DisplayText;
        public final TimeStamp InstanceTimeStamp;
        public final List<String> Children;
        public final String mNote;
        public final boolean mUpdate;

        @NonNull
        public final ExactTimeStamp mTaskStartExactTimeStamp;

        public NotificationInstanceData(@NonNull InstanceKey instanceKey, @NonNull String name, int notificationId, @NonNull String displayText, @NonNull TimeStamp instanceTimeStamp, @NonNull List<String> children, @Nullable String note, boolean update, @NonNull ExactTimeStamp taskStartExactTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(displayText));

            InstanceKey = instanceKey;
            Name = name;
            NotificationId = notificationId;
            DisplayText = displayText;
            InstanceTimeStamp = instanceTimeStamp;
            Children = children;
            mNote = note;
            mUpdate = update;
            mTaskStartExactTimeStamp = taskStartExactTimeStamp;
        }
    }

    public static class ShownInstanceData {
        public final int NotificationId;
        public final InstanceKey InstanceKey;

        public ShownInstanceData(int notificationId, InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);

            NotificationId = notificationId;
            InstanceKey = instanceKey;
        }
    }
}
