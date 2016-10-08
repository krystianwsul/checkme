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

    private EditText mFindFriendEmail;

    private LinearLayout mFindFriendUserLayout;
    private TextView mFindFriendUserName;
    private TextView mFindFriendUserEmail;

    private LinearLayout mFindFriendProgress;

    private User mUser = null;

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

                mUser = null;

                mFindFriendEmail.setEnabled(false);
                mFindFriendUserLayout.setVisibility(View.GONE);
                mFindFriendProgress.setVisibility(View.VISIBLE);

                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                String key = User.getKey(mFindFriendEmail.getText().toString());

                Log.e("asdf", "starting");

                databaseReference.child("users").child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.e("asdf", "onDataChange");

                        mFindFriendEmail.setEnabled(true);
                        mFindFriendProgress.setVisibility(View.GONE);

                        if (dataSnapshot.exists()) {
                            mUser = dataSnapshot.getValue(User.class);
                            Assert.assertTrue(mUser != null);

                            setUserLayout();
                        } else {
                            Toast.makeText(FindFriendActivity.this, R.string.userNotFound, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mFindFriendEmail.setEnabled(true);
                        mFindFriendProgress.setVisibility(View.GONE);

                        MyCrashlytics.logException(databaseError.toException());

                        Log.e("asdf", "onCancelled", databaseError.toException());

                        Toast.makeText(FindFriendActivity.this, R.string.connectionError, Toast.LENGTH_SHORT).show();
                    }
                });

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

        if (savedInstanceState != null && savedInstanceState.containsKey(USER_KEY)) {
            mUser = (User) savedInstanceState.getSerializable(USER_KEY);
            Assert.assertTrue(mUser != null);

            setUserLayout();
        }
    }

    private void setUserLayout() {
        Assert.assertTrue(mUser != null);

        mFindFriendUserLayout.setVisibility(View.VISIBLE);
        mFindFriendUserName.setText(mUser.displayName);
        mFindFriendUserEmail.setText(mUser.email);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mUser != null)
            outState.putSerializable(USER_KEY, mUser);
    }
}
