package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseWrapper {
    private static final String USERS_KEY = "users";

    private static final String RECORDS_KEY = "records";

    private static final DatabaseReference sDatabaseReference = FirebaseDatabase.getInstance().getReference();

    public static void setUserData(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);
        sDatabaseReference.child(USERS_KEY).child(key).child("userData").setValue(userData);
    }

    public static DatabaseReference getUserDataDatabaseReference(@NonNull String key) {
        return sDatabaseReference.child(USERS_KEY).child(key).child("userData");
    }

    public static void addFriend(@NonNull UserData userData, @NonNull UserData friendUserData) {
        String myKey = UserData.getKey(userData.email);
        String friendKey = UserData.getKey(friendUserData.email);

        sDatabaseReference.child(USERS_KEY).child(friendKey).child("friendOf").child(myKey).setValue(true);
    }

    public static void removeFriend(@NonNull UserData userData, @NonNull UserData friendUserData) {
        String myKey = UserData.getKey(userData.email);
        String friendKey = UserData.getKey(friendUserData.email);

        sDatabaseReference.child(USERS_KEY).child(friendKey).child("friendOf").child(myKey).setValue(null);
    }

    @NonNull
    static Query getFriendsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child(USERS_KEY).orderByChild("friendOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    public static void addRootTask(@NonNull UserData userData, @NonNull TaskJson taskJson, @NonNull List<UserData> friends) {
        Assert.assertTrue(!friends.isEmpty());

        List<UserData> userDatas = new ArrayList<>(friends);
        userDatas.add(userData);

        sDatabaseReference.child(RECORDS_KEY).push().setValue(new JsonWrapper(userDatas, taskJson));
    }

    @NonNull
    public static String getRecordId() {
        String id = sDatabaseReference.child(RECORDS_KEY).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static Query getTaskRecordsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child(RECORDS_KEY).orderByChild("recordOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    public static void addChildTask(@NonNull RemoteTask parentTask, @NonNull TaskJson taskJson) {
        String parentTaskId = parentTask.getTaskKey().mRemoteTaskId;
        Assert.assertTrue(!TextUtils.isEmpty(parentTaskId));

        Set<String> taskOf = parentTask.getRecordOf();

        String childTaskId = sDatabaseReference.child(RECORDS_KEY).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(childTaskId));

        String taskHierarchyId = sDatabaseReference.child(RECORDS_KEY).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(taskHierarchyId));

        Map<String, Object> updateData = new HashMap<>();
        updateData.put(childTaskId, new JsonWrapper(taskOf, taskJson));
        updateData.put(taskHierarchyId, new JsonWrapper(taskOf, new TaskHierarchyJson(parentTaskId, childTaskId, taskJson.getStartTime(), null)));

        sDatabaseReference.child(RECORDS_KEY).updateChildren(updateData);
    }

    public static void setTaskEndTimeStamp(@NonNull RemoteTask task, @NonNull ExactTimeStamp now) {
        Map<String, Object> values = new HashMap<>();

        task.setEndExactTimeStamp(values, now);

        sDatabaseReference.updateChildren(values);
    }
}
