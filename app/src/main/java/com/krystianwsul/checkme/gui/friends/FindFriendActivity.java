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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.MainActivity;

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
    private UserData mUserData = null;

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
                startSearch();

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

        mFindFriendEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch();
                return true;
            }
            return false;
        });

        mFindFriendUserLayout = (LinearLayout) findViewById(R.id.find_friend_user_layout);
        Assert.assertTrue(mFindFriendUserLayout != null);

        mFindFriendUserLayout.setOnClickListener(v -> {
            Assert.assertTrue(mUserData != null);
            Assert.assertTrue(!mLoading);

            UserData myUserData = MainActivity.getUserData();
            Assert.assertTrue(myUserData != null);

            DatabaseWrapper.addFriend(myUserData, mUserData);

            finish();
        });

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
                mUserData = savedInstanceState.getParcelable(USER_KEY);
                Assert.assertTrue(mUserData != null);
            }
        }

        updateLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mLoading)
            loadUser();
    }

    private void loadUser() {
        Assert.assertTrue(mLoading);
        Assert.assertTrue(mUserData == null);
        Assert.assertTrue(!TextUtils.isEmpty(mFindFriendEmail.getText()));

        String key = UserData.getKey(mFindFriendEmail.getText().toString());

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
                    mUserData = dataSnapshot.getValue(UserData.class);
                    Assert.assertTrue(mUserData != null);
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

        mDatabaseReference = DatabaseWrapper.getUserDataDatabaseReference(key);

        Log.e("asdf", "addValueEventListener " + mValueEventListener.hashCode());
        mDatabaseReference.addValueEventListener(mValueEventListener);
    }

    private void updateLayout() {
        Log.e("asdf", "updateLayout " + hashCode());

        if (mUserData != null) {
            Assert.assertTrue(!mLoading);

            mFindFriendEmail.setEnabled(true);
            mFindFriendUserLayout.setVisibility(View.VISIBLE);
            mFindFriendProgress.setVisibility(View.GONE);

            mFindFriendUserName.setText(mUserData.getDisplayName());
            mFindFriendUserEmail.setText(mUserData.getEmail());
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

        if (mUserData != null)
            outState.putParcelable(USER_KEY, mUserData);
    }

    private void startSearch() {
        if (TextUtils.isEmpty(mFindFriendEmail.getText()))
            return;

        mLoading = true;
        mUserData = null;

        updateLayout();

        loadUser();
    }
}
