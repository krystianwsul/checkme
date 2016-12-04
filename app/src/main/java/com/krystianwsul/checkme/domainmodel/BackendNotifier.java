package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteProject;

import junit.framework.Assert;

import java.util.List;
import java.util.Set;

class BackendNotifier {
    private static final String PREFIX = "http://check-me-add47.appspot.com/notify?";

    @NonNull
    static String getUrl(@NonNull Set<String> projects, boolean production) {
        Assert.assertTrue(!projects.isEmpty());

        List<String> parameters = Stream.of(projects)
                .map(project -> "projects=" + project)
                .collect(Collectors.toList());

        if (production)
            parameters.add("production=1");

        return PREFIX + TextUtils.join("&", parameters);
    }

    BackendNotifier(@NonNull Context context, @NonNull Set<RemoteProject> remoteProjects) {
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

        String url = getUrl(projectIds, production);
        Assert.assertTrue(!TextUtils.isEmpty(url));

        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, url,
                (response) -> Log.e("asdf", "BackendNotifier response:" + response),
                MyCrashlytics::logException);

        queue.add(stringRequest);

        Log.e("asdf", "BackendNotifier queued projects " + projectIds);
    }
}
