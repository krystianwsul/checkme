package com.example.krystianwsul.organizator;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TickReceiver extends BroadcastReceiver {
    private static boolean mRegistered = false;

    private static final int MAX_NOTIFICATIONS = 4;

    private static final String PREFERENCE_FILE_KEY = "com.example.krystianwsul.organizator.TICK_RECEIVER_PREFERENCES";
    public static final String INSTANCE_KEY = "instance";
    public static final String INSTANCES_KEY = "instances";

    public static void register(Context context) {
        if (mRegistered)
            return;

        Intent intent = new Intent(context.getApplicationContext(), TickReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 60 * 1000, pendingIntent);

        mRegistered = true;
    }

    public TickReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<Instance> instances = InstanceFactory.getInstance().getNotificationInstances();

        if (instances.isEmpty())
            return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (instances.size() > MAX_NOTIFICATIONS) {
            ArrayList<Instance> shownInstances = InstanceFactory.getInstance().getShownInstances();
            for (Instance instance : shownInstances) {
                notificationManager.cancel(instance.getNotificationId());
                instance.setNotificationShown(false);
            }

            notify(context, instances);
        } else {
            notificationManager.cancel(0);

            for (Instance instance : instances)
                notify(context, instance);
        }
    }

    private void notify(Context context, Instance instance) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instance != null);

        instance.setNotificationShown(true);

        Intent intent = new Intent(context, InstanceNotificationDeleteReceiver.class);
        intent.putExtra(INSTANCE_KEY, InstanceData.getBundle(instance));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        notify(context, instance.getName(), instance.getDisplayText(context), instance.getNotificationId(), pendingIntent);
    }

    private void notify(Context context, ArrayList<Instance> instances) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(instances.size() > MAX_NOTIFICATIONS);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (Instance instance : instances) {
            names.add(instance.getName());
            bundles.add(InstanceData.getBundle(instance));
        }

        Intent intent = new Intent(context, GroupNotificationDeleteReceiver.class);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        notify(context, instances.size() + " " + context.getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingIntent);
    }

    private void notify(Context context, String title, String text, int notificationId, PendingIntent deleteIntent) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(!TextUtils.isEmpty(title));
        Assert.assertTrue(!TextUtils.isEmpty(text));
        Assert.assertTrue(deleteIntent != null);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = (new NotificationCompat.Builder(context)).setContentTitle(title).setContentText(text).setSmallIcon(R.drawable.ic_label_outline_white_24dp).setSound(Settings.System.DEFAULT_NOTIFICATION_URI).setDeleteIntent(deleteIntent).build();
        notificationManager.notify(notificationId, notification);
    }
}
