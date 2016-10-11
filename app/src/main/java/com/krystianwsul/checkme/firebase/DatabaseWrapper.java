package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import junit.framework.Assert;

import java.util.List;

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

    public static void addTask(@NonNull UserData userData, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull List<UserData> friends) {
        Assert.assertTrue(!friends.isEmpty());

        sDatabaseReference.child("tasks").push().setValue(new TaskWrapper(userData, friends, remoteTaskRecord));
    }

    @NonNull
    public static Query getTaskRecordsQuery(@NonNull UserData userData) {
        String key = UserData.getKey(userData.email);

        Query query = sDatabaseReference.child("tasks").orderByChild("taskOf/" + key).equalTo(true);
        Assert.assertTrue(query != null);

        return query;
    }

}
