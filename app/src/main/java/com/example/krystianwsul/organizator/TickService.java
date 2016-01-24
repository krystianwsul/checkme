package com.example.krystianwsul.organizator;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.gui.instances.InstanceData;

import junit.framework.Assert;

import java.util.ArrayList;

public class TickService extends IntentService {
    private static boolean mRegistered = false;

    private static final int MAX_NOTIFICATIONS = 4;

    public static final String INSTANCE_KEY = "instance";
    public static final String INSTANCES_KEY = "instances";

    public static synchronized void register(Context context) {
        if (mRegistered)
            return;

        Intent intent = new Intent(context, TickService.class);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 60 * 1000, pendingIntent);

        mRegistered = true;
    }

    public static void startService(Context context) {
        Assert.assertTrue(context != null);

        context.startService(new Intent(context, TickService.class));
    }

    public TickService() {
        super("TickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        DomainFactory.InstanceFactory instanceFactory = domainFactory.getInstanceFactory();
        Assert.assertTrue(instanceFactory != null);

        ArrayList<Instance> instances = instanceFactory.getNotificationInstances();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ArrayList<Instance> shownInstances = instanceFactory.getShownInstances();

        if (instances.size() > MAX_NOTIFICATIONS) {
            for (Instance instance : shownInstances) {
                notificationManager.cancel(instance.getNotificationId());
                instance.setNotificationShown(false);
            }

            domainFactory.getPersistenceManager().save();

            notify(instances);
        } else {
            notificationManager.cancel(0);

            for (Instance instance : shownInstances) {
                if (!instances.contains(instance)) {
                    notificationManager.cancel(instance.getNotificationId());
                    instance.setNotificationShown(false);
                }
            }

            for (Instance instance : instances)
                notify(instance);

            domainFactory.getPersistenceManager().save();
        }
    }

    private void notify(Instance instance) {
        Assert.assertTrue(instance != null);

        instance.setNotificationShown(true);

        Intent deleteIntent = new Intent(this, InstanceNotificationDeleteReceiver.class);
        deleteIntent.putExtra(INSTANCE_KEY, InstanceData.getBundle(instance));
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(this, instance.getNotificationId(), deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = new Intent(this, InstanceNotificationContentReceiver.class);
        contentIntent.putExtra(INSTANCE_KEY, InstanceData.getBundle(instance));
        PendingIntent pendingContentIntent = PendingIntent.getBroadcast(this, instance.getNotificationId(), contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notify(instance.getName(), instance.getDisplayText(this), instance.getNotificationId(), pendingDeleteIntent, pendingContentIntent);
    }

    private void notify(ArrayList<Instance> instances) {
        Assert.assertTrue(instances != null);
        Assert.assertTrue(instances.size() > MAX_NOTIFICATIONS);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (Instance instance : instances) {
            names.add(instance.getName());
            bundles.add(InstanceData.getBundle(instance));
        }

        Intent deleteIntent = new Intent(this, GroupNotificationDeleteReceiver.class);
        deleteIntent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = new Intent(this, GroupNotificationContentReceiver.class);
        contentIntent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);
        PendingIntent pendingContentIntent = PendingIntent.getBroadcast(this, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notify(instances.size() + " " + getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent);
    }

    private void notify(String title, String text, int notificationId, PendingIntent deleteIntent, PendingIntent contentIntent) {
        Assert.assertTrue(!TextUtils.isEmpty(title));
        Assert.assertTrue(!TextUtils.isEmpty(text));
        Assert.assertTrue(deleteIntent != null);
        Assert.assertTrue(contentIntent != null);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = (new NotificationCompat.Builder(this)).setContentTitle(title).setContentText(text).setSmallIcon(R.drawable.ic_label_outline_white_24dp).setSound(Settings.System.DEFAULT_NOTIFICATION_URI).setDeleteIntent(deleteIntent).setContentIntent(contentIntent).setAutoCancel(true).build();
        notificationManager.notify(notificationId, notification);
    }
}
