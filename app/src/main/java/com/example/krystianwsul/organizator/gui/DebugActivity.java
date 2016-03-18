package com.example.krystianwsul.organizator.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

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

        TextView debugTime = (TextView) findViewById(R.id.debug_time);
        Assert.assertTrue(debugTime != null);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        debugTime.setText("data load time: " + domainFactory.getTotalMillis() + "ms (read: " + domainFactory.getReadMillis() + "ms)");

        Button debugTick = (Button) findViewById(R.id.debug_tick);
        Assert.assertTrue(debugTick != null);

        debugTick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TickService.startService(DebugActivity.this);
            }
        });
    }
}
