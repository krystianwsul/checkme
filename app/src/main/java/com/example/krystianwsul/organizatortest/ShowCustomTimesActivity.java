package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;

public class ShowCustomTimesActivity extends AppCompatActivity {
    public static Intent getIntent(Context context) {
        return new Intent(context, ShowCustomTimesActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_times);

        ListView showTimesList = (ListView) findViewById(R.id.show_times_list);
        showTimesList.setAdapter(new CustomTimesAdapter(this));

        showTimesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CustomTime customTime = (CustomTime) parent.getItemAtPosition(position);
                startActivity(ShowCustomTimeActivity.getIntent(customTime, view.getContext()));
            }
        });
    }
}
