package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.UserWrapper;

import java.util.HashMap;
import java.util.Map;

public class RemoteFriendManager {
    private boolean mSaved = false;

    @NonNull
    public final Map<String, RemoteRootUserRecord> mRemoteRootUserRecords;

    public RemoteFriendManager(@NonNull Iterable<DataSnapshot> children) {
        mRemoteRootUserRecords = Stream.of(children)
                .map(child -> child.getValue(UserWrapper.class))
                .map(userWrapper -> new RemoteRootUserRecord(false, userWrapper))
                .collect(Collectors.toMap(RemoteRootUserRecord::getId, remoteRootUserRecord -> remoteRootUserRecord));
    }

    public void save() {
        Map<String, Object> values = new HashMap<>();

        Stream.of(mRemoteRootUserRecords.values())
                .forEach(remoteRootUserRecord -> remoteRootUserRecord.getValues(values));

        Log.e("asdf", "RemoteFriendManager.save values: " + values);

        if (!values.isEmpty()) {
            mSaved = true;
            DatabaseWrapper.INSTANCE.updateFriends(values);
        }
    }

    public boolean isSaved() {
        return mSaved;
    }
}
