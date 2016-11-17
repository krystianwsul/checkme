package com.krystianwsul.checkme;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.List;

public class OrganizatorApplication extends Application {
    private static SharedPreferences sSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        MyCrashlytics.initialize(this);

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        sSharedPreferences = getSharedPreferences("asdf", Context.MODE_PRIVATE);
    }

    public static void logInfo(@NonNull Task task, @NonNull String message) {
        if (!logTask(task.getTaskKey()))
            return;

        Log.e("asdf", "logging message: " + message);

        String asdf = sSharedPreferences.getString("asdf", "");
        asdf = asdf + "\n" + ExactTimeStamp.getNow() + ", " + task.getTaskKey().mLocalTaskId + " - " + task.getName() + ": " + message;
        sSharedPreferences.edit()
                .putString("asdf", asdf)
                .apply();
    }

    public static final List<Integer> sLoggedTaskIds = Arrays.asList(455, 471, 472, 473, 508, 510, 511);

    public static boolean logTask(@NonNull TaskKey taskKey) {
        Assert.assertTrue(taskKey.mLocalTaskId != null);
        return sLoggedTaskIds.contains(taskKey.mLocalTaskId);
    }
}
