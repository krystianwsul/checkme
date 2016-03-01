package com.example.krystianwsul.organizator.notifications;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.ShowInstanceActivity;
import com.example.krystianwsul.organizator.gui.instances.ShowNotificationGroupActivity;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public class TickService extends IntentService {
    private static boolean sRegistered = false;

    private static final int MAX_NOTIFICATIONS = 4;

    public static synchronized void register(Context context) {
        if (sRegistered)
            return;

        Intent intent = new Intent(context, TickService.class);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 60 * 1000, pendingIntent);

        sRegistered = true;
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
        Data data = DomainFactory.getDomainFactory(this).getTickServiceData(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ArrayList<InstanceKey> shownInstanceKeys = new ArrayList<>();
        for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas)
            shownInstanceKeys.add(shownInstanceData.InstanceKey);

        if (data.NotificationInstanceDatas.size() > MAX_NOTIFICATIONS) {
            if (!data.ShownInstanceDatas.isEmpty()) {
                for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas)
                    notificationManager.cancel(shownInstanceData.NotificationId);

                DomainFactory.getDomainFactory(this).updateInstancesShown(data.DataId, null, shownInstanceKeys);
            }

            notify(data.NotificationInstanceDatas);
        } else {
            notificationManager.cancel(0);

            ArrayList<InstanceKey> notificationInstanceKeys = new ArrayList<>();
            for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas)
                notificationInstanceKeys.add(notificationInstanceData.InstanceKey);

            ArrayList<InstanceKey> hideInstanceKeys = new ArrayList<>();
            for (ShownInstanceData shownInstanceData : data.ShownInstanceDatas) {
                if (!notificationInstanceKeys.contains(shownInstanceData.InstanceKey)) {
                    notificationManager.cancel(shownInstanceData.NotificationId);
                    hideInstanceKeys.add(shownInstanceData.InstanceKey);
                }
            }

            ArrayList<InstanceKey> showInstanceKeys = new ArrayList<>();
            for (NotificationInstanceData notificationInstanceData : data.NotificationInstanceDatas) {
                if (!shownInstanceKeys.contains(notificationInstanceData.InstanceKey))
                    showInstanceKeys.add(notificationInstanceData.InstanceKey);

                notify(notificationInstanceData);
            }

            if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty())
                DomainFactory.getDomainFactory(this).updateInstancesShown(data.DataId, showInstanceKeys, hideInstanceKeys);
        }
    }

    private void notify(NotificationInstanceData notificationInstanceData) {
        Assert.assertTrue(notificationInstanceData != null);

        Intent deleteIntent = InstanceNotificationDeleteService.getIntent(this, notificationInstanceData.InstanceKey);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(this, notificationInstanceData.NotificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowInstanceActivity.getNotificationIntent(this, notificationInstanceData.InstanceKey);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(this, notificationInstanceData.NotificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notify(notificationInstanceData.Name, notificationInstanceData.DisplayText, notificationInstanceData.NotificationId, pendingDeleteIntent, pendingContentIntent);
    }

    private void notify(ArrayList<NotificationInstanceData> notificationInstanceDatas) {
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

        notify(notificationInstanceDatas.size() + " " + getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent);
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

    public static class Data extends DomainLoader.Data {
        public final ArrayList<NotificationInstanceData> NotificationInstanceDatas;
        public final ArrayList<ShownInstanceData> ShownInstanceDatas;

        public Data(ArrayList<NotificationInstanceData> notificationInstanceDatas, ArrayList<ShownInstanceData> shownInstanceDatas) {
            Assert.assertTrue(notificationInstanceDatas != null);
            Assert.assertTrue(shownInstanceDatas != null);

            NotificationInstanceDatas = notificationInstanceDatas;
            ShownInstanceDatas = shownInstanceDatas;
        }
    }

    public static class NotificationInstanceData {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final int NotificationId;
        public final String DisplayText;

        public NotificationInstanceData(InstanceKey instanceKey, String name, int notificationId, String displayText) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(displayText));

            InstanceKey = instanceKey;
            Name = name;
            NotificationId = notificationId;
            DisplayText = displayText;
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
