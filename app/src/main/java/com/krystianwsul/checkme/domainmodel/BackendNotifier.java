package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

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
}
