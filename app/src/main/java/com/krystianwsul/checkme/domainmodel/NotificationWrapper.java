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
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity;
import com.krystianwsul.checkme.notifications.GroupNotificationDeleteService;
import com.krystianwsul.checkme.notifications.InstanceDoneService;
import com.krystianwsul.checkme.notifications.InstanceHourService;
import com.krystianwsul.checkme.notifications.InstanceNotificationDeleteService;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class NotificationWrapper {
    @Nullable
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

    public abstract void cancelNotification(@NonNull Context context, int id);

    public abstract void notifyInstance(@NonNull Context context, @NonNull DomainFactory domainFactory, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now, boolean nougat);

    public abstract void notifyGroup(@NonNull Context context, @NonNull DomainFactory domainFactory, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now, boolean nougat);

    public abstract void setAlarm(@NonNull Context context, @NonNull PendingIntent pendingIntent, @NonNull TimeStamp nextAlarm);

    public abstract void cleanGroup(@NonNull Context context, @Nullable Integer lastNotificationId);

    @NonNull
    public abstract PendingIntent getPendingIntent(@NonNull Context context);

    public abstract void cancelAlarm(@NonNull Context context, @NonNull PendingIntent pendingIntent);

    private static class NotificationWrapperImpl extends NotificationWrapper {
        @Override
        public void cancelNotification(@NonNull Context context, int id) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Assert.assertTrue(notificationManager != null);

            MyCrashlytics.log("NotificationManager.cancel " + id);
            notificationManager.cancel(id);
        }

        @NonNull
        private InstanceKey getRemoteCustomTimeFixInstanceKey(@NonNull DomainFactory domainFactory, @NonNull Instance instance) { // remote custom time key hack
            InstanceKey instanceKey = instance.getInstanceKey();

            if (instanceKey.getType() == TaskKey.Type.LOCAL)
                return instanceKey;

            if (instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey == null)
                return instanceKey;

            if (instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey.getType() == TaskKey.Type.REMOTE)
                return instanceKey;

            String projectId = instance.getTask().getRemoteNonNullProject().getId();

            String customTimeId = domainFactory.getRemoteCustomTimeId(projectId, instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey);

            CustomTimeKey customTimeKey = new CustomTimeKey(projectId, customTimeId);
            ScheduleKey scheduleKey = new ScheduleKey(instanceKey.mScheduleKey.ScheduleDate, new TimePair(customTimeKey));
            return new InstanceKey(instanceKey.mTaskKey, scheduleKey);
        }

        @Override
        public void notifyInstance(@NonNull Context context, @NonNull DomainFactory domainFactory, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now, boolean nougat) {
            Task task = instance.getTask();
            int notificationId = instance.getNotificationId();

            InstanceKey instanceKey = instance.getInstanceKey();
            InstanceKey remoteCustomTimeFixInstanceKey = getRemoteCustomTimeFixInstanceKey(domainFactory, instance);

            Intent deleteIntent = InstanceNotificationDeleteService.getIntent(context, remoteCustomTimeFixInstanceKey);
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

            List<String> childNames = getChildNames(instance, now);

            String text;
            NotificationCompat.Style style;
            if (!childNames.isEmpty()) {
                text = TextUtils.join(", ", childNames);
                style = getInboxStyle(context, childNames, false);
            } else if (!TextUtils.isEmpty(task.getNote())) {
                text = task.getNote();

                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                bigTextStyle.bigText(task.getNote());

                style = bigTextStyle;
            } else {
                text = null;
                style = null;
            }

            notify(context, instance.getName(), text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.getInstanceDateTime().getTimeStamp().getLong(), style, true, nougat, false, instance.getInstanceDateTime().getTimeStamp().getLong().toString() + instance.getTask().getStartExactTimeStamp().toString());
        }

        @NonNull
        private String getInstanceText(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
            List<String> childNames = getChildNames(instance, now);

            if (!childNames.isEmpty()) {
                return " (" + TextUtils.join(", ", childNames) + ")";
            } else {
                String note = instance.getTask().getNote();

                if (!TextUtils.isEmpty(note)) {
                    return " (" + note + ")";
                } else {
                    return "";
                }
            }
        }

        @NonNull
        private List<String> getChildNames(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
            List<Instance> childInstances = instance.getChildInstances(now);

            Stream<Instance> notDone = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() == null)
                    .sortBy(childInstance -> childInstance.getTask().getStartExactTimeStamp());

            //noinspection ConstantConditions
            Stream<Instance> done = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() != null)
                    .sortBy(childInstance -> -childInstance.getDone().getLong());

            return Stream.concat(notDone, done)
                    .map(Instance::getName)
                    .collect(Collectors.toList());
        }

        @NonNull
        private NotificationCompat.InboxStyle getInboxStyle(@NonNull Context context, @NonNull List<String> lines, boolean nougatGroup) {
            Assert.assertTrue(!lines.isEmpty());

            int max = 5;

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            Stream.of(lines)
                    .limit(max)
                    .forEach(inboxStyle::addLine);

            int extraCount = lines.size() - max;

            if (extraCount > 0 && !nougatGroup) {
                inboxStyle.setSummaryText("+" + extraCount + " " + context.getString(R.string.more));
            }

            return inboxStyle;
        }

        private void notify(@NonNull Context context, @NonNull String title, @Nullable String text, int notificationId, @NonNull PendingIntent deleteIntent, @NonNull PendingIntent contentIntent, boolean silent, @NonNull List<NotificationCompat.Action> actions, @Nullable Long when, @Nullable NotificationCompat.Style style, boolean autoCancel, boolean nougat, boolean summary, @NonNull String sortKey) {
            Assert.assertTrue(!TextUtils.isEmpty(title));

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.ikona_bez)
                    .setDeleteIntent(deleteIntent)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSortKey(sortKey);

            if (!TextUtils.isEmpty(text))
                builder.setContentText(text);

            if (!silent)
                builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

            Assert.assertTrue(actions.size() <= 3);

            Stream.of(actions)
                    .forEach(builder::addAction);

            if (when != null)
                builder.setWhen(when).setShowWhen(true);

            if (style != null)
                builder.setStyle(style);

            if (autoCancel)
                builder.setAutoCancel(true);

            if (nougat) {
                builder.setGroup(TickService.GROUP_KEY);

                if (summary) {
                    builder.setGroupSummary(true);
                }
            }

            Notification notification = builder.build();

            if (!silent)
                notification.defaults |= Notification.DEFAULT_VIBRATE;

            MyCrashlytics.log("NotificationManager.notify " + notificationId);
            notificationManager.notify(notificationId, notification);
        }

        @SuppressLint("NewApi")
        private void setExact(@NonNull Context context, long time, @NonNull PendingIntent pendingIntent) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Assert.assertTrue(alarmManager != null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }

        @Override
        public void notifyGroup(@NonNull Context context, @NonNull DomainFactory domainFactory, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now, boolean nougat) {
            ArrayList<String> names = new ArrayList<>();
            ArrayList<InstanceKey> instanceKeys = new ArrayList<>();
            ArrayList<InstanceKey> remoteCustomTimeFixInstanceKeys = new ArrayList<>();
            for (Instance instance : instances) {
                names.add(instance.getName());
                instanceKeys.add(instance.getInstanceKey());
                remoteCustomTimeFixInstanceKeys.add(getRemoteCustomTimeFixInstanceKey(domainFactory, instance));
            }

            Intent deleteIntent = GroupNotificationDeleteService.getIntent(context, remoteCustomTimeFixInstanceKeys);
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
                    .map(instance -> instance.getName() + getInstanceText(instance, now))
                    .collect(Collectors.toList()), nougat);

            notify(context, instances.size() + " " + context.getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, new ArrayList<>(), null, inboxStyle, false, nougat, true, "0");
        }

        @Override
        public void setAlarm(@NonNull Context context, @NonNull PendingIntent pendingIntent, @NonNull TimeStamp nextAlarm) {
            setExact(context, nextAlarm.getLong(), pendingIntent);
        }

        @NonNull
        @Override
        public PendingIntent getPendingIntent(@NonNull Context context) {
            Intent nextIntent = TickService.getIntent(context, false, "NotificationWrapper: TickService.getIntent");

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Assert.assertTrue(pendingIntent != null);

            return pendingIntent;
        }

        @Override
        public void cancelAlarm(@NonNull Context context, @NonNull PendingIntent pendingIntent) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Assert.assertTrue(alarmManager != null);

            alarmManager.cancel(pendingIntent);
        }

        @Override
        public void cleanGroup(@NonNull Context context, @Nullable Integer lastNotificationId) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                if (lastNotificationId != null) {
                    cancelNotification(context, lastNotificationId);
                }
            } else {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                Assert.assertTrue(notificationManager != null);

                StatusBarNotification[] statusBarNotifications = notificationManager.getActiveNotifications();
                Assert.assertTrue(statusBarNotifications != null);

                if (lastNotificationId != null) {
                    if (statusBarNotifications.length > 2) {
                        cancelNotification(context, lastNotificationId);
                    } else {
                        if (statusBarNotifications.length < 2) {
                            Assert.assertTrue(statusBarNotifications.length == 0);
                        } else {
                            if (Stream.of(statusBarNotifications).noneMatch(statusBarNotification -> statusBarNotification.getId() == 0))
                                NotificationException.throwException(lastNotificationId, statusBarNotifications);

                            if (Stream.of(statusBarNotifications).noneMatch(statusBarNotification -> Integer.valueOf(statusBarNotification.getId()).equals(lastNotificationId)))
                                NotificationException.throwException(lastNotificationId, statusBarNotifications);

                            cancelNotification(context, 0);
                            cancelNotification(context, lastNotificationId);
                        }
                    }
                } else {
                    if (statusBarNotifications.length != 1)
                        return;

                    Log.e("asdf", "cleaning group");

                    Assert.assertTrue(statusBarNotifications[0].getId() == 0);

                    cancelNotification(context, 0);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private static class NotificationException extends RuntimeException {
        static void throwException(int lastNotificationId, @NonNull StatusBarNotification[] statusBarNotifications) {
            throw new NotificationException("last id: " + lastNotificationId + ", shown ids: " + Stream.of(statusBarNotifications)
                    .map(StatusBarNotification::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }

        NotificationException(@NonNull String message) {
            super(message);
        }
    }
}
