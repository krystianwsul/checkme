package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.krystianwsul.checkme.OrganizatorApplication;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.UserInfo;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.firebase.records.RemoteProjectRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskHierarchyRecord;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;

import junit.framework.Assert;

import java.util.Map;

public class DatabaseWrapper {
    private static final String USERS_KEY = "users";
    private static final String RECORDS_KEY = "records";

    @Nullable
    private static String sRoot;

    @Nullable
    private static DatabaseReference sRootReference;

    public static void initialize(@NonNull OrganizatorApplication organizatorApplication) {
        Assert.assertTrue(sRootReference == null);

        sRoot = organizatorApplication.getResources().getString(R.string.firebase_root);
        Assert.assertTrue(!TextUtils.isEmpty(sRoot));

        sRootReference = FirebaseDatabase.getInstance().getReference().child(sRoot);
        Assert.assertTrue(sRootReference != null);
    }

    public static void setUserInfo(@NonNull UserInfo userInfo, @NonNull String uuid) {
        Assert.assertTrue(sRootReference != null);

        String key = userInfo.getKey();
        sRootReference.child(USERS_KEY).child(key).child("userData").updateChildren(userInfo.getValues(uuid));
    }

    @NonNull
    public static DatabaseReference getUserDataDatabaseReference(@NonNull String key) {
        Assert.assertTrue(sRootReference != null);

        return sRootReference.child(USERS_KEY).child(key).child("userData");
    }

    public static void addFriend(@NonNull UserInfo userInfo, @NonNull UserData friendUserData) {
        Assert.assertTrue(sRootReference != null);

        String myKey = userInfo.getKey();
        String friendKey = friendUserData.getKey();

        sRootReference.child(USERS_KEY).child(friendKey).child("friendOf").child(myKey).setValue(true);
    }

    @NonNull
    public static Query getFriendsQuery(@NonNull UserInfo userInfo) {
        Assert.assertTrue(sRootReference != null);

        String key = userInfo.getKey();

        Query query = sRootReference.child(USERS_KEY).orderByChild("friendOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    @NonNull
    public static String getRootRecordId() {
        Assert.assertTrue(sRootReference != null);

        String id = sRootReference.child(RECORDS_KEY).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static String getScheduleRecordId(@NonNull String projectId, @NonNull String taskId) {
        Assert.assertTrue(sRootReference != null);

        String id = sRootReference.child(RECORDS_KEY + "/" + projectId + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + RemoteTaskRecord.TASKS + "/" + taskId + "/" + RemoteScheduleRecord.SCHEDULES).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static String getTaskRecordId(@NonNull String projectId) {
        Assert.assertTrue(sRootReference != null);

        String id = sRootReference.child(RECORDS_KEY + "/" + projectId + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + RemoteTaskRecord.TASKS).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static String getTaskHierarchyRecordId(@NonNull String projectId) {
        Assert.assertTrue(sRootReference != null);

        String id = sRootReference.child(RECORDS_KEY + "/" + projectId + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + RemoteTaskHierarchyRecord.TASK_HIERARCHIES).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static String getCustomTimeRecordId(@NonNull String projectId) {
        Assert.assertTrue(sRootReference != null);

        String id = sRootReference.child(RECORDS_KEY + "/" + projectId + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + RemoteCustomTimeRecord.CUSTOM_TIMES).push().getKey();
        Assert.assertTrue(!TextUtils.isEmpty(id));

        return id;
    }

    @NonNull
    public static Query getTaskRecordsQuery(@NonNull UserInfo userInfo) {
        Assert.assertTrue(sRootReference != null);

        String key = userInfo.getKey();

        Query query = sRootReference.child(RECORDS_KEY).orderByChild("recordOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

    public static void updateRecords(@NonNull Map<String, Object> values) {
        Assert.assertTrue(sRootReference != null);

        sRootReference.child(RECORDS_KEY).updateChildren(values);
    }

    public static void updateFriends(@NonNull Map<String, Object> values) {
        Assert.assertTrue(sRootReference != null);

        sRootReference.child(USERS_KEY).updateChildren(values);
    }

    @NonNull
    public static String getRoot() {
        Assert.assertTrue(!TextUtils.isEmpty(sRoot));

        return sRoot;
    }

    @NonNull
    public static Query getUserQuery(@NonNull UserInfo userInfo) {
        Assert.assertTrue(sRootReference != null);

        String key = userInfo.getKey();

        Query query = sRootReference.child(USERS_KEY + "/" + userInfo.getKey());
        Assert.assertTrue(query != null);

        return query;
    }
}
