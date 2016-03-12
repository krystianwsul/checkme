package com.example.krystianwsul.organizator.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.krystianwsul.organizator.R;

public class DebugActivity extends AppCompatActivity {

    public static Intent getIntent(Context context) {
        return new Intent(context, DebugActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        Button debugException = (Button) findViewById(R.id.debug_exception);
        debugException.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = 0;
                int j = 1 / i;
            }
        });
    }
}
