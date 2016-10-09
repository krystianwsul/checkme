package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DatabaseWrapper {
    private static DatabaseReference sDatabaseReference = FirebaseDatabase.getInstance().getReference();

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

        sDatabaseReference.child("users").child(myKey).child("friends").child(friendKey).setValue(true);
    }
}
