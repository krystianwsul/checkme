package com.example.krystianwsul.organizatortest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

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

        HashMap<DateTime, Group> groupHash = new HashMap<>();
        for (Instance instance : instances) {
            DateTime dateTime = instance.getDateTime();
            if (groupHash.containsKey(dateTime)) {
                groupHash.get(dateTime).addInstance(instance);
            } else {
                Group group = new Group(dateTime.getDate(), dateTime.getTime().getTimeByDay(dateTime.getDate().getDayOfWeek()));
                group.addInstance(instance);
                groupHash.put(dateTime, group);
            }
        }

        ArrayList<Group> groupArray = new ArrayList<>(groupHash.values());

        Collections.sort(groupArray, new Comparator<Group>() {
            @Override
            public int compare(Group lhs, Group rhs) {
                int dateComparison = lhs.getDate().compareTo(rhs.getDate());
                if (dateComparison != 0)
                    return dateComparison;
                return lhs.getHourMinute().compareTo(rhs.getHourMinute());
            }
        });

        groupList.setAdapter(new GroupAdapter(getContext(), groupArray));

        /*
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group group = (Group) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowGroup.class);
                intent.putExtra(group.getIntentKey(), group.getIntentValue());
                startActivity(intent);
            }
        });
        */
    }
}
