package com.example.krystianwsul.organizator.notifications;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.ShowInstanceActivity;
import com.example.krystianwsul.organizator.gui.instances.ShowNotificationGroupActivity;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class TickService extends IntentService {
    private static final int MAX_NOTIFICATIONS = 4;

    private static final String SILENT_KEY = "silent";
    private static final String REGISTERING_KEY = "registering";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void register(Context context) {
        context.startService(getIntent(context, true, true));
    }

    public static void startService(Context context) {
        Assert.assertTrue(context != null);

        context.startService(getIntent(context, true, false));
    }

    private static Intent getIntent(Context context, boolean silent, boolean registering) {
        Assert.assertTrue(context != null);

        Intent intent = new Intent(context, TickService.class);
        intent.putExtra(SILENT_KEY, silent);
        intent.putExtra(REGISTERING_KEY, registering);
        return intent;
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(SILENT_KEY));
        Assert.assertTrue(intent.hasExtra(REGISTERING_KEY));

        boolean silent = intent.getBooleanExtra(SILENT_KEY, false);
        boolean registering = intent.getBooleanExtra(REGISTERING_KEY, false);

        if (!silent) {
            SharedPreferences sharedPreferences = getSharedPreferences(TICK_PREFERENCES, MODE_PRIVATE);
            sharedPreferences.edit().putLong(LAST_TICK_KEY, ExactTimeStamp.getNow().getLong()).apply();
        }

        Data data = DomainFactory.getDomainFactory(this).getTickServiceData(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ArrayList<InstanceKey> shownInstanceKeys = new ArrayList<>();
        for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas.values())
            shownInstanceKeys.add(shownInstanceData.InstanceKey);

        ArrayList<InstanceKey> notificationInstanceKeys = new ArrayList<>();
        for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values())
            notificationInstanceKeys.add(notificationInstanceData.InstanceKey);

        ArrayList<InstanceKey> showInstanceKeys = new ArrayList<>();
        for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values())
            if (!shownInstanceKeys.contains(notificationInstanceData.InstanceKey))
                showInstanceKeys.add(notificationInstanceData.InstanceKey);

        ArrayList<InstanceKey> hideInstanceKeys = new ArrayList<>();
        for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas.values())
            if (!notificationInstanceKeys.contains(shownInstanceData.InstanceKey))
                hideInstanceKeys.add(shownInstanceData.InstanceKey);

        if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty())
            DomainFactory.getDomainFactory(this).updateInstancesShown(data.DataId, showInstanceKeys, hideInstanceKeys);

        if (registering) {
            if (data.NotificationInstanceDatas.size() > MAX_NOTIFICATIONS) { // show group
                notifyGroup(data.NotificationInstanceDatas.values(), silent);
            } else { // show instances
                for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values()) {
                    Assert.assertTrue(notificationInstanceData != null);
                    notifyInstance(notificationInstanceData, silent);
                }
            }
        } else {
            if (data.NotificationInstanceDatas.size() > MAX_NOTIFICATIONS) { // show group
                if (data.ShownInstanceDatas.size() > MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty())
                        notifyGroup(data.NotificationInstanceDatas.values(), silent);
                } else { // instances shown
                    for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas.values())
                        notificationManager.cancel(shownInstanceData.NotificationId);

                    notifyGroup(data.NotificationInstanceDatas.values(), silent);
                }
            } else { // show instances
                if (data.ShownInstanceDatas.size() > MAX_NOTIFICATIONS) { // group shown
                    notificationManager.cancel(0);

                    for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values())
                        notifyInstance(notificationInstanceData, silent);
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        ShownInstanceData shownInstanceData = data.ShownInstanceDatas.get(hideInstanceKey);
                        Assert.assertTrue(shownInstanceData != null);

                        notificationManager.cancel(shownInstanceData.NotificationId);
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        NotificationInstanceData notificationInstanceData = data.NotificationInstanceDatas.get(showInstanceKey);
                        Assert.assertTrue(notificationInstanceData != null);

                        notifyInstance(notificationInstanceData, silent);
                    }
                }
            }
        }

        if (data.NextAlarm != null) {
            Intent nextIntent = getIntent(this, false, false);

            PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            alarmManager.set(AlarmManager.RTC_WAKEUP, data.NextAlarm.getLong(), pendingIntent);
        }
    }

    private void notifyInstance(NotificationInstanceData notificationInstanceData, boolean silent) {
        Assert.assertTrue(notificationInstanceData != null);

        Intent deleteIntent = InstanceNotificationDeleteService.getIntent(this, notificationInstanceData.InstanceKey);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(this, notificationInstanceData.NotificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowInstanceActivity.getNotificationIntent(this, notificationInstanceData.InstanceKey);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(this, notificationInstanceData.NotificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        ArrayList<NotificationCompat.Action> actions = new ArrayList<>();

        Intent doneIntent = InstanceDoneService.getIntent(this, notificationInstanceData.InstanceKey, notificationInstanceData.NotificationId);
        PendingIntent pendingDoneIntent = PendingIntent.getService(this, notificationInstanceData.NotificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, getString(R.string.done), pendingDoneIntent).build());

        Intent hourIntent = InstanceHourService.getIntent(this, notificationInstanceData.InstanceKey, notificationInstanceData.NotificationId);
        PendingIntent pendingHourIntent = PendingIntent.getService(this, notificationInstanceData.NotificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, getString(R.string.hour), pendingHourIntent).build());

        notify(notificationInstanceData.Name, notificationInstanceData.DisplayText, notificationInstanceData.NotificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, notificationInstanceData.InstanceTimeStamp.getLong(), null, true);
    }

    private void notifyGroup(Collection<NotificationInstanceData> notificationInstanceDatas, boolean silent) {
        Assert.assertTrue(notificationInstanceDatas != null);
        Assert.assertTrue(notificationInstanceDatas.size() > MAX_NOTIFICATIONS);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<InstanceKey> instanceKeys = new ArrayList<>();
        for (NotificationInstanceData notificationInstanceData : notificationInstanceDatas) {
            names.add(notificationInstanceData.Name);
            instanceKeys.add(notificationInstanceData.InstanceKey);
        }

        Intent deleteIntent = GroupNotificationDeleteService.getIntent(this, instanceKeys);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowNotificationGroupActivity.getIntent(this, instanceKeys);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        ArrayList<NotificationInstanceData> notificationInstanceDataArray = new ArrayList<>(notificationInstanceDatas);
        Collections.sort(notificationInstanceDataArray, new Comparator<NotificationInstanceData>() {
            @Override
            public int compare(NotificationInstanceData lhs, NotificationInstanceData rhs) {
                int timeStampComparison = lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp);
                if (timeStampComparison != 0)
                    return timeStampComparison;

                return Integer.valueOf(lhs.InstanceKey.TaskId).compareTo(rhs.InstanceKey.TaskId);
            }
        });

        int lineCount = Math.min(5, notificationInstanceDataArray.size());
        for (int i = 0; i < lineCount; i++) {
            NotificationInstanceData notificationInstanceData = notificationInstanceDataArray.get(i);
            inboxStyle.addLine(notificationInstanceData.Name + " (" + notificationInstanceData.DisplayText + ")");
        }
        int extraCount = notificationInstanceDatas.size() - lineCount;
        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + getString(R.string.more));

        notify(notificationInstanceDatas.size() + " " + getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, null, null, inboxStyle, false);
    }

    private void notify(String title, String text, int notificationId, PendingIntent deleteIntent, PendingIntent contentIntent, boolean silent, ArrayList<NotificationCompat.Action> actions, Long when, NotificationCompat.InboxStyle inboxStyle, boolean autoCancel) {
        Assert.assertTrue(!TextUtils.isEmpty(title));
        Assert.assertTrue(!TextUtils.isEmpty(text));
        Assert.assertTrue(deleteIntent != null);
        Assert.assertTrue(contentIntent != null);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = (new NotificationCompat.Builder(this))
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_label_outline_white_24dp)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        if (actions != null) {
            Assert.assertTrue(actions.size() <= 3);
            for (NotificationCompat.Action action : actions)
                builder.addAction(action);
        }

        if (when != null)
            builder.setWhen(when);

        if (inboxStyle != null)
            builder.setStyle(inboxStyle);

        if (autoCancel)
            builder.setAutoCancel(true);

        Notification notification = builder.build();

        if (!silent)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        notificationManager.notify(notificationId, notification);
    }

    public static class Data extends DomainLoader.Data {
        public final HashMap<InstanceKey, NotificationInstanceData> NotificationInstanceDatas;
        public final HashMap<InstanceKey, ShownInstanceData> ShownInstanceDatas;
        public final TimeStamp NextAlarm;

        public Data(HashMap<InstanceKey, NotificationInstanceData> notificationInstanceDatas, HashMap<InstanceKey, ShownInstanceData> shownInstanceDatas, TimeStamp nextAlarm) {
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

        public NotificationInstanceData(InstanceKey instanceKey, String name, int notificationId, String displayText, TimeStamp instanceTimeStamp) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(displayText));
            Assert.assertTrue(instanceTimeStamp != null);

            InstanceKey = instanceKey;
            Name = name;
            NotificationId = notificationId;
            DisplayText = displayText;
            InstanceTimeStamp = instanceTimeStamp;
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
