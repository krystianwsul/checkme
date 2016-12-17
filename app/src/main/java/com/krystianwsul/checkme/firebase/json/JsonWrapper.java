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
    public ProjectJson projectJson;

    public JsonWrapper() {

    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull ProjectJson projectJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.projectJson = projectJson;
    }

    public void updateRecordOf(@NonNull Set<String> add, @NonNull Set<String> remove) {
        Assert.assertTrue(Stream.of(add).noneMatch(remove::contains));
        Assert.assertTrue(Stream.of(add).noneMatch(recordOf::containsKey));

        Assert.assertTrue(Stream.of(remove).noneMatch(add::contains));
        Assert.assertTrue(Stream.of(remove).allMatch(recordOf::containsKey));

        for (String key : remove)
            recordOf.remove(key);

        for (String key : add)
            recordOf.put(key, true);
    }
}
