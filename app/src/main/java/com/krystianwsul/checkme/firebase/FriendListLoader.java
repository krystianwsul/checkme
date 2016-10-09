package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.v4.content.Loader;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;

import junit.framework.Assert;

import java.util.List;

public class FriendListLoader extends Loader<List<UserData>> {
    private Query mQuery;
    private ValueEventListener mValueEventListener;

    private List<UserData> mUserDatas;

    public FriendListLoader(Context context) {
        super(context);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        //String key = UserData.getKey(MainActivity.getUser().email);
        String key = UserData.getKey("krystianwsul@gmail.com");

        mQuery = databaseReference.child("users").orderByChild("friendOf/" + key).equalTo(true);
    }

    @Override
    protected void onStartLoading() {
        Log.e("asdf", "FriendListLoader.onStartLoading");

        if (mUserDatas != null)
            deliverResult(mUserDatas);

        if (mValueEventListener == null)
            attachListener();
    }

    @Override
    protected void onStopLoading() {
        Log.e("asdf", "FriendListLoader.onStopLoading");

        if (mValueEventListener != null) {
            mQuery.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    @Override
    protected void onReset() {
        Log.e("asdf", "FriendListLoader.onReset");

        if (mUserDatas != null)
            mUserDatas = null;
    }

    private void attachListener() {
        Assert.assertTrue(mValueEventListener == null);

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "FriendListLoader.mValueEventListener.onDataChange");

                List<UserData> userDatas = Stream.of(dataSnapshot.getChildren())
                        .map(child -> child.child("userData"))
                        .map(userData -> userData.getValue(UserData.class))
                        .collect(Collectors.toList());

                deliverResult(userDatas);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                MyCrashlytics.logException(databaseError.toException());

                Log.e("asdf", "FriendListLoader.mValueEventListener.onCancelled", databaseError.toException());
            }
        };
        mQuery.addValueEventListener(mValueEventListener);
    }

    @Override
    public void deliverResult(List<UserData> userDatas) {
        Log.e("asdf", "FriendListLoader.deliverResult starting");
        Assert.assertTrue(userDatas != null);

        if (isReset())
            return;

        mUserDatas = userDatas;

        if (isStarted()) {
            Log.e("asdf", "FriendListLoader.deliverResult delivering");
            super.deliverResult(userDatas);
        } else {
            Log.e("asdf", "FriendListLoader.deliverResult skipping");
        }
    }
}
