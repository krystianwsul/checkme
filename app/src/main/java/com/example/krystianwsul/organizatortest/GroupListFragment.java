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
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.InstanceGroup;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Krystian on 10/31/2015.
 */
public class GroupListFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_list_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView groupList = (ListView) getView().findViewById(R.id.groups_list);

        Collection<RootTask> rootTasks = TaskFactory.getInstance().getRootTasks();

        Calendar calendarWeekAgo = Calendar.getInstance();
        calendarWeekAgo.add(Calendar.DATE, -7);

        final ArrayList<Instance> instances = new ArrayList<>();
        for (RootTask rootTask : rootTasks)
            instances.addAll(rootTask.getInstances(new TimeStamp(calendarWeekAgo), new TimeStamp(Date.today(), new HourMinute(23, 59))));

        ArrayList<Group> groups = new ArrayList<>();
        for (Instance instance : instances)
            groups.add(new InstanceGroup(instance));

        Collections.sort(groups, new Comparator<Group>() {
            @Override
            public int compare(Group lhs, Group rhs) {
                int dateTimeComparison = lhs.getDateTime().compareTo(rhs.getDateTime());
                if (dateTimeComparison != 0)
                    return dateTimeComparison;

                return lhs.getName().compareTo(rhs.getName());
            }
        });

        groupList.setAdapter(new GroupAdapter(getContext(), groups));

        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = (Group) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowGroup.class);
                intent.putExtra(group.getIntentKey(), group.getIntentValue());
                startActivity(intent);
            }
        });
    }
}
