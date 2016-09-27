package com.krystianwsul.checkme.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.PruneService;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity;
import com.krystianwsul.checkme.loaders.DomainLoader;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TickService extends IntentService {
    private static final int MAX_NOTIFICATIONS = 4;

    private static final String SILENT_KEY = "silent";
    private static final String REGISTERING_KEY = "registering";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String TASK_UPDATED_KEY = "taskUpdated";

    public static final String TICK_PREFERENCES = "tickPreferences";
    public static final String LAST_TICK_KEY = "lastTick";

    // DON'T HOLD STATE IN STATIC VARIABLES

    public static void register(@NonNull Context context) {
        context.startService(getIntent(context, true, true, new ArrayList<>()));
    }

    public static void startService(@NonNull Context context) {
        context.startService(getIntent(context, true, false, new ArrayList<>()));
    }

    public static void startService(@NonNull Context context, @NonNull ArrayList<Integer> taskIds) {
        context.startService(getIntent(context, true, false, taskIds));
    }

    public static void startServiceDebug(@NonNull Context context) {
        context.startService(getIntent(context, false, false, new ArrayList<>()));
    }

    private static Intent getIntent(@NonNull Context context, boolean silent, boolean registering, @NonNull ArrayList<Integer> taskIds) {
        Assert.assertTrue(!registering || silent);

        Intent intent = new Intent(context, TickService.class);
        intent.putExtra(SILENT_KEY, silent);
        intent.putExtra(REGISTERING_KEY, registering);
        intent.putExtra(TASK_IDS_KEY, taskIds);
        return intent;
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Assert.assertTrue(intent.hasExtra(SILENT_KEY));
        Assert.assertTrue(intent.hasExtra(REGISTERING_KEY));
        Assert.assertTrue(intent.hasExtra(TASK_IDS_KEY));

        boolean silent = intent.getBooleanExtra(SILENT_KEY, false);
        boolean registering = intent.getBooleanExtra(REGISTERING_KEY, false);

        List<Integer> taskIds = intent.getIntegerArrayListExtra(TASK_IDS_KEY);
        Assert.assertTrue(taskIds != null);

        if (!silent) {
            SharedPreferences sharedPreferences = getSharedPreferences(TICK_PREFERENCES, MODE_PRIVATE);
            sharedPreferences.edit().putLong(LAST_TICK_KEY, ExactTimeStamp.getNow().getLong()).apply();
        }

        Data data = DomainFactory.getDomainFactory(this).getTickServiceData(this, taskIds);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Assert.assertTrue(notificationManager != null);

        List<InstanceKey> shownInstanceKeys = Stream.of(data.ShownInstanceDatas.values())
                .map(shownInstanceData -> shownInstanceData.InstanceKey)
                .collect(Collectors.toList());

        List<InstanceKey> notificationInstanceKeys = Stream.of(data.NotificationInstanceDatas.values())
                .map(notificationInstanceData -> notificationInstanceData.InstanceKey)
                .collect(Collectors.toList());

        List<InstanceKey> showInstanceKeys = Stream.of(data.NotificationInstanceDatas.values())
                .map(notificationInstanceData -> notificationInstanceData.InstanceKey)
                .filter(instanceKey -> !shownInstanceKeys.contains(instanceKey))
                .collect(Collectors.toList());

        List<InstanceKey> hideInstanceKeys = Stream.of(data.ShownInstanceDatas.values())
                .map(shownInstanceData -> shownInstanceData.InstanceKey)
                .filter(instanceKey -> !notificationInstanceKeys.contains(instanceKey))
                .collect(Collectors.toList());

        if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty())
            DomainFactory.getDomainFactory(this).updateInstancesShown(this, data.DataId, showInstanceKeys, hideInstanceKeys);

        Log.e("asdf", "a");
        if (registering) {
            Log.e("asdf", "b");
            Assert.assertTrue(silent);

            if (data.NotificationInstanceDatas.size() > MAX_NOTIFICATIONS) { // show group
                notifyGroup(data.NotificationInstanceDatas.values(), true);
            } else { // show instances
                for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values()) {
                    Assert.assertTrue(notificationInstanceData != null);
                    notifyInstance(notificationInstanceData, true);
                }
            }
        } else {
            Log.e("asdf", "c");
            if (data.NotificationInstanceDatas.size() > MAX_NOTIFICATIONS) { // show group
                Log.e("asdf", "d");
                if (data.ShownInstanceDatas.size() > MAX_NOTIFICATIONS) { // group shown
                    Log.e("asdf", "e");
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        Log.e("asdf", "f");
                        notifyGroup(data.NotificationInstanceDatas.values(), silent);
                    } else if (Stream.of(data.NotificationInstanceDatas.values()).anyMatch(notificationInstanceData -> notificationInstanceData.mUpdate)) {
                        Log.e("asdf", "updating group");
                        notifyGroup(data.NotificationInstanceDatas.values(), true);
                    }
                } else { // instances shown
                    Log.e("asdf", "g");
                    for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas.values())
                        notificationManager.cancel(shownInstanceData.NotificationId);

                    notifyGroup(data.NotificationInstanceDatas.values(), silent);
                }
            } else { // show instances
                Log.e("asdf", "h");
                if (data.ShownInstanceDatas.size() > MAX_NOTIFICATIONS) { // group shown
                    Log.e("asdf", "i");
                    notificationManager.cancel(0);

                    for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas.values())
                        notifyInstance(notificationInstanceData, silent);
                } else { // instances shown
                    Log.e("asdf", "j");
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

                    Stream.of(data.NotificationInstanceDatas.values())
                            .filter(notificationInstanceData -> notificationInstanceData.mUpdate)
                            .filter(notificationInstanceData -> !showInstanceKeys.contains(notificationInstanceData.InstanceKey))
                            .forEach(notificationInstanceData -> notifyInstance(notificationInstanceData, true));
                }
            }
        }

        if (data.NextAlarm != null) {
            Intent nextIntent = getIntent(this, false, false, new ArrayList<>());

            PendingIntent pendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            setExact(data.NextAlarm.getLong(), pendingIntent);
        }

        PruneService.startService(this);
    }

    @SuppressLint("NewApi")
    private void setExact(long time, PendingIntent pendingIntent) {
        Assert.assertTrue(pendingIntent != null);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
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

        String text;
        NotificationCompat.Style style;
        if (!notificationInstanceData.Children.isEmpty()) {
            text = TextUtils.join(", ", notificationInstanceData.Children);
            style = getInboxStyle(notificationInstanceData.Children);
        } else if (!TextUtils.isEmpty(notificationInstanceData.mNote)) {
            text = notificationInstanceData.mNote;

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(notificationInstanceData.mNote);

            style = bigTextStyle;
        } else {
            text = null;
            style = null;
        }

        notify(notificationInstanceData.Name, text, notificationInstanceData.NotificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, notificationInstanceData.InstanceTimeStamp.getLong(), style, true);
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

        NotificationCompat.InboxStyle inboxStyle = getInboxStyle(Stream.of(notificationInstanceDatas)
                .sorted((lhs, rhs) -> {
                    int timeStampComparison = lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp);
                    if (timeStampComparison != 0)
                        return timeStampComparison;

                    return Integer.valueOf(lhs.InstanceKey.TaskId).compareTo(rhs.InstanceKey.TaskId);
                })
                .map(notificationInstanceData -> notificationInstanceData.Name + " (" + notificationInstanceData.DisplayText + ")")
                .collect(Collectors.toList()));

        notify(notificationInstanceDatas.size() + " " + getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, new ArrayList<>(), null, inboxStyle, false);
    }

    @NonNull
    private NotificationCompat.InboxStyle getInboxStyle(@NonNull List<String> lines) {
        Assert.assertTrue(!lines.isEmpty());

        int max = 5;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        Stream.of(lines)
                .limit(max)
                .forEach(inboxStyle::addLine);

        int extraCount = lines.size() - max;

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + getString(R.string.more));

        return inboxStyle;
    }

    private void notify(@NonNull String title, @Nullable String text, int notificationId, @NonNull PendingIntent deleteIntent, @NonNull PendingIntent contentIntent, boolean silent, @NonNull List<NotificationCompat.Action> actions, @Nullable Long when, @Nullable NotificationCompat.Style style, boolean autoCancel) {
        Assert.assertTrue(!TextUtils.isEmpty(title));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = (new NotificationCompat.Builder(this))
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (!TextUtils.isEmpty(text))
            builder.setContentText(text);

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        Assert.assertTrue(actions.size() <= 3);

        Stream.of(actions)
                .forEach(builder::addAction);

        if (when != null)
            builder.setWhen(when);

        if (style != null)
            builder.setStyle(style);

        if (autoCancel)
            builder.setAutoCancel(true);

        Notification notification = builder.build();

        if (!silent)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        notificationManager.notify(notificationId, notification);
    }

    public static class Data extends DomainLoader.Data {
        final Map<InstanceKey, NotificationInstanceData> NotificationInstanceDatas;
        final Map<InstanceKey, ShownInstanceData> ShownInstanceDatas;
        final TimeStamp NextAlarm;

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
        final int NotificationId;
        final String DisplayText;
        final TimeStamp InstanceTimeStamp;
        final List<String> Children;
        final String mNote;
        final boolean mUpdate;

        public NotificationInstanceData(@NonNull InstanceKey instanceKey, @NonNull String name, int notificationId, @NonNull String displayText, @NonNull TimeStamp instanceTimeStamp, @NonNull List<String> children, @Nullable String note, boolean update) {
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
        }
    }

    public static class ShownInstanceData {
        final int NotificationId;
        public final InstanceKey InstanceKey;

        public ShownInstanceData(int notificationId, InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);

            NotificationId = notificationId;
            InstanceKey = instanceKey;
        }
    }
}
