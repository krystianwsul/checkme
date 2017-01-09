package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.records.RemoteFriendManager;

import junit.framework.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class RemoteFriendFactory {
    @NonNull
    private final RemoteFriendManager mRemoteFriendManager;

    @NonNull
    private final Map<String, RemoteRootUser> mFriends;

    public RemoteFriendFactory(@NonNull Iterable<DataSnapshot> children) {
        mRemoteFriendManager = new RemoteFriendManager(children);

        mFriends = Stream.of(mRemoteFriendManager.mRemoteRootUserRecords.values())
                .map(RemoteRootUser::new)
                .collect(Collectors.toMap(RemoteRootUser::getId, remoteRootUser -> remoteRootUser));
    }

    public void save() {
        Assert.assertTrue(!mRemoteFriendManager.isSaved());

        mRemoteFriendManager.save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSaved() {
        return mRemoteFriendManager.isSaved();
    }

    @NonNull
    public Collection<RemoteRootUser> getFriends() {
        return mFriends.values();
    }

    @NonNull
    public Map<String, UserJson> getUserJsons(@NonNull Set<String> friendIds) {
        Assert.assertTrue(Stream.of(friendIds).allMatch(mFriends::containsKey));

        return Stream.of(mFriends.entrySet())
                .filter(entry -> friendIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUserJson()));
    }

    @NonNull
    public RemoteRootUser getFriend(@NonNull String friendId) {
        Assert.assertTrue(mFriends.containsKey(friendId));

        RemoteRootUser remoteRootUser = mFriends.get(friendId);
        Assert.assertTrue(remoteRootUser != null);

        return remoteRootUser;
    }

    public void removeFriend(@NonNull String userKey, @NonNull String friendId) {
        Assert.assertTrue(!TextUtils.isEmpty(userKey));
        Assert.assertTrue(!TextUtils.isEmpty(friendId));
        Assert.assertTrue(mFriends.containsKey(friendId));

        RemoteRootUser remoteRootUser = mFriends.get(friendId);
        Assert.assertTrue(remoteRootUser != null);

        remoteRootUser.removeFriend(userKey);

        mFriends.remove(friendId);
    }
}
