package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity;
import com.krystianwsul.checkme.notifications.GroupNotificationDeleteService;
import com.krystianwsul.checkme.notifications.InstanceDoneService;
import com.krystianwsul.checkme.notifications.InstanceHourService;
import com.krystianwsul.checkme.notifications.InstanceNotificationDeleteService;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class NotificationWrapper {
    private static NotificationWrapper sInstance;

    @NonNull
    public static NotificationWrapper getInstance() {
        if (sInstance == null)
            sInstance = new NotificationWrapperImpl();
        return sInstance;
    }

    public static void setInstance(@NonNull NotificationWrapper notificationWrapper) {
        Assert.assertTrue(sInstance == null);

        sInstance = notificationWrapper;
    }

    public abstract void cancel(@NonNull Context context, int id);

    public abstract void notifyInstance(@NonNull Context context, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now);

    public abstract void notifyGroup(@NonNull Context context, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now);

    public abstract void setAlarm(@NonNull Context context, @NonNull TimeStamp nextAlarm);

    private static class NotificationWrapperImpl extends NotificationWrapper {
        @Override
        public void cancel(@NonNull Context context, int id) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Assert.assertTrue(notificationManager != null);

            notificationManager.cancel(id);
        }

        @Override
        public void notifyInstance(@NonNull Context context, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {
            Task task = instance.getTask();
            int notificationId = instance.getNotificationId();
            InstanceKey instanceKey = instance.getInstanceKey();

            Intent deleteIntent = InstanceNotificationDeleteService.getIntent(context, instanceKey);
            PendingIntent pendingDeleteIntent = PendingIntent.getService(context, notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent contentIntent = ShowInstanceActivity.getNotificationIntent(context, instanceKey);
            PendingIntent pendingContentIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            ArrayList<NotificationCompat.Action> actions = new ArrayList<>();

            Intent doneIntent = InstanceDoneService.getIntent(context, instanceKey, notificationId);
            PendingIntent pendingDoneIntent = PendingIntent.getService(context, notificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, context.getString(R.string.done), pendingDoneIntent).build());

            Intent hourIntent = InstanceHourService.getIntent(context, instanceKey, notificationId);
            PendingIntent pendingHourIntent = PendingIntent.getService(context, notificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, context.getString(R.string.hour), pendingHourIntent).build());

            List<Instance> childInstances = instance.getChildInstances(now);

            String text;
            NotificationCompat.Style style;
            if (!childInstances.isEmpty()) {
                Stream<Instance> notDone = Stream.of(childInstances)
                        .filter(childInstance -> childInstance.getDone() == null)
                        .sortBy(childInstance -> childInstance.getTask().getStartExactTimeStamp());

                //noinspection ConstantConditions
                Stream<Instance> done = Stream.of(childInstances)
                        .filter(childInstance -> childInstance.getDone() != null)
                        .sortBy(childInstance -> -childInstance.getDone().getLong());

                List<String> children = Stream.concat(notDone, done)
                        .map(Instance::getName)
                        .collect(Collectors.toList());

                text = TextUtils.join(", ", children);
                style = getInboxStyle(context, children);
            } else if (!TextUtils.isEmpty(task.getNote())) {
                text = task.getNote();

                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                bigTextStyle.bigText(task.getNote());

                style = bigTextStyle;
            } else {
                text = null;
                style = null;
            }

            notify(context, instance.getName(), text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.getInstanceDateTime().getTimeStamp().getLong(), style, true);
        }

        @NonNull
        private NotificationCompat.InboxStyle getInboxStyle(@NonNull Context context, @NonNull List<String> lines) {
            Assert.assertTrue(!lines.isEmpty());

            int max = 5;

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            Stream.of(lines)
                    .limit(max)
                    .forEach(inboxStyle::addLine);

            int extraCount = lines.size() - max;

            if (extraCount > 0)
                inboxStyle.setSummaryText("+" + extraCount + " " + context.getString(R.string.more));

            return inboxStyle;
        }

        private void notify(@NonNull Context context, @NonNull String title, @Nullable String text, int notificationId, @NonNull PendingIntent deleteIntent, @NonNull PendingIntent contentIntent, boolean silent, @NonNull List<NotificationCompat.Action> actions, @Nullable Long when, @Nullable NotificationCompat.Style style, boolean autoCancel) {
            Assert.assertTrue(!TextUtils.isEmpty(title));

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = (new NotificationCompat.Builder(context))
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

        @SuppressLint("NewApi")
        private void setExact(@NonNull Context context, long time, @NonNull PendingIntent pendingIntent) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Assert.assertTrue(alarmManager != null);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }

        @Override
        public void notifyGroup(@NonNull Context context, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now) {
            Assert.assertTrue(instances.size() > TickService.MAX_NOTIFICATIONS);

            ArrayList<String> names = new ArrayList<>();
            ArrayList<InstanceKey> instanceKeys = new ArrayList<>();
            for (Instance instance : instances) {
                names.add(instance.getName());
                instanceKeys.add(instance.getInstanceKey());
            }

            Intent deleteIntent = GroupNotificationDeleteService.getIntent(context, instanceKeys);
            PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent contentIntent = ShowNotificationGroupActivity.getIntent(context, instanceKeys);
            PendingIntent pendingContentIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.InboxStyle inboxStyle = getInboxStyle(context, Stream.of(instances)
                    .sorted((lhs, rhs) -> {
                        int timeStampComparison = lhs.getInstanceDateTime().getTimeStamp().compareTo(rhs.getInstanceDateTime().getTimeStamp());
                        if (timeStampComparison != 0)
                            return timeStampComparison;

                        return lhs.getTask().getStartExactTimeStamp().compareTo(rhs.getTask().getStartExactTimeStamp());
                    })
                    .map(notificationInstanceData -> notificationInstanceData.getName() + " (" + notificationInstanceData.getDisplayText(context, now) + ")")
                    .collect(Collectors.toList()));

            notify(context, instances.size() + " " + context.getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, new ArrayList<>(), null, inboxStyle, false);
        }

        @Override
        public void setAlarm(@NonNull Context context, @NonNull TimeStamp nextAlarm) {
            Intent nextIntent = TickService.getIntent(context, false, new ArrayList<>(), "NotificationWrapper: TickService.getIntent");

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Assert.assertTrue(pendingIntent != null);

            setExact(context, nextAlarm.getLong(), pendingIntent);
        }
    }
}
