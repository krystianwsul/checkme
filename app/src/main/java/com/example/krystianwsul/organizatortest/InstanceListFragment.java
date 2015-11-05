package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/31/2015.
 */
public class InstanceListFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.instance_list_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView instanceList = (ListView) getView().findViewById(R.id.instances_list);

        ArrayList<RootTask> rootTasks = Task.getRootTasks();

        ArrayList<Instance> instances = new ArrayList<>();
        for (RootTask rootTask : rootTasks)
            instances.addAll(rootTask.getInstances(new TimeStamp(Date.today(), new HourMinute(0, 0)), new TimeStamp(Date.today(), new HourMinute(23, 59))));

        instanceList.setAdapter(new InstanceAdapter(getContext(), instances));

        /*
        instanceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowTask.class);
                intent.putExtra("taskId", task.getTaskId());
                startActivity(intent);
            }
        });
        */
    }
}
