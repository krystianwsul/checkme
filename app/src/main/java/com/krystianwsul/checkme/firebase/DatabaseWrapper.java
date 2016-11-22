package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;

import junit.framework.Assert;

import java.util.Map;

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
    public static Query getFriendsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child(USERS_KEY).orderByChild("friendOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    @NonNull
    public static String getRootRecordId() {
        String id = sDatabaseReference.child(RECORDS_KEY).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static String getScheduleRecordId(@NonNull String taskId) {
        String id = sDatabaseReference.child(RECORDS_KEY + "/" + taskId + "/" + RemoteTaskRecord.TASK_JSON + "/" + RemoteScheduleRecord.SCHEDULES).push().getKey();
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

    public static void updateRecords(@NonNull Map<String, Object> values) {
        sDatabaseReference.child(RECORDS_KEY).updateChildren(values);
    }
}
