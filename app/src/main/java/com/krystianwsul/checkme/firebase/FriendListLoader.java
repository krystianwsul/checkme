package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;

import junit.framework.Assert;

import java.util.List;

public class FriendListLoader extends Loader<List<UserData>> {
    private final Query mQuery;
    private ValueEventListener mValueEventListener;

    private List<UserData> mUserDatas;
    private boolean mFirst = true;

    public FriendListLoader(@NonNull Context context, @NonNull UserData userData) {
        super(context);

        Log.e("asdf", "FriendListLoader.construct");

        mQuery = DatabaseWrapper.getFriendsQuery(userData);
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

        if (mValueEventListener != null) {
            mQuery.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }

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

                deliverResult(null);
            }
        };
        mQuery.addValueEventListener(mValueEventListener);
    }

    @Override
    public void deliverResult(@Nullable List<UserData> userDatas) {
        Log.e("asdf", "FriendListLoader.deliverResult starting");

        if (isReset())
            return;

        if ((mUserDatas == null) || !mUserDatas.equals(userDatas)) {
            Log.e("asdf", "FriendListLoader.deliverResult delivering");
            mUserDatas = userDatas;

            if (isStarted()) {
                super.deliverResult(mUserDatas);
            } else {
                Log.e("asdf", "FriendListLoader.deliverResult skipping (stopped)");
            }
        } else {
            Log.e("asdf", "FriendListLoader.deliverResult skipping (no change)");
        }
    }
}
