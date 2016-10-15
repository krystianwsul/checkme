package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseWrapper {
    private static final DatabaseReference sDatabaseReference = FirebaseDatabase.getInstance().getReference();

    public static void setUserData(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);
        sDatabaseReference.child("users").child(key).child("userData").setValue(userData);
    }

    public static DatabaseReference getUserDataDatabaseReference(@NonNull String key) {
        return sDatabaseReference.child("users").child(key).child("userData");
    }

    public static void addFriend(@NonNull UserData userData, @NonNull UserData friendUserData) {
        String myKey = UserData.getKey(userData.email);
        String friendKey = UserData.getKey(friendUserData.email);

        sDatabaseReference.child("users").child(friendKey).child("friendOf").child(myKey).setValue(true);
    }

    public static void removeFriend(@NonNull UserData userData, @NonNull UserData friendUserData) {
        String myKey = UserData.getKey(userData.email);
        String friendKey = UserData.getKey(friendUserData.email);

        sDatabaseReference.child("users").child(friendKey).child("friendOf").child(myKey).setValue(null);
    }

    @NonNull
    static Query getFriendsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child("users").orderByChild("friendOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    public static void addRootTask(@NonNull UserData userData, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull List<UserData> friends) {
        Assert.assertTrue(!friends.isEmpty());

        List<UserData> userDatas = new ArrayList<>(friends);
        userDatas.add(userData);

        sDatabaseReference.child("tasks").push().setValue(new TaskWrapper(userDatas, remoteTaskRecord));
    }

    @NonNull
    public static Query getTaskRecordsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child("tasks").orderByChild("taskOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    public static void addChildTask(@NonNull RemoteTask parentTask, @NonNull RemoteTaskRecord remoteTaskRecord) {
        String parentTaskId = parentTask.getTaskKey().mRemoteTaskId;
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));

        Set<String> taskOf = parentTask.getTaskOf();

        String childTaskId = sDatabaseReference.child("tasks").push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(childTaskId));

        String taskHierarchyId = sDatabaseReference.child("tasks").push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(taskHierarchyId));

        Map<String, Object> updateData = new HashMap<>();
        updateData.put(childTaskId, new TaskWrapper(taskOf, remoteTaskRecord));
        updateData.put(taskHierarchyId, new TaskWrapper(taskOf, new RemoteTaskHierarchyRecord(parentTaskId, childTaskId, remoteTaskRecord.getStartTime(), null)));

        sDatabaseReference.child("tasks").updateChildren(updateData);
    }

    public static void setTaskEndTimeStamp(@NonNull RemoteTask task, @NonNull ExactTimeStamp now) {
        Map<String, Object> values = new HashMap<>();

        task.setEndExactTimeStamp(values, now);

        sDatabaseReference.updateChildren(values);
    }
}
