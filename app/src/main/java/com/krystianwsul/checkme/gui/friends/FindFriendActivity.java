package com.krystianwsul.checkme.gui.friends;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.User;

import junit.framework.Assert;

public class FindFriendActivity extends AppCompatActivity {
    private static final String USER_KEY = "user";
    private static final String LOADING_KEY = "loading";

    private EditText mFindFriendEmail;

    private LinearLayout mFindFriendUserLayout;
    private TextView mFindFriendUserName;
    private TextView mFindFriendUserEmail;

    private LinearLayout mFindFriendProgress;

    private boolean mLoading = false;
    private User mUser = null;

    private DatabaseReference mDatabaseReference;
    private ValueEventListener mValueEventListener;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, FindFriendActivity.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_find_friend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                if (TextUtils.isEmpty(mFindFriendEmail.getText()))
                    break;

                mLoading = true;
                mUser = null;

                updateLayout();

                loadUser();

                break;
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_find_friend);

        Log.e("asdf", "onCreate " + hashCode());

        Toolbar toolbar = (Toolbar) findViewById(R.id.find_friend_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mFindFriendEmail = (EditText) findViewById(R.id.find_friend_email);
        Assert.assertTrue(mFindFriendEmail != null);

        mFindFriendUserLayout = (LinearLayout) findViewById(R.id.find_friend_user_layout);
        Assert.assertTrue(mFindFriendUserLayout != null);

        mFindFriendUserName = (TextView) findViewById(R.id.find_friend_user_name);
        Assert.assertTrue(mFindFriendUserName != null);

        mFindFriendUserEmail = (TextView) findViewById(R.id.find_friend_user_email);
        Assert.assertTrue(mFindFriendUserEmail != null);

        mFindFriendProgress = (LinearLayout) findViewById(R.id.find_friend_progress);
        Assert.assertTrue(mFindFriendProgress != null);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(LOADING_KEY));
            mLoading = savedInstanceState.getBoolean(LOADING_KEY);

            if (savedInstanceState.containsKey(USER_KEY)) {
                mUser = (User) savedInstanceState.getSerializable(USER_KEY);
                Assert.assertTrue(mUser != null);
            }
        }

        updateLayout();

        if (mLoading)
            loadUser();
    }

    private void loadUser() {
        Assert.assertTrue(mLoading);
        Assert.assertTrue(mUser == null);
        Assert.assertTrue(!TextUtils.isEmpty(mFindFriendEmail.getText()));

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String key = User.getKey(mFindFriendEmail.getText().toString());

        Log.e("asdf", "starting");

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "onDataChange " + hashCode());

                Assert.assertTrue(dataSnapshot != null);

                mDatabaseReference.removeEventListener(mValueEventListener);

                mLoading = false;
                mValueEventListener = null;
                mDatabaseReference = null;

                if (dataSnapshot.exists()) {
                    mUser = dataSnapshot.getValue(User.class);
                    Assert.assertTrue(mUser != null);
                } else {
                    Toast.makeText(FindFriendActivity.this, R.string.userNotFound, Toast.LENGTH_SHORT).show();
                }

                updateLayout();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);

                mDatabaseReference.removeEventListener(mValueEventListener);

                mLoading = false;
                mValueEventListener = null;
                mDatabaseReference = null;

                updateLayout();

                MyCrashlytics.logException(databaseError.toException());

                Log.e("asdf", "onCancelled", databaseError.toException());

                Toast.makeText(FindFriendActivity.this, R.string.connectionError, Toast.LENGTH_SHORT).show();
            }
        };

        mDatabaseReference = databaseReference.child("users").child(key);

        Log.e("asdf", "addValueEventListener " + mValueEventListener.hashCode());
        mDatabaseReference.addValueEventListener(mValueEventListener);
    }

    private void updateLayout() {
        Log.e("asdf", "updateLayout " + hashCode());

        if (mUser != null) {
            Assert.assertTrue(!mLoading);

            mFindFriendEmail.setEnabled(true);
            mFindFriendUserLayout.setVisibility(View.VISIBLE);
            mFindFriendProgress.setVisibility(View.GONE);

            mFindFriendUserName.setText(mUser.displayName);
            mFindFriendUserEmail.setText(mUser.email);
        } else if (mLoading) {
            mFindFriendEmail.setEnabled(false);
            mFindFriendUserLayout.setVisibility(View.GONE);
            mFindFriendProgress.setVisibility(View.VISIBLE);
        } else {
            mFindFriendEmail.setEnabled(true);
            mFindFriendUserLayout.setVisibility(View.GONE);
            mFindFriendProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e("asdf", "onStop");

        if (mLoading) {
            Assert.assertTrue(mDatabaseReference != null);
            Assert.assertTrue(mValueEventListener != null);

            Log.e("asdf", "removing listener " + mValueEventListener.hashCode());

            mDatabaseReference.removeEventListener(mValueEventListener);
        } else {
            Assert.assertTrue(mDatabaseReference == null);
            Assert.assertTrue(mValueEventListener == null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(LOADING_KEY, mLoading);

        if (mUser != null)
            outState.putSerializable(USER_KEY, mUser);
    }
}
