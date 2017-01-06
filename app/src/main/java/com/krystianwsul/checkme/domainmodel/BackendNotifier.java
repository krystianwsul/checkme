package com.krystianwsul.checkme.domainmodel;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.firebase.UserData;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class BackendNotifier {
    private static final String PREFIX = "http://check-me-add47.appspot.com/notify?";

    @NonNull
    static String getUrl(@NonNull Set<String> projects, boolean production, @NonNull String sender, @NonNull Collection<String> userKeys) {
        Assert.assertTrue(!projects.isEmpty());
        Assert.assertTrue(!TextUtils.isEmpty(sender));

        List<String> parameters = Stream.of(projects)
                .map(project -> "projects=" + project)
                .collect(Collectors.toList());

        parameters.addAll(Stream.of(userKeys)
                .map(userKey -> "userKeys=" + userKey)
                .collect(Collectors.toList()));

        if (production)
            parameters.add("production=1");

        parameters.add("sender=" + sender);

        return PREFIX + TextUtils.join("&", parameters);
    }

    BackendNotifier(@NonNull Set<RemoteProject> remoteProjects, @NonNull UserData userData, @NonNull Collection<String> userKeys) {
        String root = DatabaseWrapper.getRoot();

        boolean production;
        switch (root) {
            case "development":
                production = false;
                break;
            case "production":
                production = true;
                break;
            default:
                throw new IllegalArgumentException();
        }

        Set<String> projectIds = Stream.of(remoteProjects)
                .map(RemoteProject::getId)
                .collect(Collectors.toSet());

        String url = getUrl(projectIds, production, userData.getKey(), userKeys);
        Assert.assertTrue(!TextUtils.isEmpty(url));

        run(url);
    }

    private void run(@NonNull String url) {
        Assert.assertTrue(!TextUtils.isEmpty(url));

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

                    StringBuilder stringBuilder = new StringBuilder();

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        stringBuilder.append(inputLine);
                        stringBuilder.append("\n");
                    }
                    in.close();

                    Log.e("asdf", "BackendNotifier response: " + stringBuilder);
                } catch (UnknownHostException e) {
                    Log.e("asdf", "BackendNotifier exception", e);
                } catch (IOException e) {
                    MyCrashlytics.logException(e);
                }

                return null;
            }
        };

        asyncTask.execute();
    }
}
