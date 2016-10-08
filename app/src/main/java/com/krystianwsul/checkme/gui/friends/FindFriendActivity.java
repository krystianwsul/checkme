package com.krystianwsul.checkme.gui.friends;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.krystianwsul.checkme.R;

import junit.framework.Assert;

public class FindFriendActivity extends AppCompatActivity {
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

        EditText editText = (EditText) findViewById(R.id.find_friend_email);
        Assert.assertTrue(editText != null);
    }
}
