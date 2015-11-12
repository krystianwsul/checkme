package com.example.krystianwsul.organizatortest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.krystianwsul.organizatortest.arrayadapters.TaskAdapter;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Krystian on 10/31/2015.
 */
public class TaskListFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView tasksList = (ListView) getView().findViewById(R.id.tasks_list);

        ArrayList<Task> rootTasks = new ArrayList<Task>(TaskFactory.getInstance().getRootTasks());

        Collections.sort(rootTasks, new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        tasksList.setAdapter(new TaskAdapter(getContext(), rootTasks));

        tasksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) parent.getItemAtPosition(position);
                startActivity(ShowTaskActivity.getIntent(task, view.getContext()));
            }
        });
    }
}
