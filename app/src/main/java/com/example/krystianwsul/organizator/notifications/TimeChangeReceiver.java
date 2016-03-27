package com.example.krystianwsul.organizator.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TickService.startService(context);
    }
}
