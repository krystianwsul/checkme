package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.IgnoreExtraProperties;

import junit.framework.Assert;

import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class JsonWrapper {
    public Map<String, Boolean> recordOf;
    public CustomTimeJson customTimeJson;
    public ProjectJson projectJson;

    public JsonWrapper() {

    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull CustomTimeJson customTimeJsonJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.customTimeJson = customTimeJsonJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull ProjectJson projectJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.projectJson = projectJson;
    }

    public void setRecordOf(@NonNull Map<String, Boolean> recordOf) {
        this.recordOf = recordOf;
    }
}
