package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.IgnoreExtraProperties;

import junit.framework.Assert;

import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class UserWrapper {
    @Nullable
    private Map<String, Boolean> friendOf;

    @Nullable
    private UserJson userData;

    public UserWrapper() {

    }

    public UserWrapper(@NonNull Set<String> fiendOf, @NonNull UserJson userJson) {
        Assert.assertTrue(!fiendOf.isEmpty());

        this.friendOf = Stream.of(fiendOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.userData = userJson;
    }
}
